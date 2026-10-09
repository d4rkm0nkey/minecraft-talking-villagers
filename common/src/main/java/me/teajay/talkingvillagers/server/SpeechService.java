package me.teajay.talkingvillagers.server;

import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.select.LineContext;
import me.teajay.talkingvillagers.core.select.LineSelector;
import me.teajay.talkingvillagers.core.select.PickedLine;
import me.teajay.talkingvillagers.network.SpeakPayload;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Decides what an entity says and sends it to the players who can hear it.
 *
 * <p>Players whose client has the voicepack get a {@link SpeakPayload}. Everyone else nearby, including players without
 * the mod, gets the vanilla fallback sound, so nobody misses a sound because a pack is missing.
 */
public final class SpeechService {
    /** Vanilla's hearing distance for a sound of volume 1. */
    private static final double HEARING_DISTANCE = 16.0;
    /** Extra pause after a line before the entity speaks again. */
    private static final int PAUSE_TICKS = 10;

    private static final SpeechTracker TRACKER = new SpeechTracker();
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private SpeechService() {
    }

    /**
     * @param interrupt speak even if the entity is still saying something, e.g. when it gets hurt
     * @param fallback  vanilla sound for players without the pack, or null to stay silent for them
     * @return whether a line was said; if false the caller should play its vanilla sound as usual
     */
    public static boolean speak(Entity entity, ResolvedVoice voice, VoiceEvent event, LineContext context,
                                float pitch, float volume, boolean interrupt, @Nullable Holder<SoundEvent> fallback) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isSilent()) {
            return false;
        }
        long gameTime = level.getGameTime();
        if (!interrupt && TRACKER.isSpeaking(entity, gameTime)) {
            return false;
        }
        Optional<LoadedPack> pack = Voicepacks.index().packOf(voice.ref());
        if (pack.isEmpty()) {
            return false;
        }
        Optional<PickedLine> picked = LineSelector.pick(voice.voice(), event, context, TRACKER.lastLine(entity, event.id()), RANDOM);
        if (picked.isEmpty()) {
            return false;
        }

        PickedLine line = picked.get();
        int durationTicks = Math.round(line.line().duration().orElse(2.0f) / pitch * 20) + PAUSE_TICKS;
        TRACKER.spoke(entity, event.id(), line.index(), gameTime, durationTicks);

        String packHash = pack.get().sha256();
        SpeakPayload payload = new SpeakPayload(entity.getId(), packHash, voice.ref().voiceId(), line.event().id(), line.index(), pitch, volume);
        double range = HEARING_DISTANCE * Math.max(1.0f, volume);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) > range * range) {
                continue;
            }
            if (ServerVoices.hasPack(player, packHash) && Services.NETWORK.canSend(player, SpeakPayload.TYPE)) {
                Services.NETWORK.sendToPlayer(player, payload);
            } else if (fallback != null) {
                player.connection.send(new ClientboundSoundEntityPacket(fallback, entity.getSoundSource(), entity, volume, pitch, level.getRandom().nextLong()));
            }
        }
        return true;
    }
}
