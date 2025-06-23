package com.panosnikolakakis.coordmanager.utils;

import com.panosnikolakakis.coordmanager.commands.LocationSave;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PlayerJoinListener {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            Identifier currentDimension = player.getWorld().getRegistryKey().getValue();
            LocationSave.syncWaypoints(player, currentDimension);
        });
    }
}
