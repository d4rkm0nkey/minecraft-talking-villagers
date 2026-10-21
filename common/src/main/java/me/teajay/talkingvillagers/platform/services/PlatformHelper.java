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

    /** The version of this mod, e.g. {@code 2.0.0-beta.5+mc26.3}. */
    String getModVersion();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
