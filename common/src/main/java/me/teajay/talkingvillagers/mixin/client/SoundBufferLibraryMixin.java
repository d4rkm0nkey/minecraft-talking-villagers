package me.teajay.talkingvillagers.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import me.teajay.talkingvillagers.client.VoiceAudio;
import me.teajay.talkingvillagers.client.VoiceBufferCache;
import net.minecraft.client.sounds.FiniteAudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

/**
 * Loads voice lines straight from the {@code .voicepack} files, so installing a pack needs no resource reload.
 * Buffers go into the vanilla cache and are released with it, or when their pack is gone.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin implements VoiceBufferCache {
    @Shadow
    @Final
    private Map<Identifier, CompletableFuture<SoundBuffer>> cache;

    @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
    private void talkingvillagers$loadVoiceLine(Identifier location, CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        if (!VoiceAudio.isVoice(location)) {
            return;
        }
        CompletableFuture<SoundBuffer> cached = cache.get(location);
        if (cached == null || cached.isCompletedExceptionally()) {
            // a failed read is tried again next time, e.g. after the pack was downloaded
            cached = CompletableFuture.supplyAsync(() -> {
                try (InputStream in = VoiceAudio.open(location).orElseThrow(); FiniteAudioStream stream = new JOrbisAudioStream(in)) {
                    ByteBuffer data = stream.readAll();
                    return new SoundBuffer(data, stream.getFormat());
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.nonCriticalIoPool());
            cache.put(location, cached);
        }
        cir.setReturnValue(cached);
    }

    @Override
    public void talkingvillagers$dropVoices(Predicate<String> keep) {
        cache.entrySet().removeIf(entry -> {
            boolean drop = VoiceAudio.packHash(entry.getKey()).filter(hash -> !keep.test(hash)).isPresent();
            if (drop) {
                entry.getValue().thenAccept(SoundBuffer::discardAlBuffer);
            }
            return drop;
        });
    }
}
