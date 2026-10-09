package me.teajay.talkingvillagers.platform;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.platform.services.PlatformHelper;
import me.teajay.talkingvillagers.platform.services.PlatformNetworking;

import java.util.ServiceLoader;

/**
 * Looks up the loader specific implementations of the platform services.
 * Each loader registers its implementation in {@code META-INF/services}.
 */
public final class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);
    public static final PlatformNetworking NETWORK = load(PlatformNetworking.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        T service = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation found for service " + clazz.getName()));
        TalkingVillagers.LOG.debug("Loaded {} for service {}", service, clazz);
        return service;
    }
}
