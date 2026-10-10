package com.panosnikolakakis.coordmanager.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyImporterTest {
	@Test
	void readsTheThreeVanillaDimensions() {
		var result = LegacyImporter.parse(List.of(
				"Base: Overworld: 10 64 -20",
				"Fortress: Nether: 73 102 73",
				"Portal: The End: 100 49 0"
		));

		assertEquals(3, result.locations().size());
		assertEquals(Dimensions.OVERWORLD, result.locations().get(0).dimension());
		assertEquals(Dimensions.NETHER, result.locations().get(1).dimension());
		assertEquals(Dimensions.END, result.locations().get(2).dimension());
		assertEquals(-20, result.locations().get(0).z());
		assertNull(result.locations().get(0).creatorId());
	}

	@Test
	void namesWithColonsAndSpacesSurvive() {
		var result = LegacyImporter.parse(List.of("My base: east side: Overworld: 1 2 3"));
		assertEquals("My base: east side", result.locations().get(0).name());
	}

	@Test
	void moddedDimensionsGetANamespace() {
		var result = LegacyImporter.parse(List.of("Cave: deep_dark: 1 2 3"));
		assertEquals("minecraft:deep_dark", result.locations().get(0).dimension());
	}

	@Test
	void badLinesAreSkippedNotFatal() {
		var result = LegacyImporter.parse(List.of("garbage", "Base: Overworld: 1 two 3", "", "Ok: Overworld: 1 2 3"));
		assertEquals(1, result.locations().size());
		assertEquals(2, result.skippedLines().size());
	}

	@Test
	void caseClashesKeepTheLaterLine() {
		var result = LegacyImporter.parse(List.of("Base: Overworld: 1 1 1", "base: Nether: 2 2 2"));
		assertEquals(1, result.locations().size());
		assertEquals("base", result.locations().get(0).name());
		assertEquals(2, result.locations().get(0).x());
	}
}
