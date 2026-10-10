package com.panosnikolakakis.coordmanager;

import com.panosnikolakakis.coordmanager.core.Config;
import com.panosnikolakakis.coordmanager.core.Location;
import com.panosnikolakakis.coordmanager.core.LocationRepository;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Connects the pure-Java {@link LocationRepository} to a running server: works out where
 * the files live, and refreshes everyone's waypoints when the list changes.
 */
final class LocationService {
	private final MinecraftServer server;
	private final LocationRepository repository;
	private final Waypoints waypoints;

	private LocationService(MinecraftServer server, LocationRepository repository, Waypoints waypoints) {
		this.server = server;
		this.repository = repository;
		this.waypoints = waypoints;
	}

	static LocationService start(MinecraftServer server, Config config) throws IOException {
		// Saved in the world folder for both singleplayer and dedicated servers,
		// so each world has its own list and it moves with the world.
		Path dir = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().resolve(CoordManager.MOD_ID);

		// Where 1.x kept its files: the world folder in singleplayer, ./config on dedicated servers.
		List<Path> legacy = new ArrayList<>();
		legacy.add(dir.resolve("locations.txt"));
		if (server.isDedicatedServer()) {
			legacy.add(FabricLoader.getInstance().getConfigDir().resolve(CoordManager.MOD_ID).resolve("locations.txt"));
		}

		LocationRepository repository = LocationRepository.open(dir.resolve("locations.json"), legacy, CoordManager.LOGGER::info);
		CoordManager.LOGGER.info("Loaded {} saved location(s).", repository.size());
		return new LocationService(server, repository, new Waypoints(config.waypoints(), repository));
	}

	Waypoints waypoints() {
		return waypoints;
	}

	Optional<Location> get(String name) {
		return repository.get(name);
	}

	List<Location> all() {
		return repository.all();
	}

	List<String> names() {
		return repository.names();
	}

	Optional<Location> save(Location location) throws IOException {
		Optional<Location> previous = repository.save(location);
		waypoints.refreshAll(server);
		return previous;
	}

	Optional<Location> delete(String name) throws IOException {
		Optional<Location> removed = repository.delete(name);
		if (removed.isPresent()) {
			waypoints.refreshAll(server);
		}
		return removed;
	}

	boolean waypointsHidden(UUID playerId) {
		return repository.waypointsHidden(playerId);
	}

	void setWaypointsHidden(UUID playerId, boolean hidden) throws IOException {
		repository.setWaypointsHidden(playerId, hidden);
	}

	/** The loaded level for a saved dimension id, or {@code null} if this server doesn't have it. */
	ServerLevel level(String dimensionId) {
		Identifier id = Identifier.tryParse(dimensionId);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}
}
