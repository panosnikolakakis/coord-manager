package com.panosnikolakakis.coordmanager.core;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A saved location. Plain data with no Minecraft types, so it can be stored,
 * tested and reasoned about without the game.
 *
 * @param name        display name, exactly as the player typed it (after {@link LocationNames#clean})
 * @param dimension   full dimension id, e.g. {@code minecraft:overworld}
 * @param creatorId   UUID of the player who saved it, or {@code null} for locations imported from 1.x
 * @param creatorName name of that player at save time, or {@code null}
 * @param createdAt   when it was saved, or {@code null} if unknown
 */
public record Location(
		String name,
		String dimension,
		int x,
		int y,
		int z,
		UUID creatorId,
		String creatorName,
		Instant createdAt
) {
	public Location {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(dimension, "dimension");
	}

	/** The case-insensitive key this location is stored under. */
	public String key() {
		return LocationNames.key(name);
	}

	public Optional<UUID> creator() {
		return Optional.ofNullable(creatorId);
	}

	/** {@code true} if the given player saved this location (or nobody is recorded as its owner). */
	public boolean isOwnedByOrUnowned(UUID playerId) {
		return creatorId == null || creatorId.equals(playerId);
	}

	public String coordinates() {
		return x + " " + y + " " + z;
	}

	public double distanceTo(double px, double py, double pz) {
		double dx = (x + 0.5) - px;
		double dy = y - py;
		double dz = (z + 0.5) - pz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
