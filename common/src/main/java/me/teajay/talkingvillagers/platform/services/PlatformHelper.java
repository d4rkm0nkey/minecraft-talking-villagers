package me.teajay.talkingvillagers.platform.services;

import java.nio.file.Path;

public interface PlatformHelper {
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /**
     * The game directory, i.e. {@code .minecraft} on the client or the server root on a dedicated server.
     */
    Path getGameDir();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
