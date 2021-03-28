package me.teajay.talking.villagers.common.sound;

import java.io.File;

import me.teajay.talking.villagers.TalkingVillagers;
import me.teajay.talking.villagers.mixin.VillagerEntityMixin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class VillagerVoiceManager {
    public enum Reason {
        DEATH,
        RANDOM,
        TRADE,
        GOSSIP
    }
    public static final Identifier sound_id = new Identifier(TalkingVillagers.MODID + ":greeting");
    public static SoundEvent sound_event = new SoundEvent(sound_id); 

    // unused
    public static void initialiseVoices() {
        Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
//        File resourceFolder = new File(FabricLoader.getInstance().getConfigDir()+ "/" + TalkingVillagers.MODID);
//        System.out.println(resourceFolder);
//        String soundName = "";
//        final Identifier sound_id = new Identifier(TalkingVillagers.MODID + ":" + soundName);
//        SoundEvent sound_event = new SoundEvent(sound_id);
//        Registry.register(Registry.SOUND_EVENT, sound_id, sound_event);
    }

    public VillagerVoiceManager() {
    }
    public void speak(World world, Reason reason, BlockPos pos) {
        if(reason.equals(Reason.RANDOM)) {
            if (!world.isClient) {
                world.playSound(null, pos, sound_event, SoundCategory.VOICE, 1f, 1f);
            }
        }
    }
    
}