package com.panosnikolakakis.coordmanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Finds locations from part of a name, so {@code th} finds {@code over there}.
 *
 * <p>Matches are ranked: the exact name first, then names that start with the text,
 * then names with a word that starts with it, then names that contain it anywhere.
 * Matching ignores case and extra spaces.
 */
public final class LocationSearch {
	private LocationSearch() {
	}

	/** What a typed name refers to. */
	public sealed interface Result {
		/** Exactly one location: either the name typed in full, or the only partial match. */
		record Found(String name, boolean exact) implements Result {
		}

		/** Several partial matches; the player has to pick one. */
		record Ambiguous(List<String> names) implements Result {
		}

		record None() implements Result {
		}
	}

	/** Every name that matches, best match first. Names in the same tier keep their original (alphabetical) order. */
	public static List<String> rank(String query, Collection<String> names) {
		String q = LocationNames.key(query);
		List<List<String>> tiers = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

		for (String name : names) {
			String n = LocationNames.key(name);
			if (q.isEmpty()) {
				tiers.get(1).add(name);
			} else if (n.equals(q)) {
				tiers.get(0).add(name);
			} else if (n.startsWith(q)) {
				tiers.get(1).add(name);
			} else if (hasWordStartingWith(n, q)) {
				tiers.get(2).add(name);
			} else if (n.contains(q)) {
				tiers.get(3).add(name);
			}
		}

		List<String> ranked = new ArrayList<>();
		tiers.forEach(ranked::addAll);
		return ranked;
	}

	/**
	 * Works out which location a typed name means. An exact name always wins; otherwise a
	 * partial match is only accepted if it's the only one.
	 */
	public static Result resolve(String query, Collection<String> names) {
		String q = LocationNames.key(query);
		if (q.isEmpty()) {
			return new Result.None();
		}

		List<String> matches = rank(query, names);
		if (matches.isEmpty()) {
			return new Result.None();
		}
		String best = matches.get(0);
		if (LocationNames.key(best).equals(q)) {
			return new Result.Found(best, true);
		}
		if (matches.size() == 1) {
			return new Result.Found(best, false);
		}
		return new Result.Ambiguous(matches);
	}

	private static boolean hasWordStartingWith(String name, String query) {
		for (String word : name.split("[\\s_\\-:/.,]+")) {
			if (word.toLowerCase(Locale.ROOT).startsWith(query)) {
				return true;
			}
		}
		return false;
	}
}
