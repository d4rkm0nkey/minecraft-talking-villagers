package me.teajay.talkingvillagers.language;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.language.LanguageAnchors;
import me.teajay.talkingvillagers.core.language.LanguageChooser;
import me.teajay.talkingvillagers.core.language.LanguageMode;
import me.teajay.talkingvillagers.core.language.LanguageSettings;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SplittableRandom;

/**
 * Decides which language an entity without a language gets.
 */
public final class LanguageResolver {
    /**
     * Freshly generated villagers find their bell within a few seconds. Until then (or this many ticks) the voice
     * is not assigned yet, so the villager does not end up with a language of its own.
     */
    public static final int MAX_WAIT_FOR_VILLAGE_TICKS = 600;
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private LanguageResolver() {
    }

    /**
     * @param pending  wait and ask again later
     * @param language the language to use; empty means any language
     */
    public record Resolution(boolean pending, Optional<String> language) {
        static final Resolution ANY = new Resolution(false, Optional.empty());
        static final Resolution PENDING = new Resolution(true, Optional.empty());

        static Resolution of(Optional<String> language) {
            return new Resolution(false, language);
        }
    }

    public static Resolution resolve(LivingEntity entity, EntityKind kind, ServerLevel level, PackIndex index, LanguageSettings settings) {
        List<String> candidates = LanguageChooser.candidates(index.voicesFor(kind), settings.allowed());
        if (settings.mode() == LanguageMode.ANY || candidates.isEmpty()) {
            return Resolution.ANY;
        }
        if (settings.mode() == LanguageMode.PREFERRED) {
            return Resolution.of(LanguageChooser.preferred(candidates, settings.preferred()));
        }

        LanguageData data = LanguageData.get(level);
        return switch (kind) {
            case VILLAGER -> village((Villager) entity, level, data, index, settings);
            case WANDERING_TRADER -> {
                if (settings.traderLanguage() == LanguageSettings.TraderLanguage.NEAREST_VILLAGE) {
                    BlockPos pos = entity.blockPosition();
                    Optional<String> village = data.villages().find(pos.getX(), pos.getY(), pos.getZ(), settings.villageRadius() * 4)
                            .map(LanguageAnchors.Anchor::language);
                    if (village.isPresent()) {
                        yield Resolution.of(village);
                    }
                }
                yield Resolution.of(LanguageChooser.random(candidates, RANDOM));
            }
            case PILLAGER, VINDICATOR, EVOKER, ILLUSIONER -> {
                Raid raid = entity instanceof Raider raider ? raider.getCurrentRaid() : null;
                yield raid != null ? raid(raid, level, data, candidates, index, settings) : outsider(entity, data, candidates, settings);
            }
            case WITCH -> outsider(entity, data, candidates, settings);
        };
    }

    private static Resolution village(Villager villager, ServerLevel level, LanguageData data, PackIndex index, LanguageSettings settings) {
        List<String> candidates = LanguageChooser.candidates(index.voicesFor(EntityKind.VILLAGER), settings.allowed());
        Optional<BlockPos> bell = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT)
                .filter(point -> point.dimension() == level.dimension())
                .map(GlobalPos::pos);
        BlockPos pos = bell.orElse(villager.blockPosition());
        Optional<LanguageAnchors.Anchor> anchor = data.villages().find(pos.getX(), pos.getY(), pos.getZ(), settings.villageRadius());
        if (anchor.isPresent()) {
            return Resolution.of(Optional.of(anchor.get().language()));
        }
        if (bell.isEmpty() && villager.tickCount < MAX_WAIT_FOR_VILLAGE_TICKS) {
            return Resolution.PENDING;
        }
        Optional<String> language = LanguageChooser.random(candidates, RANDOM);
        language.ifPresent(chosen -> {
            data.villages().add(pos.getX(), pos.getY(), pos.getZ(), chosen);
            data.setDirty();
        });
        return Resolution.of(language);
    }

    /** Witch huts, outposts and mansions: everyone near the same spot shares a language. */
    private static Resolution outsider(LivingEntity entity, LanguageData data, List<String> candidates, LanguageSettings settings) {
        BlockPos pos = entity.blockPosition();
        Optional<LanguageAnchors.Anchor> anchor = data.outsiders().find(pos.getX(), pos.getY(), pos.getZ(), settings.villageRadius());
        if (anchor.isPresent()) {
            return Resolution.of(Optional.of(anchor.get().language()));
        }
        Optional<String> language = LanguageChooser.random(candidates, RANDOM);
        language.ifPresent(chosen -> {
            data.outsiders().add(pos.getX(), pos.getY(), pos.getZ(), chosen);
            data.setDirty();
        });
        return Resolution.of(language);
    }

    /** All raiders of one raid speak the same language. */
    private static Resolution raid(Raid raid, ServerLevel level, LanguageData data, List<String> candidates, PackIndex index, LanguageSettings settings) {
        OptionalInt raidId = level.getRaids().getId(raid);
        if (raidId.isEmpty()) {
            return Resolution.of(LanguageChooser.random(candidates, RANDOM));
        }
        Optional<String> known = data.raidLanguage(raidId.getAsInt());
        if (known.isPresent()) {
            return Resolution.of(known);
        }
        Optional<String> language = LanguageChooser.random(candidates, RANDOM);
        if (settings.raidLanguage() == LanguageSettings.RaidLanguage.OPPOSITE_OF_VILLAGE) {
            BlockPos center = raid.getCenter();
            Optional<String> villageLanguage = data.villages().find(center.getX(), center.getY(), center.getZ(), settings.villageRadius())
                    .map(LanguageAnchors.Anchor::language);
            if (villageLanguage.isPresent()) {
                language = LanguageChooser.randomExcept(candidates, villageLanguage.get(), RANDOM);
            }
        }
        language.ifPresent(chosen -> data.setRaidLanguage(raidId.getAsInt(), chosen, id -> level.getRaids().get(id) != null));
        return Resolution.of(language);
    }
}
