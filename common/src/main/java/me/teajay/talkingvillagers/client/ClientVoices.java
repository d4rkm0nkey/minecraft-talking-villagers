package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.config.ClientConfig;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.network.ClientPacksPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.network.SpeakPayload;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client side handling of the mod's payloads. Runs on the client thread.
 */
public final class ClientVoices {
    /** The line each entity is saying right now, by entity id. */
    private static final Map<Integer, VoiceSoundInstance> PLAYING = new HashMap<>();
    private static @Nullable ServerPacksPayload serverPacks;

    private ClientVoices() {
    }

    public static void onServerPacks(ServerPacksPayload payload) {
        serverPacks = payload;
        answerServer();
    }

    /**
     * After the local packs changed, tell the server again which of its packs we have.
     */
    public static void onLocalPacksReloaded() {
        Minecraft.getInstance().execute(() -> {
            if (serverPacks != null && Minecraft.getInstance().getConnection() != null) {
                answerServer();
            }
        });
    }

    public static void onDisconnect() {
        serverPacks = null;
        PLAYING.clear();
    }

    /**
     * Server packs this client does not have.
     */
    public static List<ServerPacksPayload.PackInfo> missingPacks() {
        if (serverPacks == null) {
            return List.of();
        }
        return serverPacks.packs().stream().filter(pack -> Voicepacks.index().byHash(pack.sha256()).isEmpty()).toList();
    }

    private static void answerServer() {
        Set<String> available = serverPacks.packs().stream()
                .map(ServerPacksPayload.PackInfo::sha256)
                .filter(hash -> Voicepacks.index().byHash(hash).isPresent())
                .collect(Collectors.toSet());
        List<ServerPacksPayload.PackInfo> missing = missingPacks();
        if (!missing.isEmpty()) {
            TalkingVillagers.LOG.info("The server uses voicepacks you do not have: {}",
                    missing.stream().map(pack -> pack.name() + " " + pack.version()).collect(Collectors.joining(", ")));
        }
        Services.NETWORK.sendToServer(new ClientPacksPayload(available));
    }

    /**
     * Whether the server sends voice lines for this entity to this client, so vanilla sounds the client plays on its
     * own must be skipped.
     */
    public static boolean isVoiced(Entity entity) {
        return Services.ATTACHMENTS.getIdentity(entity).flatMap(VoiceIdentity::voice).filter(ClientVoices::canPlay).isPresent();
    }

    /**
     * Whether this client has exactly the pack file the server uses for the voice.
     */
    public static boolean canPlay(VoiceRef voice) {
        if (serverPacks == null) {
            return false;
        }
        return serverPacks.packs().stream()
                .anyMatch(pack -> pack.id().equals(voice.packId()) && Voicepacks.index().byHash(pack.sha256()).isPresent());
    }

    /**
     * Whether the entity is saying a line right now.
     */
    public static boolean isSpeaking(int entityId) {
        VoiceSoundInstance instance = PLAYING.get(entityId);
        return instance != null && Minecraft.getInstance().getSoundManager().isActive(instance);
    }

    public static void onSpeak(SpeakPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entityId());
        Optional<VoiceLine> line = findLine(payload);
        if (entity == null || line.isEmpty()) {
            return;
        }

        PLAYING.values().removeIf(instance -> !minecraft.getSoundManager().isActive(instance));
        VoiceSoundInstance previous = PLAYING.remove(entity.getId());
        if (previous != null) {
            minecraft.getSoundManager().stop(previous);
        }

        ClientConfig config = Configs.client();
        Component generic = Component.translatable("subtitles.talkingvillagers.speaks", entity.getDisplayName());
        Component subtitle = switch (config.subtitles()) {
            case TRANSCRIPT -> line.get().text().<Component>map(Component::literal).orElse(generic);
            case GENERIC -> generic;
            case OFF -> null;
        };
        VoiceSoundInstance instance = new VoiceSoundInstance(
                VoiceAudio.soundLocation(payload.packHash(), line.get().file()), entity, payload.volume() * config.volume(), payload.pitch(), subtitle);
        PLAYING.put(entity.getId(), instance);
        minecraft.getSoundManager().play(instance);
    }

    private static Optional<VoiceLine> findLine(SpeakPayload payload) {
        Optional<Voice> voice = Voicepacks.index().byHash(payload.packHash())
                .flatMap(LoadedPack::pack)
                .flatMap(pack -> pack.voice(payload.voiceId()));
        Optional<VoiceEvent> event = EventCatalog.byId(payload.eventId());
        if (voice.isEmpty() || event.isEmpty()) {
            TalkingVillagers.LOG.debug("Cannot play {}: unknown pack, voice or event", payload);
            return Optional.empty();
        }
        List<VoiceLine> lines = voice.get().lines(event.get());
        if (payload.lineIndex() < 0 || payload.lineIndex() >= lines.size()) {
            return Optional.empty();
        }
        return Optional.of(lines.get(payload.lineIndex()));
    }
}
