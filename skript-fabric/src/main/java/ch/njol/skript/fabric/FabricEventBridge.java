package ch.njol.skript.fabric;

import ch.njol.skript.core.RuntimeEventContext;
import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.fabric.platform.FabricSkriptPlayerInfo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Bridges Fabric events into the shared Skript runtime.
 * Fires events: load, join, quit. Scripts can use "on load:", "on join:", "on quit:".
 */
final class FabricEventBridge {

    FabricEventBridge() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SkriptBootstrap.fireEvent("load", new RuntimeEventContext("server_started", null));
        });

        ServerPlayConnectionEvents.JOIN.register((ServerGamePacketListenerImpl handler,
            net.fabricmc.fabric.api.networking.v1.PacketSender sender,
            MinecraftServer server) -> {
            ServerPlayer player = handler.getPlayer();
            FabricSkriptPlayerInfo playerInfo = new FabricSkriptPlayerInfo(player);
            SkriptBootstrap.fireEvent("join",
                new RuntimeEventContext("player_join", player.getName().getString(), playerInfo, null));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            FabricSkriptPlayerInfo playerInfo = new FabricSkriptPlayerInfo(player);
            SkriptBootstrap.fireEvent("quit",
                new RuntimeEventContext("player_quit", player.getName().getString(), playerInfo, null));
        });

        ServerWorldEvents.LOAD.register((MinecraftServer server, ServerLevel world) -> {
            String key = world.dimension().toString();
            SkriptBootstrap.fireEvent("world_load",
                new RuntimeEventContext("world_loaded", key, null, null));
        });

        ServerWorldEvents.UNLOAD.register((MinecraftServer server, ServerLevel world) -> {
            String key = world.dimension().toString();
            SkriptBootstrap.fireEvent("world_unload",
                new RuntimeEventContext("world_unloaded", key, null, null));
        });
    }
}

