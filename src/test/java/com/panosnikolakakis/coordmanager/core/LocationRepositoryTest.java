package com.panosnikolakakis.coordmanager.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationRepositoryTest {
	@TempDir
	Path dir;

	private static Location loc(String name, int x) {
		return new Location(name, Dimensions.OVERWORLD, x, 64, 0, UUID.randomUUID(), "Steve", Instant.parse("2026-10-10T08:00:00Z"));
	}

	@Test
	void savesSurviveAReopen() throws IOException {
		Path data = dir.resolve("coordmanager/locations.json");
		var repo = LocationRepository.open(data, List.of(), msg -> { });
		repo.save(loc("Base", 1));
		repo.save(loc("Farm", 2));
		repo.delete("farm");

		var reopened = LocationRepository.open(data, List.of(), msg -> { });
		assertEquals(List.of("Base"), reopened.names());
		assertEquals(loc("Base", 1).creatorName(), reopened.get("BASE").orElseThrow().creatorName());
	}

	@Test
	void namesAreCaseInsensitiveAndOverwriteReturnsThePrevious() throws IOException {
		var repo = LocationRepository.open(dir.resolve("l.json"), List.of(), msg -> { });
		repo.save(loc("Base", 1));
		var previous = repo.save(loc("BASE", 2));

		assertEquals(1, previous.orElseThrow().x());
		assertEquals(1, repo.size());
		assertEquals("BASE", repo.get("base").orElseThrow().name());
	}

	@Test
	void importsLegacyFilesOnceAndRenamesThem() throws IOException {
		Path legacy = dir.resolve("old/locations.txt");
		Files.createDirectories(legacy.getParent());
		Files.writeString(legacy, "Base: Overworld: 1 2 3\nbroken line\n");
		List<String> log = new ArrayList<>();

		Path data = dir.resolve("coordmanager/locations.json");
		var repo = LocationRepository.open(data, List.of(legacy), log::add);

		assertEquals(List.of("Base"), repo.names());
		assertTrue(Files.exists(data));
		assertFalse(Files.exists(legacy));
		assertTrue(Files.exists(legacy.resolveSibling("locations.txt.migrated")));
		assertTrue(log.stream().anyMatch(m -> m.contains("broken line")));

		// A second start doesn't import again, even if a locations.txt reappears.
		Files.writeString(legacy, "Other: Overworld: 1 2 3\n");
		var again = LocationRepository.open(data, List.of(legacy), msg -> { });
		assertEquals(List.of("Base"), again.names());
	}

	@Test
	void corruptFilesAreBackedUpNotOverwritten() throws IOException {
		Path data = dir.resolve("locations.json");
		Files.writeString(data, "{ this is not json");
		List<String> log = new ArrayList<>();

		var repo = LocationRepository.open(data, List.of(), log::add);

		assertEquals(0, repo.size());
		assertEquals(1, log.size());
		try (var files = Files.list(dir)) {
			assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("locations.json.corrupt-")));
		}
	}

	@Test
	void refusesFilesFromANewerVersion() throws IOException {
		Path data = dir.resolve("locations.json");
		Files.writeString(data, "{\"formatVersion\": 99, \"locations\": []}");
		assertThrows(LocationFile.CorruptFileException.class, () -> new LocationFile(data).load());
	}

	@Test
	void failedWritesAreRolledBack() throws IOException {
		Path data = dir.resolve("sub/locations.json");
		var repo = LocationRepository.open(data, List.of(), msg -> { });
		repo.save(loc("Base", 1));

		// Make the next write fail by putting a directory where the temp file goes.
		Files.createDirectories(dir.resolve("sub/locations.json.tmp/blocker"));
		assertThrows(IOException.class, () -> repo.save(loc("Base", 99)));
		assertEquals(1, repo.get("Base").orElseThrow().x());
		assertThrows(IOException.class, () -> repo.delete("Base"));
		assertTrue(repo.get("Base").isPresent());
	}

	@Test
	void waypointPreferencePersists() throws IOException {
		Path data = dir.resolve("locations.json");
		UUID player = UUID.randomUUID();
		LocationRepository.open(data, List.of(), msg -> { }).setWaypointsHidden(player, true);

		assertTrue(LocationRepository.open(data, List.of(), msg -> { }).waypointsHidden(player));
	}
}
