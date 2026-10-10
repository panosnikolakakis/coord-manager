package com.panosnikolakakis.coordmanager.core;

import java.util.Locale;
import java.util.Optional;

/** Rules for location names, in one place so every command treats names the same way. */
public final class LocationNames {
	public static final int MAX_LENGTH = 48;

	private LocationNames() {
	}

	/** Trims and collapses runs of whitespace, so {@code "  my   base "} becomes {@code "my base"}. */
	public static String clean(String raw) {
		return raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
	}

	/** Names are unique ignoring case: {@code Base} and {@code base} are the same location. */
	public static String key(String name) {
		return clean(name).toLowerCase(Locale.ROOT);
	}

	/** Returns an error message if the (cleaned) name can't be used, or empty if it's fine. */
	public static Optional<String> validate(String cleaned) {
		if (cleaned.isEmpty()) {
			return Optional.of("Location name can't be empty.");
		}
		if (cleaned.length() > MAX_LENGTH) {
			return Optional.of("Location name is too long (max " + MAX_LENGTH + " characters).");
		}
		for (int i = 0; i < cleaned.length(); i++) {
			char c = cleaned.charAt(i);
			if (Character.isISOControl(c) || c == '§') {
				return Optional.of("Location name contains a character that isn't allowed.");
			}
		}
		return Optional.empty();
	}
}
