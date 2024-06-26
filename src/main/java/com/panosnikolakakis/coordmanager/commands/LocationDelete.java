package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocationDelete {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("locationdelete")
                        .then(CommandManager.argument("LocationName", StringArgumentType.greedyString())
                                .suggests(getLocationSuggestions())
                                .executes(context -> {
                                    String locationName = StringArgumentType.getString(context, "LocationName");

                                    if (deleteLocationFromFile(context.getSource(), locationName)) {
                                        Text message = Text.literal("Location '")
                                                .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                .append("' deleted.");

                                        context.getSource().sendFeedback(() -> message, false);
                                        return 1;
                                    } else {
                                        Text message = Text.literal("Location '")
                                                .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                .append("' not found.");

                                        context.getSource().sendFeedback(() -> message, false);
                                        return 0;
                                    }
                                })
                        )
        );
    }

    private static boolean deleteLocationFromFile(ServerCommandSource source, String locationName) {
        try {
            Path filePath = getFilePath(source.getServer(), source.getWorld().getServer().getSavePath(WorldSavePath.ROOT));

            if (Files.exists(filePath)) {
                // Read all lines from the file
                List<String> lines = Files.readAllLines(filePath);

                // Check if the location exists and remove it
                boolean locationFound = false;
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(locationName + ":")) {
                        lines.remove(i);
                        locationFound = true;
                        break;
                    }
                }

                // Write the updated content back to the file if the location was found
                if (locationFound) {
                    Files.write(filePath, lines);
                }

                return locationFound; // Return whether the location was found and deleted
            } else {
                return false; // File does not exist
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static SuggestionProvider<ServerCommandSource> getLocationSuggestions() {
        return (context, builder) -> {
            List<String> suggestions = getLocationNames(context.getSource().getServer(), context.getSource().getWorld().getServer().getSavePath(WorldSavePath.ROOT));
            return CommandSource.suggestMatching(suggestions, builder);
        };
    }

    private static List<String> getLocationNames(MinecraftServer server, Path worldSavePath) {
        List<String> names = new ArrayList<>();
        try {
            Path filePath = getFilePath(server, worldSavePath);

            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                for (String line : lines) {
                    String[] parts = line.split(":");
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

    private static Path getFilePath(MinecraftServer server, Path worldSavePath) {
        if (server.isDedicated()) {
            // Server environment, use the config directory
            return Paths.get("config").resolve("coordmanager").resolve("locations.txt");
        } else {
            // Single-player, use the world-specific directory
            return worldSavePath.resolve("coordmanager").resolve("locations.txt");
        }
    }
}