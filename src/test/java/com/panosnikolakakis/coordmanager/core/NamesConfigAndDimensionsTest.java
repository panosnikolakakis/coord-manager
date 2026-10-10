package com.panosnikolakakis.coordmanager.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamesConfigAndDimensionsTest {
	@TempDir
	Path dir;

	@Test
	void namesAreCleanedAndValidated() {
		assertEquals("my base", LocationNames.clean("  my \t  base "));
		assertEquals(LocationNames.key("Base"), LocationNames.key(" BASE "));
		assertTrue(LocationNames.validate("").isPresent());
		assertTrue(LocationNames.validate("x".repeat(LocationNames.MAX_LENGTH + 1)).isPresent());
		assertTrue(LocationNames.validate("bad§ccolor").isPresent());
		assertFalse(LocationNames.validate("Base: east (old)").isPresent());
	}

	@Test
	void dimensionNames() {
		assertEquals("Overworld", Dimensions.displayName(Dimensions.OVERWORLD));
		assertEquals("The End", Dimensions.displayName(Dimensions.END));
		assertEquals("Deep Dark", Dimensions.displayName("somemod:deep_dark"));
		assertEquals(Dimensions.NETHER, Dimensions.fromLegacyName("Nether"));
	}

	@Test
	void configIsCreatedWithDefaults() throws IOException {
		Path file = dir.resolve("config/coordmanager.json");
		assertEquals(Config.DEFAULTS, Config.loadOrCreate(file));
		assertTrue(Files.exists(file));
	}

	@Test
	void missingConfigKeysAreFilledInAndKeptValuesStay() throws IOException {
		Path file = dir.resolve("coordmanager.json");
		Files.writeString(file, "{\"teleportRequiresOp\": false}");

		Config config = Config.loadOrCreate(file);

		assertFalse(config.teleportRequiresOp());
		assertTrue(config.waypoints());
		assertTrue(Files.readString(file).contains("listPageSize"));
		assertEquals(Config.DEFAULT_WAYPOINT_STYLE, config.waypointStyle());
	}

	@Test
	void waypointColourParsing() {
		assertEquals(0xEFBF04, Config.DEFAULTS.waypointRgb().getAsInt());
		assertTrue(new Config(true, true, true, "", "", 10).waypointRgb().isEmpty());
		assertEquals(0xFFAA00, new Config(true, true, true, "", "#FFAA00", 10).waypointRgb().getAsInt());
		assertEquals(0x00FF00, new Config(true, true, true, "", "00ff00", 10).waypointRgb().getAsInt());
		assertTrue(new Config(true, true, true, "", "orange", 10).waypointRgb().isEmpty());
		assertEquals(Config.DEFAULT_WAYPOINT_STYLE, new Config(true, true, true, " ", "", 10).waypointStyle());
	}
}
