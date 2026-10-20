package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.mixin.client.SoundEngineAccessor;
import me.teajay.talkingvillagers.mixin.client.SoundManagerAccessor;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Maps voice lines to sound ids and back. Minecraft asks the sound buffer library for
 * {@code talkingvillagers:sounds/voice/<pack sha256>/<path>.ogg}; {@link me.teajay.talkingvillagers.mixin.client.SoundBufferLibraryMixin}
 * answers those requests from the {@code .voicepack} file instead of the resource packs.
 */
public final class VoiceAudio {
    private static final String SOUNDS_DIR = "sounds/";
    private static final String OGG = ".ogg";
    private static final String VOICE_PREFIX = "voice/";
    private static final String FILE_PREFIX = SOUNDS_DIR + VOICE_PREFIX;

    private VoiceAudio() {
    }

    /**
     * Sound location for a file inside a pack, e.g. {@code sounds/anna/hurt/au.ogg}.
     */
    public static Identifier soundLocation(String packHash, String file) {
        String path = file.substring(SOUNDS_DIR.length(), file.length() - OGG.length());
        return TalkingVillagers.id(VOICE_PREFIX + packHash + "/" + path);
    }

    /**
     * @param resource the file location from {@link Sound#getPath()}
     */
    public static Optional<InputStream> open(Identifier resource) throws IOException {
        if (!resource.getNamespace().equals(TalkingVillagers.MOD_ID) || !resource.getPath().startsWith(FILE_PREFIX)) {
            return Optional.empty();
        }
        String rest = resource.getPath().substring(FILE_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }
        String packHash = rest.substring(0, slash);
        String file = SOUNDS_DIR + rest.substring(slash + 1);
        return Optional.of(new ByteArrayInputStream(Voicepacks.repository().readFile(packHash, file)));
    }

    public static boolean isVoice(Identifier resource) {
        return resource.getNamespace().equals(TalkingVillagers.MOD_ID) && resource.getPath().startsWith(FILE_PREFIX);
    }

    /** The SHA-256 of the pack a voice line sound comes from. */
    public static Optional<String> packHash(Identifier resource) {
        if (!isVoice(resource)) {
            return Optional.empty();
        }
        String rest = resource.getPath().substring(FILE_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? Optional.empty() : Optional.of(rest.substring(0, slash));
    }

    /**
     * Releases the sounds of packs that were removed, so they don't stay in memory until the next resource reload.
     */
    public static void onPacksReloaded() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            SoundManager sounds = minecraft.getSoundManager();
            if (sounds != null) {
                var library = ((SoundEngineAccessor) ((SoundManagerAccessor) sounds).talkingvillagers$soundEngine()).talkingvillagers$soundBuffers();
                ((VoiceBufferCache) library).talkingvillagers$dropVoices(hash -> Voicepacks.index().byHash(hash).isPresent());
            }
        });
    }
}
