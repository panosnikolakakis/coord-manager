package com.panosnikolakakis.coordmanager.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The saved locations for one world: the in-memory {@link LocationStore} plus the
 * {@link LocationFile} behind it. Every change is written to disk straight away and
 * rolled back in memory if the write fails, so the two never disagree.
 *
 * <p>Still pure Java: the Minecraft side only hands it paths.
 */
public final class LocationRepository {
	private final LocationFile file;
	private final LocationStore store;
	private final Set<UUID> waypointsHiddenFor;

	private LocationRepository(LocationFile file, LocationFile.Data data) {
		this.file = file;
		this.store = new LocationStore(data.locations());
		this.waypointsHiddenFor = new HashSet<>(data.waypointsHiddenFor());
	}

	/**
	 * Opens the repository for a world.
	 *
	 * @param dataFile    where {@code locations.json} lives
	 * @param legacyFiles 1.x {@code locations.txt} files to import the first time 2.x runs on this world;
	 *                    each one imported is renamed to {@code locations.txt.migrated}
	 * @param log         receives human-readable notes about imports and recoveries
	 */
	public static LocationRepository open(Path dataFile, List<Path> legacyFiles, Consumer<String> log) throws IOException {
		LocationFile file = new LocationFile(dataFile);
		boolean firstRun = !file.exists();

		LocationFile.Data data = LocationFile.Data.empty();
		if (!firstRun) {
			try {
				data = file.load();
			} catch (LocationFile.CorruptFileException e) {
				Path backup = file.backUpCorruptFile();
				log.accept(e.getMessage() + ". Moved it to " + backup.getFileName() + " and started with an empty list.");
			}
		}

		LocationRepository repository = new LocationRepository(file, data);
		if (firstRun) {
			repository.importLegacy(legacyFiles, log);
		}
		return repository;
	}

	private void importLegacy(List<Path> legacyFiles, Consumer<String> log) throws IOException {
		List<Path> found = legacyFiles.stream().filter(Files::isRegularFile).distinct().toList();
		if (found.isEmpty()) {
			return;
		}

		for (Path legacy : found) {
			LegacyImporter.Result result = LegacyImporter.read(legacy);
			result.locations().forEach(store::put);
			log.accept("Imported " + result.locations().size() + " location(s) from " + legacy + ".");
			for (String line : result.skippedLines()) {
				log.accept("Skipped a line in " + legacy + " that couldn't be read: " + line);
			}
		}
		persist();

		for (Path legacy : found) {
			try {
				Files.move(legacy, legacy.resolveSibling(legacy.getFileName() + ".migrated"));
			} catch (IOException e) {
				log.accept("Imported " + legacy + " but couldn't rename it (" + e.getMessage() + "). It won't be imported again.");
			}
		}
	}

	public Optional<Location> get(String name) {
		return store.get(name);
	}

	public List<Location> all() {
		return store.all();
	}

	public List<Location> inDimension(String dimension) {
		return store.inDimension(dimension);
	}

	public List<String> names() {
		return store.names();
	}

	public int size() {
		return store.size();
	}

	/** Adds or replaces a location and saves. Returns the location it replaced, if any. */
	public synchronized Optional<Location> save(Location location) throws IOException {
		Optional<Location> previous = store.put(location);
		try {
			persist();
		} catch (IOException e) {
			store.remove(location.name());
			previous.ifPresent(store::put);
			throw e;
		}
		return previous;
	}

	/** Removes a location and saves. Returns what was removed, if anything. */
	public synchronized Optional<Location> delete(String name) throws IOException {
		Optional<Location> removed = store.remove(name);
		if (removed.isPresent()) {
			try {
				persist();
			} catch (IOException e) {
				store.put(removed.get());
				throw e;
			}
		}
		return removed;
	}

	public boolean waypointsHidden(UUID playerId) {
		return waypointsHiddenFor.contains(playerId);
	}

	public synchronized void setWaypointsHidden(UUID playerId, boolean hidden) throws IOException {
		boolean changed = hidden ? waypointsHiddenFor.add(playerId) : waypointsHiddenFor.remove(playerId);
		if (!changed) {
			return;
		}
		try {
			persist();
		} catch (IOException e) {
			if (hidden) {
				waypointsHiddenFor.remove(playerId);
			} else {
				waypointsHiddenFor.add(playerId);
			}
			throw e;
		}
	}

	private void persist() throws IOException {
		file.save(store.all(), waypointsHiddenFor);
	}
}
