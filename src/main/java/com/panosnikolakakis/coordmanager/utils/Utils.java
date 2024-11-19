package com.panosnikolakakis.coordmanager.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
    public static Formatting getDimensionColor(String dimensionName) {
        switch (dimensionName) {
            case "Overworld":
                return Formatting.GREEN;
            case "Nether":
                return Formatting.RED;
            case "The End":
                return Formatting.DARK_PURPLE;
            default:
                return Formatting.WHITE;
        }
    }

    public static List<String> getLocationNames(MinecraftServer server, Path worldSavePath) {
        List<String> names = new ArrayList<>();
        try {
            Path filePath = getFilePath(server, worldSavePath);

            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                for (String line : lines) {
                    String[] parts = line.split(":", 2);
                    if (parts.length > 0) {
                        names.add(parts[0].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

    public static Path getFilePath(MinecraftServer server, Path worldSavePath) {
        if (server.isDedicated()) {
            // Server environment, use the config directory
            return Paths.get("config").resolve("coordmanager").resolve("locations.txt");
        } else {
            // Single-player, use the world-specific directory
            return worldSavePath.resolve("coordmanager").resolve("locations.txt");
        }
    }

    public static List<String> getLocationLines(MinecraftServer server, Path worldSavePath, String locationName) {
        try {
            Path filePath = getFilePath(server, worldSavePath);

            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                List<String> foundLines = new ArrayList<>();

                for (String line : lines) {
                    // Remove formatting codes from the line
                    String cleanedLine = Formatting.strip(Text.of(line).getString());

                    // Splitting the cleaned line to get the location name and coordinates
                    String[] parts = cleanedLine.split(":", 3);

                    if (parts.length == 3) {
                        String foundLocationName = parts[0].trim();

                        // Compare the trimmed location names (case-insensitive)
                        if (foundLocationName.equalsIgnoreCase(locationName.trim())) {
                            foundLines.add(line);
                        }
                    }
                }

                return foundLines;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}