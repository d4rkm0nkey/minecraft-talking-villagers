package me.teajay.talkingvillagers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.server.SpeechService;
import me.teajay.talkingvillagers.server.SpeechWatchers;
import me.teajay.talkingvillagers.voice.EntityVoices;
import me.teajay.talkingvillagers.voice.VoiceCommandService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * {@code /talkingvillagers} (alias {@code /tv}). The work is done by {@link VoiceCommandService}; this class only parses
 * arguments and formats replies.
 */
public final class TalkingVillagersCommand {
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private TalkingVillagersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("talkingvillagers")
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(TalkingVillagersCommand::reloadAll))
                .then(packs())
                .then(voice())
                .then(gender())
                .then(language())
                .then(debug()));
        dispatcher.register(Commands.literal("tv").redirect(root));
    }

    // --- packs ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> packs() {
        return Commands.literal("packs")
                .then(Commands.literal("list").executes(TalkingVillagersCommand::listPacks))
                .then(Commands.literal("info")
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Voicepacks.index().active().keySet(), builder))
                                .executes(TalkingVillagersCommand::packInfo)))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(TalkingVillagersCommand::reloadPacks));
    }

    private static int listPacks(CommandContext<CommandSourceStack> context) {
        PackIndex index = Voicepacks.index();
        CommandSourceStack source = context.getSource();
        if (index.all().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.none", Voicepacks.repository().directory().toString()), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.header", index.active().size(), index.all().size()), false);
        for (LoadedPack loaded : index.all()) {
            String file = loaded.source().getFileName().toString();
            if (loaded.pack().isEmpty()) {
                String firstError = loaded.errors().isEmpty() ? "" : loaded.errors().getFirst().toString();
                source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.broken", file, loaded.errors().size(), firstError)
                        .withStyle(ChatFormatting.RED), false);
                continue;
            }
            VoicePack pack = loaded.pack().get();
            boolean active = index.active().get(pack.id()) == loaded;
            MutableComponent line = Component.translatable(active ? "commands.talkingvillagers.packs.entry" : "commands.talkingvillagers.packs.inactive",
                    pack.name(), pack.version(), pack.voices().size(), pack.language(), file)
                    .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            if (active) {
                line = clickable(line, "/tv packs info " + pack.id());
            }
            MutableComponent message = line;
            source.sendSuccess(() -> message, false);
        }
        return index.active().size();
    }

    private static int packInfo(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "pack");
        CommandSourceStack source = context.getSource();
        Optional<VoicePack> pack = Voicepacks.index().pack(id);
        if (pack.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.packs.unknown", id));
            return 0;
        }
        VoicePack p = pack.get();
        MutableComponent header = Component.translatable("commands.talkingvillagers.packs.info", p.name(), p.version(), p.language());
        if (!p.authors().isEmpty()) {
            header.append(" ").append(Component.translatable("commands.talkingvillagers.packs.info.authors", String.join(", ", p.authors())));
        }
        p.description().ifPresent(description -> header.append(Component.literal(" – " + description).withStyle(ChatFormatting.GRAY)));
        source.sendSuccess(() -> header, false);
        for (ResolvedVoice voice : p.resolvedVoices()) {
            Voice v = voice.voice();
            int lines = v.lines().values().stream().mapToInt(List::size).sum();
            String entities = String.join(", ", v.entities().stream().sorted().map(EntityKind::id).toList());
            MutableComponent line = Component.translatable("commands.talkingvillagers.packs.info.voice", v.name(), voice.ref().toString(),
                    entities, gender(v.gender()), age(v.child()), voice.language(), lines);
            source.sendSuccess(() -> line, false);
        }
        return p.voices().size();
    }

    private static int reloadAll(CommandContext<CommandSourceStack> context) {
        Configs.load();
        context.getSource().sendSuccess(() -> Component.translatable("commands.talkingvillagers.config.reloaded", Configs.serverFile().toString()), true);
        return reloadPacks(context);
    }

    private static int reloadPacks(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.reloading"), false);
        // reading large packs takes a while; the server keeps ticking and the answer comes when the reload is done
        Voicepacks.reloadAsync().thenAcceptAsync(index -> {
            long broken = index.all().stream().filter(pack -> !pack.isUsable()).count();
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.reloaded",
                    index.active().size(), index.voices().size(), broken), true);
        }, source.getServer());
        return 1;
    }

    // --- voice ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> voice() {
        return Commands.literal("voice")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(TalkingVillagersCommand::getVoice)))
                .then(Commands.literal("set")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("voice", IdentifierArgument.id())
                                        .suggests(TalkingVillagersCommand::suggestFittingVoices)
                                        .executes(context -> setVoice(context, false))
                                        .then(Commands.literal("force").executes(context -> setVoice(context, true))))))
                .then(Commands.literal("randomize")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> randomize(context, true))
                                .then(Commands.literal("newGender").executes(context -> randomize(context, false)))))
                .then(Commands.literal("clear")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(TalkingVillagersCommand::clearVoice)))
                .then(Commands.literal("say")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("event", StringArgumentType.word())
                                        .suggests(TalkingVillagersCommand::suggestEventsOfTarget)
                                        .executes(context -> say(context, OptionalInt.empty()))
                                        .then(Commands.argument("line", IntegerArgumentType.integer(0))
                                                .executes(context -> say(context, OptionalInt.of(IntegerArgumentType.getInteger(context, "line"))))))));
    }

    private static int getVoice(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();
        Optional<VoiceIdentity> identity = entity instanceof LivingEntity living ? VoiceCommandService.identity(living) : Optional.empty();
        if (identity.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.voice.unsupported", entity.getDisplayName()));
            return 0;
        }
        VoiceIdentity id = identity.get();
        Optional<ResolvedVoice> voice = id.voice().flatMap(VoiceCommandService::resolve);
        String voiceName = voice.map(v -> v.voice().name() + " (" + v.ref() + ")").orElse("-");
        MutableComponent message = Component.translatable("commands.talkingvillagers.voice.get", entity.getDisplayName(), voiceName,
                gender(id.gender()), age(id.child()), String.format(Locale.ROOT, "%.2f", id.pitch()), id.language().orElse("-"), state(id));
        Optional<VoiceEvent> sample = voice.flatMap(VoiceCommandService::sampleEvent);
        if (sample.isPresent()) {
            message.append(" ").append(clickable(Component.translatable("commands.talkingvillagers.voice.play").withStyle(ChatFormatting.AQUA),
                    "/tv voice say " + entity.getStringUUID() + " " + sample.get().id()));
        }
        source.sendSuccess(() -> message, false);
        return voice.isPresent() ? 1 : 0;
    }

    private static int setVoice(CommandContext<CommandSourceStack> context, boolean force) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        Identifier id = IdentifierArgument.getId(context, "voice");
        CommandSourceStack source = context.getSource();
        Optional<ResolvedVoice> voice = VoiceCommandService.resolve(new VoiceRef(id.getNamespace(), id.getPath()));
        if (voice.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.say.unknown_voice", id.toString()));
            return 0;
        }
        int changed = 0;
        int skipped = 0;
        for (Entity entity : targets) {
            VoiceCommandService.SetResult result = entity instanceof LivingEntity living
                    ? VoiceCommandService.setVoice(living, voice.get(), force)
                    : VoiceCommandService.SetResult.UNSUPPORTED;
            switch (result) {
                case OK -> changed++;
                case UNSUPPORTED -> skipped++;
                case WRONG_KIND -> source.sendFailure(Component.translatable("commands.talkingvillagers.voice.set.wrong_kind", entity.getDisplayName(),
                        voice.get().ref().toString(), String.join(", ", voice.get().voice().entities().stream().sorted().map(EntityKind::id).toList())));
                case WRONG_AGE -> source.sendFailure(Component.translatable("commands.talkingvillagers.voice.set.wrong_age", entity.getDisplayName(),
                        voice.get().ref().toString(), age(voice.get().voice().child())));
            }
        }
        reportSkipped(source, skipped, targets.size());
        int count = changed;
        if (count > 0) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.voice.set.success", count, voice.get().ref().toString()), true);
        }
        return count;
    }

    private static int randomize(CommandContext<CommandSourceStack> context, boolean keepGender) throws CommandSyntaxException {
        return forEachLiving(context, "targets", entity -> VoiceCommandService.randomize(entity, keepGender).isPresent(),
                count -> Component.translatable("commands.talkingvillagers.voice.randomize.success", count));
    }

    private static int clearVoice(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return forEachLiving(context, "targets", VoiceCommandService::mute,
                count -> Component.translatable("commands.talkingvillagers.voice.clear.success", count));
    }

    private static int say(CommandContext<CommandSourceStack> context, OptionalInt line) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        String eventId = StringArgumentType.getString(context, "event");
        CommandSourceStack source = context.getSource();
        Optional<VoiceEvent> event = EventCatalog.byId(eventId);
        if (event.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.say.unknown_event", eventId));
            return 0;
        }
        if (!(entity instanceof LivingEntity living) || !VoiceCommandService.say(living, event.get(), line)) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.voice.say.nothing", entity.getDisplayName(), eventId));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.voice.say.success", entity.getDisplayName(), eventId), false);
        return 1;
    }

    // --- gender --------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> gender() {
        var set = Commands.argument("targets", EntityArgument.entities());
        for (Gender gender : List.of(Gender.MALE, Gender.FEMALE)) {
            set.then(Commands.literal(gender.id()).executes(context -> forEachLiving(context, "targets",
                    entity -> VoiceCommandService.setGender(entity, gender).isPresent() || hasIdentity(entity),
                    count -> Component.translatable("commands.talkingvillagers.gender.set.success", count, gender(gender)))));
        }
        return Commands.literal("gender")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> {
                                    Entity entity = EntityArgument.getEntity(context, "target");
                                    Optional<VoiceIdentity> identity = entity instanceof LivingEntity living
                                            ? VoiceCommandService.identity(living) : Optional.empty();
                                    if (identity.isEmpty()) {
                                        context.getSource().sendFailure(Component.translatable("commands.talkingvillagers.voice.unsupported", entity.getDisplayName()));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(() -> Component.translatable("commands.talkingvillagers.gender.get",
                                            entity.getDisplayName(), gender(identity.get().gender())), false);
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(set));
    }

    // --- language ------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> language() {
        return Commands.literal("language")
                .then(Commands.literal("village")
                        .then(Commands.literal("get")
                                .executes(context -> villageLanguage(context, here(context)))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> villageLanguage(context, BlockPosArgument.getBlockPos(context, "pos")))))
                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("language", StringArgumentType.word())
                                        .suggests(TalkingVillagersCommand::suggestLanguages)
                                        .executes(context -> setVillageLanguage(context, here(context), false))
                                        .then(Commands.literal("reassign")
                                                .executes(context -> setVillageLanguage(context, here(context), true)))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(context -> setVillageLanguage(context, BlockPosArgument.getBlockPos(context, "pos"), false))
                                                .then(Commands.literal("reassign")
                                                        .executes(context -> setVillageLanguage(context, BlockPosArgument.getBlockPos(context, "pos"), true)))))))
                .then(Commands.literal("entity")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("language", StringArgumentType.word())
                                                .suggests(TalkingVillagersCommand::suggestLanguages)
                                                .executes(TalkingVillagersCommand::setEntityLanguage)))));
    }

    private static BlockPos here(CommandContext<CommandSourceStack> context) {
        return BlockPos.containing(context.getSource().getPosition());
    }

    private static int villageLanguage(CommandContext<CommandSourceStack> context, BlockPos pos) {
        CommandSourceStack source = context.getSource();
        Optional<String> language = VoiceCommandService.villageLanguage(source.getLevel(), pos);
        String mode = Configs.server().languageMode().id();
        if (language.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language.village.none", pos.toShortString(), mode), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language.village.get", pos.toShortString(), language.get(), mode), false);
        return 1;
    }

    private static int setVillageLanguage(CommandContext<CommandSourceStack> context, BlockPos pos, boolean reassign) {
        CommandSourceStack source = context.getSource();
        Optional<String> language = parseLanguage(source, StringArgumentType.getString(context, "language"));
        if (language.isEmpty()) {
            return 0;
        }
        int reassigned = VoiceCommandService.setVillageLanguage(source.getLevel(), pos, language.get(), reassign);
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language.village.set", pos.toShortString(), language.get()), true);
        if (reassign) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language.village.reassigned", reassigned, language.get()), true);
        }
        return 1;
    }

    private static int setEntityLanguage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Optional<String> language = parseLanguage(context.getSource(), StringArgumentType.getString(context, "language"));
        if (language.isEmpty()) {
            return 0;
        }
        return forEachLiving(context, "targets", entity -> VoiceCommandService.setLanguage(entity, language.get()).isPresent() || hasIdentity(entity),
                count -> Component.translatable("commands.talkingvillagers.language.entity.set", count, language.get()));
    }

    /**
     * Validates a language tag and warns if no voice speaks it; the language is used anyway, e.g. before installing packs.
     */
    private static Optional<String> parseLanguage(CommandSourceStack source, String value) {
        if (!Languages.isValid(value)) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.language.invalid", value));
            return Optional.empty();
        }
        String language = Languages.normalize(value);
        if (Voicepacks.index().languages().stream().noneMatch(installed -> Languages.matches(installed, language))) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language.not_installed", language)
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return Optional.of(language);
    }

    // --- debug ---------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> debug() {
        return Commands.literal("debug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stats").executes(TalkingVillagersCommand::stats))
                .then(Commands.literal("watch").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    boolean watching = source.getPlayer() != null ? SpeechWatchers.toggle(source.getPlayer()) : SpeechWatchers.toggleConsole();
                    source.sendSuccess(() -> Component.translatable(watching
                            ? "commands.talkingvillagers.watch.on" : "commands.talkingvillagers.watch.off"), false);
                    return watching ? 1 : 0;
                }))
                .then(Commands.literal("say")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("voice", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                Voicepacks.index().voices().keySet().stream().map(TalkingVillagersCommand::toId).toList(), builder))
                                        .then(Commands.argument("event", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        EventCatalog.all().stream().map(VoiceEvent::id).toList(), builder))
                                                .executes(TalkingVillagersCommand::debugSay)))));
    }

    private static int stats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<VoiceRef, Integer> usage = VoiceCommandService.usage(source.getLevel());
        int total = usage.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.stats.none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.stats.header", total, usage.size()), false);
        usage.entrySet().stream()
                .sorted(Map.Entry.<VoiceRef, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.stats.entry",
                        entry.getValue(), entry.getKey().toString()), false));
        return total;
    }

    /** Lets any entity say a line of any voice, to try out voicepacks. */
    private static int debugSay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
        Identifier voiceId = IdentifierArgument.getId(context, "voice");
        String eventId = StringArgumentType.getString(context, "event");
        CommandSourceStack source = context.getSource();

        Optional<ResolvedVoice> voice = Voicepacks.index().voice(new VoiceRef(voiceId.getNamespace(), voiceId.getPath()));
        if (voice.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.say.unknown_voice", voiceId.toString()));
            return 0;
        }
        Optional<VoiceEvent> event = EventCatalog.byId(eventId);
        if (event.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.say.unknown_event", eventId));
            return 0;
        }
        int spoken = 0;
        for (Entity entity : targets) {
            float pitch = voice.get().voice().pitch().sample(RANDOM);
            if (SpeechService.speak(entity, EntityVoices.state(entity), voice.get(), event.get(), EntityVoices.context(entity), pitch, 1.0f, true, null)) {
                spoken++;
            }
        }
        int count = spoken;
        if (count == 0) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.say.nothing", voiceId.toString(), eventId));
        } else {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.say.success", count, targets.size()), false);
        }
        return count;
    }

    // --- suggestions and helpers -----------------------------------------------------------------------------------

    /** Only voices that fit the type and age of the selected entities. */
    private static CompletableFuture<Suggestions> suggestFittingVoices(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Collection<? extends Entity> targets;
        try {
            targets = EntityArgument.getEntities(context, "targets");
        } catch (CommandSyntaxException | RuntimeException e) {
            targets = List.of();
        }
        Collection<? extends Entity> entities = targets;
        List<Identifier> ids = Voicepacks.index().voices().values().stream()
                .filter(voice -> entities.isEmpty() || entities.stream().anyMatch(entity -> EntityVoices.kindOf(entity)
                        .filter(kind -> voice.voice().entities().contains(kind))
                        .filter(kind -> !(entity instanceof LivingEntity living) || living.isBaby() == voice.voice().child())
                        .isPresent()))
                .map(voice -> toId(voice.ref()))
                .toList();
        return SharedSuggestionProvider.suggestResource(ids, builder);
    }

    /** Events the target's voice has lines for. */
    private static CompletableFuture<Suggestions> suggestEventsOfTarget(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> events;
        try {
            Entity entity = EntityArgument.getEntity(context, "target");
            Optional<ResolvedVoice> voice = entity instanceof LivingEntity living ? EntityVoices.voice(living) : Optional.empty();
            events = EventCatalog.all().stream().filter(event -> voice.map(v -> v.voice().hasLines(event)).orElse(true)).map(VoiceEvent::id).toList();
        } catch (CommandSyntaxException | RuntimeException e) {
            events = EventCatalog.all().stream().map(VoiceEvent::id).toList();
        }
        return SharedSuggestionProvider.suggest(events, builder);
    }

    private static CompletableFuture<Suggestions> suggestLanguages(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Voicepacks.index().languages(), builder);
    }

    private interface EntityAction {
        boolean apply(LivingEntity entity);
    }

    /**
     * Runs an action for every target that can have a voice and reports how many succeeded and how many were skipped.
     */
    private static int forEachLiving(CommandContext<CommandSourceStack> context, String argument, EntityAction action,
                                     Function<Integer, Component> success) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, argument);
        CommandSourceStack source = context.getSource();
        int changed = 0;
        int skipped = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living && EntityVoices.kindOf(living).isPresent() && action.apply(living)) {
                changed++;
            } else {
                skipped++;
            }
        }
        reportSkipped(source, skipped, targets.size());
        int count = changed;
        if (count > 0) {
            source.sendSuccess(() -> success.apply(count), true);
        }
        return count;
    }

    private static boolean hasIdentity(LivingEntity entity) {
        return VoiceCommandService.identity(entity).isPresent();
    }

    private static void reportSkipped(CommandSourceStack source, int skipped, int total) {
        if (skipped > 0) {
            source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.skipped", skipped, total).withStyle(ChatFormatting.YELLOW), false);
        }
    }

    private static MutableComponent clickable(MutableComponent text, String command) {
        return text.withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }

    private static Component age(boolean child) {
        return Component.translatable(child ? "commands.talkingvillagers.word.child" : "commands.talkingvillagers.word.adult");
    }

    private static Component gender(Gender gender) {
        return Component.translatable("commands.talkingvillagers.word." + gender.id());
    }

    private static Component state(VoiceIdentity identity) {
        return Component.translatable("commands.talkingvillagers.word." + (identity.muted() ? "muted" : identity.manual() ? "manual" : "auto"));
    }

    private static Identifier toId(VoiceRef ref) {
        return Identifier.fromNamespaceAndPath(ref.packId(), ref.voiceId());
    }
}
