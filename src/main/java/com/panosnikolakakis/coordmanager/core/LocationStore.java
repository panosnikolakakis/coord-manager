package com.panosnikolakakis.coordmanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory set of locations, keyed case-insensitively by name and kept sorted.
 * Knows nothing about files or Minecraft; {@link LocationFile} persists it.
 */
public final class LocationStore {
	private final Map<String, Location> byKey = new TreeMap<>();

	public LocationStore() {
	}

	public LocationStore(Collection<Location> initial) {
		initial.forEach(this::put);
	}

	public synchronized Optional<Location> get(String name) {
		return Optional.ofNullable(byKey.get(LocationNames.key(name)));
	}

	/** Adds or replaces a location. Returns the one it replaced, if any. */
	public synchronized Optional<Location> put(Location location) {
		return Optional.ofNullable(byKey.put(location.key(), location));
	}

	public synchronized Optional<Location> remove(String name) {
		return Optional.ofNullable(byKey.remove(LocationNames.key(name)));
	}

	/** All locations, sorted by name ignoring case. */
	public synchronized List<Location> all() {
		return List.copyOf(byKey.values());
	}

	public synchronized List<Location> inDimension(String dimension) {
		List<Location> result = new ArrayList<>();
		for (Location location : byKey.values()) {
			if (location.dimension().equals(dimension)) {
				result.add(location);
			}
		}
		return result;
	}

	public synchronized List<String> names() {
		return byKey.values().stream().map(Location::name).toList();
	}

	public synchronized int size() {
		return byKey.size();
	}
}
