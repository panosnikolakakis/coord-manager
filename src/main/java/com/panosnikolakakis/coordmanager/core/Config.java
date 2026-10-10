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
import java.util.OptionalInt;

/**
 * Server settings from {@code config/coordmanager.json}.
 *
 * @param teleportRequiresOp     only operators (permission level 2, same as vanilla /tp) can use /loc tp
 * @param protectOthersLocations players can only overwrite or delete locations they saved themselves; operators can change any
 * @param waypoints              show saved locations on the locator bar
 * @param waypointStyle          locator-bar icon style id. {@code minecraft:bowtie} is built into vanilla and looks
 *                               different from player dots; a resource pack can add others
 * @param waypointColor          hex colour ({@code RRGGBB}, {@code #} optional) for every location's icon, or empty to
 *                               give each location its own colour
 * @param listPageSize           locations per page in /loc list
 */
public record Config(
		boolean teleportRequiresOp,
		boolean protectOthersLocations,
		boolean waypoints,
		String waypointStyle,
		String waypointColor,
		int listPageSize
) {
	public static final String DEFAULT_WAYPOINT_STYLE = "minecraft:bowtie";
	public static final String DEFAULT_WAYPOINT_COLOR = "EFBF04";
	public static final Config DEFAULTS = new Config(true, true, true, DEFAULT_WAYPOINT_STYLE, DEFAULT_WAYPOINT_COLOR, 10);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public Config {
		waypointStyle = waypointStyle == null || waypointStyle.isBlank() ? DEFAULT_WAYPOINT_STYLE : waypointStyle.strip();
		waypointColor = waypointColor == null ? "" : waypointColor.strip();
		listPageSize = Math.max(1, Math.min(listPageSize, 50));
	}

	/** The fixed waypoint colour as 0xRRGGBB, or empty if each location should get its own colour (or the value is invalid). */
	public OptionalInt waypointRgb() {
		String hex = waypointColor.startsWith("#") ? waypointColor.substring(1) : waypointColor;
		if (hex.length() != 6) {
			return OptionalInt.empty();
		}
		try {
			return OptionalInt.of(Integer.parseInt(hex, 16));
		} catch (NumberFormatException e) {
			return OptionalInt.empty();
		}
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
				string(json, "waypointStyle", DEFAULTS.waypointStyle),
				string(json, "waypointColor", DEFAULTS.waypointColor),
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
		json.addProperty("waypointStyle", waypointStyle);
		json.addProperty("waypointColor", waypointColor);
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

	private static String string(JsonObject json, String key, String fallback) {
		try {
			return json.has(key) ? json.get(key).getAsString() : fallback;
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
