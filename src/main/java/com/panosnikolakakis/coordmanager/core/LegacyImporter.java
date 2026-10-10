package com.panosnikolakakis.coordmanager.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code locations.txt} files written by Coord Manager 1.x.
 *
 * <p>Each line looked like {@code Name: Dimension: x y z}. Names could contain
 * colons (which broke 1.x), so this parser splits on the <em>last</em> two colons.
 */
public final class LegacyImporter {
	private LegacyImporter() {
	}

	public record Result(List<Location> locations, List<String> skippedLines) {
	}

	public static Result read(Path file) throws IOException {
		return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
	}

	public static Result parse(List<String> lines) {
		// Later lines win on a case-insensitive clash, like a later /locationsave would have.
		Map<String, Location> byKey = new LinkedHashMap<>();
		List<String> skipped = new ArrayList<>();

		for (String line : lines) {
			if (line.isBlank()) {
				continue;
			}
			Location location = parseLine(line);
			if (location == null) {
				skipped.add(line);
			} else {
				byKey.put(location.key(), location);
			}
		}
		return new Result(List.copyOf(byKey.values()), List.copyOf(skipped));
	}

	static Location parseLine(String line) {
		int coordsColon = line.lastIndexOf(':');
		if (coordsColon <= 0) {
			return null;
		}
		int dimensionColon = line.lastIndexOf(':', coordsColon - 1);
		if (dimensionColon <= 0) {
			return null;
		}

		String name = LocationNames.clean(line.substring(0, dimensionColon));
		String dimension = line.substring(dimensionColon + 1, coordsColon).strip();
		String[] coords = line.substring(coordsColon + 1).strip().split("\\s+");

		if (LocationNames.validate(name).isPresent() || dimension.isEmpty() || coords.length != 3) {
			return null;
		}
		try {
			int x = Integer.parseInt(coords[0]);
			int y = Integer.parseInt(coords[1]);
			int z = Integer.parseInt(coords[2]);
			return new Location(name, Dimensions.fromLegacyName(dimension), x, y, z, null, null, null);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
