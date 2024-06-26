package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationFind {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("locationfind")
                        .then(CommandManager.argument("LocationName", StringArgumentType.greedyString())
                                .suggests(getLocationSuggestions())
                                .executes(context -> {
                                    String locationName = StringArgumentType.getString(context, "LocationName");
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    List<String> lines = getLocationLines(player.getServer(), player.getWorld().getServer().getSavePath(WorldSavePath.ROOT), locationName);

                                    if (!lines.isEmpty()) {
                                        String[] parts = lines.get(0).split(":", 3);
                                        if (parts.length == 3) {
                                            String dimension = parts[1].trim();
                                            String[] coordinates = parts[2].trim().split(" ");
                                            if (coordinates.length == 3) {
                                                int x = Integer.parseInt(coordinates[0]);
                                                int y = Integer.parseInt(coordinates[1]);
                                                int z = Integer.parseInt(coordinates[2]);

                                                Text message = Text.literal("Location '")
                                                        .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                        .append("' details: ")
                                                        .append(Text.literal(dimension).setStyle(Style.EMPTY.withFormatting(getDimensionColor(dimension))))
                                                        .append(": " + x + ", " + y + ", " + z);

                                                context.getSource().sendFeedback(() -> message, false);
                                                return 1;
                                            }
                                        }
                                    }

                                    Text message = Text.literal("Location '")
                                            .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                            .append("' not found.");

                                    context.getSource().sendFeedback(() -> message, false);
                                    return 0;
                                })
                        )
        );
    }

    // Autocomplete suggestions provider
    private static SuggestionProvider<ServerCommandSource> getLocationSuggestions() {
        return (context, builder) -> {
            List<String> suggestions = getLocationNames(context.getSource().getServer(), context.getSource().getWorld().getServer().getSavePath(WorldSavePath.ROOT));
            return CommandSource.suggestMatching(suggestions, builder);
        };
    }

    // Get a list of existing location names
    private static List<String> getLocationNames(MinecraftServer server, Path worldSavePath) {
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

    private static List<String> getLocationLines(MinecraftServer server, Path worldSavePath, String locationName) {
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

    private static Path getFilePath(MinecraftServer server, Path worldSavePath) {
        if (server.isDedicated()) {
            // Server environment, use the config directory
            return Paths.get("config").resolve("coordmanager").resolve("locations.txt");
        } else {
            // Single-player, use the world-specific directory
            return worldSavePath.resolve("coordmanager").resolve("locations.txt");
        }
    }

    private static Formatting getDimensionColor(String dimensionName) {
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
}