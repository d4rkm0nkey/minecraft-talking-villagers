package me.teajay.talkingvillagers.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code icon.png} of each pack as texture, loaded on first use and released when the pack is gone.
 */
public final class PackIcons {
    public static final Identifier DEFAULT = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_SIZE = 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    /** Client thread. Icons that could not be loaded map to {@link #DEFAULT} until the next reload. */
    private static final Map<String, Identifier> ICONS = new HashMap<>();

    private PackIcons() {
    }

    public static Identifier get(LoadedPack pack) {
        if (!pack.hasIcon()) {
            return DEFAULT;
        }
        return ICONS.computeIfAbsent(pack.sha256(), hash -> {
            try {
                byte[] png = Voicepacks.repository().readFile(hash, VoicePack.ICON, MAX_BYTES);
                checkHeader(png);
                NativeImage image = NativeImage.read(png);
                Identifier id = TalkingVillagers.id("pack_icon/" + hash);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
                return id;
            } catch (Exception e) {
                TalkingVillagers.LOG.warn("Cannot load icon of {}: {}", pack.source().getFileName(), e.getMessage());
                return DEFAULT;
            }
        });
    }

    /** Checks size and dimensions from the PNG header, so a huge image is never decoded. */
    private static void checkHeader(byte[] png) throws IOException {
        // signature, IHDR length and type, then width and height
        if (png.length < 24 || !ByteBuffer.wrap(png, 0, 8).equals(ByteBuffer.wrap(PNG_SIGNATURE))
                || !new String(png, 12, 4, StandardCharsets.US_ASCII).equals("IHDR")) {
            throw new IOException("not a PNG file");
        }
        ByteBuffer header = ByteBuffer.wrap(png);
        int width = header.getInt(16);
        int height = header.getInt(20);
        if (width <= 0 || height <= 0 || width > MAX_SIZE || height > MAX_SIZE) {
            throw new IOException("icon is " + Integer.toUnsignedString(width) + "x" + Integer.toUnsignedString(height) + ", at most " + MAX_SIZE + "x" + MAX_SIZE + " is allowed");
        }
    }

    /**
     * Releases the textures of packs that are gone and forgets failed icons, so they are tried again.
     */
    public static void onPacksReloaded() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> ICONS.entrySet().removeIf(entry -> {
            boolean failed = entry.getValue().equals(DEFAULT);
            boolean gone = Voicepacks.index().byHash(entry.getKey()).isEmpty();
            if (gone && !failed) {
                minecraft.getTextureManager().release(entry.getValue());
            }
            return failed || gone;
        }));
    }
}
