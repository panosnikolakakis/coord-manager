package com.panosnikolakakis.coordmanager.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server settings from {@code config/coordmanager.json}.
 *
 * @param teleportRequiresOp     only operators (permission level 2, same as vanilla /tp) can use /loc tp
 * @param protectOthersLocations players can only overwrite or delete locations they saved themselves; operators can change any
 * @param waypoints              show saved locations on the locator bar
 * @param listPageSize           locations per page in /loc list
 */
public record Config(
		boolean teleportRequiresOp,
		boolean protectOthersLocations,
		boolean waypoints,
		int listPageSize
) {
	public static final Config DEFAULTS = new Config(true, true, true, 10);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public Config {
		listPageSize = Math.max(1, Math.min(listPageSize, 50));
	}

	/**
	 * Loads the config, creating it with defaults if it's missing. If keys are missing
	 * (for example after an update adds a new setting) the file is rewritten with them filled in.
	 */
	public static Config loadOrCreate(Path file) throws IOException {
		if (!Files.exists(file)) {
			DEFAULTS.save(file);
			return DEFAULTS;
		}

		JsonObject json;
		try {
			JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (JsonParseException e) {
			throw new IOException("Could not read " + file + ": " + e.getMessage(), e);
		}

		Config config = new Config(
				bool(json, "teleportRequiresOp", DEFAULTS.teleportRequiresOp),
				bool(json, "protectOthersLocations", DEFAULTS.protectOthersLocations),
				bool(json, "waypoints", DEFAULTS.waypoints),
				integer(json, "listPageSize", DEFAULTS.listPageSize)
		);

		if (!config.toJson().keySet().equals(json.keySet())) {
			config.save(file);
		}
		return config;
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.toAbsolutePath().getParent());
		Files.writeString(file, GSON.toJson(toJson()), StandardCharsets.UTF_8);
	}

	JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("teleportRequiresOp", teleportRequiresOp);
		json.addProperty("protectOthersLocations", protectOthersLocations);
		json.addProperty("waypoints", waypoints);
		json.addProperty("listPageSize", listPageSize);
		return json;
	}

	private static boolean bool(JsonObject json, String key, boolean fallback) {
		try {
			return json.has(key) ? json.get(key).getAsBoolean() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static int integer(JsonObject json, String key, int fallback) {
		try {
			return json.has(key) ? json.get(key).getAsInt() : fallback;
		} catch (RuntimeException e) {
			return fallback;
		}
	}
}
