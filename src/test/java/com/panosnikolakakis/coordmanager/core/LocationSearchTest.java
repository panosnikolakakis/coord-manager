package com.panosnikolakakis.coordmanager.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationSearchTest {
	private static final List<String> NAMES = List.of("Base", "Base camp", "Iron farm", "Over there", "Theatre");

	@Test
	void partOfANameFindsIt() {
		assertEquals(new LocationSearch.Result.Found("Iron farm", false), LocationSearch.resolve("farm", NAMES));
		assertEquals(new LocationSearch.Result.Found("Over there", false), LocationSearch.resolve("ere", NAMES));
	}

	@Test
	void theRequestedExample() {
		// "th" matches both "Over there" (word start) and "Theatre" (name start), so the player picks.
		assertEquals(List.of("Theatre", "Over there"), LocationSearch.rank("th", NAMES));
		assertTrue(LocationSearch.resolve("th", NAMES) instanceof LocationSearch.Result.Ambiguous);
		// With only "Over there" saved, "th" goes straight to it.
		assertEquals(new LocationSearch.Result.Found("Over there", false), LocationSearch.resolve("th", List.of("Base", "Over there")));
	}

	@Test
	void exactNameBeatsLongerNamesThatStartTheSame() {
		assertEquals(new LocationSearch.Result.Found("Base", true), LocationSearch.resolve("base", NAMES));
		assertEquals(List.of("Base", "Base camp"), LocationSearch.rank("BASE", NAMES));
	}

	@Test
	void ranksStartThenWordThenAnywhere() {
		assertEquals(List.of("Theatre", "Over there"), LocationSearch.rank("the", NAMES));
		assertEquals(List.of("Base camp"), LocationSearch.rank("amp", NAMES));
	}

	@Test
	void noMatchAndEmptyInput() {
		assertTrue(LocationSearch.resolve("nether", NAMES) instanceof LocationSearch.Result.None);
		assertTrue(LocationSearch.resolve("  ", NAMES) instanceof LocationSearch.Result.None);
		assertEquals(NAMES, LocationSearch.rank("", NAMES));
	}
}
