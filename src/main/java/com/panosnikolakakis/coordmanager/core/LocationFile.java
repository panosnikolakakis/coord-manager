package com.panosnikolakakis.coordmanager.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads and writes {@code coordmanager/locations.json} in the world folder.
 *
 * <p>The JSON is built by hand rather than with Gson reflection so the format
 * stays stable no matter which Gson version a future Minecraft ships, and so
 * old files keep loading: unknown fields are ignored and missing ones get defaults.
 */
public final class LocationFile {
	public static final int FORMAT_VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** Everything Coord Manager saves per world. */
	public record Data(List<Location> locations, Set<UUID> waypointsHiddenFor) {
		public static Data empty() {
			return new Data(List.of(), Set.of());
		}
	}

	/** Thrown when the file exists but can't be understood. The caller decides whether to back it up. */
	public static final class CorruptFileException extends IOException {
		private static final long serialVersionUID = 1L;

		public CorruptFileException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final Path file;

	public LocationFile(Path file) {
		this.file = file;
	}

	public Path path() {
		return file;
	}

	public boolean exists() {
		return Files.exists(file);
	}

	public Data load() throws IOException {
		if (!Files.exists(file)) {
			return Data.empty();
		}
		String json = Files.readString(file, StandardCharsets.UTF_8);
		try {
			return fromJson(JsonParser.parseString(json));
		} catch (JsonParseException | IllegalStateException | UnsupportedOperationException | NumberFormatException e) {
			throw new CorruptFileException("Could not read " + file + ": " + e.getMessage(), e);
		}
	}

	/** Writes to a temporary file first, then swaps it in, so a crash mid-write can't leave a half-written file. */
	public void save(Collection<Location> locations, Collection<UUID> waypointsHiddenFor) throws IOException {
		Files.createDirectories(file.toAbsolutePath().getParent());
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(temp, GSON.toJson(toJson(locations, waypointsHiddenFor)), StandardCharsets.UTF_8);
		try {
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** Moves an unreadable file aside (e.g. {@code locations.json.corrupt-1760000000000}) so it isn't overwritten. */
	public Path backUpCorruptFile() throws IOException {
		Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
		Files.move(file, backup);
		return backup;
	}

	static JsonObject toJson(Collection<Location> locations, Collection<UUID> waypointsHiddenFor) {
		JsonObject root = new JsonObject();
		root.addProperty("formatVersion", FORMAT_VERSION);

		JsonArray array = new JsonArray();
		for (Location location : locations) {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", location.name());
			entry.addProperty("dimension", location.dimension());
			entry.addProperty("x", location.x());
			entry.addProperty("y", location.y());
			entry.addProperty("z", location.z());
			if (location.creatorId() != null) {
				entry.addProperty("creatorId", location.creatorId().toString());
			}
			if (location.creatorName() != null) {
				entry.addProperty("creatorName", location.creatorName());
			}
			if (location.createdAt() != null) {
				entry.addProperty("createdAt", location.createdAt().toString());
			}
			array.add(entry);
		}
		root.add("locations", array);

		JsonArray hidden = new JsonArray();
		waypointsHiddenFor.stream().map(UUID::toString).sorted().forEach(hidden::add);
		root.add("waypointsHiddenFor", hidden);
		return root;
	}

	static Data fromJson(JsonElement element) {
		JsonObject root = element.getAsJsonObject();
		int version = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : FORMAT_VERSION;
		if (version > FORMAT_VERSION) {
			throw new JsonParseException("file was written by a newer Coord Manager (format " + version + ")");
		}

		List<Location> locations = new ArrayList<>();
		if (root.has("locations")) {
			for (JsonElement item : root.getAsJsonArray("locations")) {
				JsonObject entry = item.getAsJsonObject();
				String name = LocationNames.clean(entry.get("name").getAsString());
				if (LocationNames.validate(name).isPresent()) {
					continue;
				}
				locations.add(new Location(
						name,
						entry.get("dimension").getAsString(),
						entry.get("x").getAsInt(),
						entry.get("y").getAsInt(),
						entry.get("z").getAsInt(),
						optionalUuid(entry, "creatorId"),
						optionalString(entry, "creatorName"),
						optionalInstant(entry, "createdAt")
				));
			}
		}

		Set<UUID> hidden = new LinkedHashSet<>();
		if (root.has("waypointsHiddenFor")) {
			for (JsonElement item : root.getAsJsonArray("waypointsHiddenFor")) {
				UUID id = parseUuid(item.getAsString());
				if (id != null) {
					hidden.add(id);
				}
			}
		}
		return new Data(List.copyOf(locations), Set.copyOf(hidden));
	}

	private static String optionalString(JsonObject entry, String key) {
		JsonElement value = entry.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static UUID optionalUuid(JsonObject entry, String key) {
		String value = optionalString(entry, key);
		return value == null ? null : parseUuid(value);
	}

	private static Instant optionalInstant(JsonObject entry, String key) {
		String value = optionalString(entry, key);
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
