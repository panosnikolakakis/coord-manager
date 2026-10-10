package com.panosnikolakakis.coordmanager;

import com.panosnikolakakis.coordmanager.core.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Entry point. Everything Minecraft-specific is wired up here and in the few classes
 * next to it; the {@code core} package holds the logic and has no Minecraft imports.
 */
public class CoordManager implements ModInitializer {
	public static final String MOD_ID = "coordmanager";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static Config config = Config.DEFAULTS;
	private static LocationService service;

	@Override
	public void onInitialize() {
		try {
			config = Config.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		} catch (IOException e) {
			LOGGER.error("Couldn't read the config, using defaults: {}", e.getMessage());
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				LocationCommands.register(dispatcher, config));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				service = LocationService.start(server, config);
			} catch (IOException | RuntimeException e) {
				service = null;
				LOGGER.error("Couldn't load saved locations; Coord Manager commands are disabled for this session.", e);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> service = null);

		// Keep each player's locator bar in step with the dimension they're in.
		ServerPlayerEvents.JOIN.register(player -> {
			if (service != null) service.waypoints().refresh(player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (service != null) service.waypoints().refresh(newPlayer);
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			if (service != null) service.waypoints().refresh(player);
		});
		ServerPlayerEvents.LEAVE.register(player -> {
			if (service != null) service.waypoints().forget(player);
		});

		ServerEntityEvents.ALLOW_LOAD.register(LegacyWaypointCleanup::allowLoad);

		LOGGER.info("Loaded.");
	}

	/** The location service for the running server, or {@code null} if no server is running or loading failed. */
	static LocationService service() {
		return service;
	}

	static Config config() {
		return config;
	}
}
