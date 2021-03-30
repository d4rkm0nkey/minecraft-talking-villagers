package me.teajay.talking.villagers.common.data;

import me.teajay.talking.villagers.TalkingVillagers;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public class ResourceListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return new Identifier(TalkingVillagers.MODID, "sounds");
    }

    @Override
    public void apply(ResourceManager manager) {
        // ToDo clean caches
        for (Identifier id : manager.findResources("voices", path -> true)) {
            System.out.println(id);
            try (InputStream stream = manager.getResource(id).getInputStream()) {
                // Consume the stream however you want, medium, rare, or well done.

            } catch (Exception e) {
                System.err.println("Error occurred while loading resource json " + id.toString() + e);

            }
        }
    }
}
