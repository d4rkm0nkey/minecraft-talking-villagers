package me.teajay.talkingvillagers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.language.LanguageAnchors;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.language.LanguageData;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import me.teajay.talkingvillagers.server.SpeechService;
import me.teajay.talkingvillagers.voice.EntityVoices;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * {@code /talkingvillagers} (alias {@code /tv}).
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
                .then(Commands.literal("packs")
                        .then(Commands.literal("list").executes(TalkingVillagersCommand::listPacks))
                        .then(Commands.literal("reload")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(TalkingVillagersCommand::reloadPacks)))
                .then(Commands.literal("debug")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("language")
                                .executes(context -> language(context, BlockPos.containing(context.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> language(context, BlockPosArgument.getBlockPos(context, "pos")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(TalkingVillagersCommand::info)))
                        .then(Commands.literal("say")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("voice", IdentifierArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        Voicepacks.index().voices().keySet().stream().map(TalkingVillagersCommand::toId).toList(), builder))
                                                .then(Commands.argument("event", StringArgumentType.word())
                                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                                EventCatalog.all().stream().map(VoiceEvent::id).toList(), builder))
                                                        .executes(TalkingVillagersCommand::say)))))));
        dispatcher.register(Commands.literal("tv").redirect(root));
    }

    private static Identifier toId(VoiceRef ref) {
        return Identifier.fromNamespaceAndPath(ref.packId(), ref.voiceId());
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
            source.sendSuccess(() -> Component.translatable(active ? "commands.talkingvillagers.packs.entry" : "commands.talkingvillagers.packs.inactive",
                    pack.name(), pack.version(), pack.voices().size(), pack.language(), file)
                    .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }
        return index.active().size();
    }

    private static int reloadAll(CommandContext<CommandSourceStack> context) {
        Configs.load();
        context.getSource().sendSuccess(() -> Component.translatable("commands.talkingvillagers.config.reloaded", Configs.serverFile().toString()), true);
        return reloadPacks(context);
    }

    private static int language(CommandContext<CommandSourceStack> context, BlockPos pos) {
        CommandSourceStack source = context.getSource();
        LanguageData data = LanguageData.get(source.getLevel());
        int radius = Configs.server().villageRadius();
        String village = data.villages().find(pos.getX(), pos.getY(), pos.getZ(), radius).map(LanguageAnchors.Anchor::language).orElse("-");
        String outsider = data.outsiders().find(pos.getX(), pos.getY(), pos.getZ(), radius).map(LanguageAnchors.Anchor::language).orElse("-");
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.language",
                pos.toShortString(), Configs.server().languageMode().id(), village, outsider), false);
        return 1;
    }

    private static int reloadPacks(CommandContext<CommandSourceStack> context) {
        PackIndex index = Voicepacks.reload();
        long broken = index.all().stream().filter(pack -> !pack.isUsable()).count();
        context.getSource().sendSuccess(() -> Component.translatable("commands.talkingvillagers.packs.reloaded",
                index.active().size(), index.voices().size(), broken), true);
        return index.active().size();
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();
        if (entity instanceof LivingEntity living) {
            // assigns a voice first if the entity has none yet
            EntityVoices.voice(living);
        }
        Optional<VoiceIdentity> identity = Services.ATTACHMENTS.getIdentity(entity);
        if (identity.isEmpty()) {
            source.sendFailure(Component.translatable("commands.talkingvillagers.info.none", entity.getDisplayName()));
            return 0;
        }
        VoiceIdentity id = identity.get();
        String voice = id.voice().map(VoiceRef::toString).orElse("-");
        source.sendSuccess(() -> Component.translatable("commands.talkingvillagers.info", entity.getDisplayName(), voice,
                id.gender().id(), id.child() ? "child" : "adult", String.format(Locale.ROOT, "%.2f", id.pitch()),
                id.language().orElse("-"), id.manual() ? "manual" : "auto"), false);
        return 1;
    }

    private static int say(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
}
