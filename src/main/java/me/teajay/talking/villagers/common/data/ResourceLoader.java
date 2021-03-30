package me.teajay.talking.villagers.common.data;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class ResourceLoader {
    public static void initLoader() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new ResourceListener());
    }
}
