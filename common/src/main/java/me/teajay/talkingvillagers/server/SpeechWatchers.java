package me.teajay.talkingvillagers.server;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.select.PickedLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players (and the server console) that see every spoken line, to test voicepacks: {@code /tv debug watch}.
 */
public final class SpeechWatchers {
    private static final Set<UUID> PLAYERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean console;

    private SpeechWatchers() {
    }

    /**
     * @return true if the player watches now
     */
    public static boolean toggle(ServerPlayer player) {
        if (PLAYERS.remove(player.getUUID())) {
            return false;
        }
        PLAYERS.add(player.getUUID());
        return true;
    }

    public static void onPlayerLeave(ServerPlayer player) {
        PLAYERS.remove(player.getUUID());
    }

    /** The next world starts without watchers, e.g. in singleplayer. */
    public static void onServerStopped() {
        PLAYERS.clear();
        console = false;
    }

    public static boolean toggleConsole() {
        console = !console;
        return console;
    }

    static void notify(Entity entity, ResolvedVoice voice, VoiceEvent requested, PickedLine line) {
        if (PLAYERS.isEmpty() && !console) {
            return;
        }
        String event = requested == line.event() ? requested.id() : requested.id() + " → " + line.event().id();
        String text = line.line().text().map(t -> " \"" + t + "\"").orElse("");
        String message = entity.getName().getString() + " (" + voice.ref() + ") " + event + " #" + line.index() + text;
        if (console) {
            TalkingVillagers.LOG.info("[speech] {}", message);
        }
        MinecraftServer server = entity.level().getServer();
        if (server == null) {
            return;
        }
        for (UUID id : PLAYERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.sendSystemMessage(Component.literal("🗣 " + message).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
