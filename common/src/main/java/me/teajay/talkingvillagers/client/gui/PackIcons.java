package me.teajay.talkingvillagers.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.pack.Voicepacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code icon.png} of each pack as texture, loaded on first use.
 */
public final class PackIcons {
    public static final Identifier DEFAULT = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Map<String, Identifier> ICONS = new HashMap<>();

    private PackIcons() {
    }

    public static Identifier get(LoadedPack pack) {
        if (!pack.hasIcon()) {
            return DEFAULT;
        }
        return ICONS.computeIfAbsent(pack.sha256(), hash -> {
            try {
                NativeImage image = NativeImage.read(Voicepacks.repository().readFile(hash, VoicePack.ICON));
                Identifier id = TalkingVillagers.id("pack_icon/" + hash);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
                return id;
            } catch (Exception e) {
                TalkingVillagers.LOG.warn("Cannot load icon of {}", pack.source().getFileName(), e);
                return DEFAULT;
            }
        });
    }
}
