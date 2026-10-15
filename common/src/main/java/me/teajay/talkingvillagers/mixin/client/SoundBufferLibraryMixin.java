package me.teajay.talkingvillagers.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import me.teajay.talkingvillagers.client.VoiceAudio;
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

/**
 * Loads voice lines straight from the {@code .voicepack} files, so installing a pack needs no resource reload.
 * Buffers go into the vanilla cache and are released with it.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Shadow
    @Final
    private Map<Identifier, CompletableFuture<SoundBuffer>> cache;

    @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
    private void talkingvillagers$loadVoiceLine(Identifier location, CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        if (!VoiceAudio.isVoice(location)) {
            return;
        }
        cir.setReturnValue(cache.computeIfAbsent(location, l -> CompletableFuture.supplyAsync(() -> {
            try (InputStream in = VoiceAudio.open(l).orElseThrow(); FiniteAudioStream stream = new JOrbisAudioStream(in)) {
                ByteBuffer data = stream.readAll();
                return new SoundBuffer(data, stream.getFormat());
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.nonCriticalIoPool())));
    }
}
