package com.panosnikolakakis.coordmanager.core;

import java.util.Locale;

/** Turns dimension ids into names for chat, and understands the names 1.x wrote to locations.txt. */
public final class Dimensions {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	public static final String END = "minecraft:the_end";

	private Dimensions() {
	}

	public static String displayName(String dimensionId) {
		return switch (dimensionId) {
			case OVERWORLD -> "Overworld";
			case NETHER -> "Nether";
			case END -> "The End";
			default -> titleCase(path(dimensionId));
		};
	}

	/**
	 * 1.x stored "Overworld", "Nether", "The End", or the bare path of a modded
	 * dimension (its namespace was lost). Map those back to full ids.
	 */
	public static String fromLegacyName(String legacy) {
		String name = legacy.strip();
		switch (name.toLowerCase(Locale.ROOT)) {
			case "overworld":
				return OVERWORLD;
			case "nether":
			case "the_nether":
				return NETHER;
			case "the end":
			case "the_end":
				return END;
			default:
				if (name.contains(":")) {
					return name.toLowerCase(Locale.ROOT);
				}
				return "minecraft:" + name.toLowerCase(Locale.ROOT).replace(' ', '_');
		}
	}

	private static String path(String id) {
		int colon = id.indexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}

	private static String titleCase(String path) {
		String[] words = path.replace('/', ' ').replace('_', ' ').strip().split("\\s+");
		StringBuilder out = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.isEmpty() ? path : out.toString();
	}
}
