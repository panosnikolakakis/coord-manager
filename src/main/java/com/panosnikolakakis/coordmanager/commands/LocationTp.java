package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationTp {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("locationtp")
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

                                                ServerWorld targetWorld = getWorldByName(player.getServer(), dimension);
                                                ServerWorld playerWorld = player.getServerWorld();

                                                if (targetWorld == null) {
                                                    context.getSource().sendError(Text.literal("Dimension '" + dimension + "' not found."));
                                                    return 0;
                                                }

                                                if (targetWorld != playerWorld) {
                                                    Text message = Text.literal("You must be in the " + dimension + " dimension to teleport to this location.");

                                                    context.getSource().sendFeedback(() -> message, false);
                                                    return 0;
                                                }

                                                player.teleport(targetWorld, x + 0.5, y, z + 0.5, player.getYaw(), player.getPitch());

                                                Text message = Text.literal("Teleported to '")
                                                        .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                        .append("'.");

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
                    String cleanedLine = Formatting.strip(Text.of(line).getString());
                    String[] parts = cleanedLine.split(":", 3);

                    if (parts.length == 3) {
                        String foundLocationName = parts[0].trim();

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
            return Paths.get("config").resolve("coordmanager").resolve("locations.txt");
        } else {
            return worldSavePath.resolve("coordmanager").resolve("locations.txt");
        }
    }

    private static ServerWorld getWorldByName(MinecraftServer server, String dimensionName) {
        switch (dimensionName) {
            case "Overworld":
                return server.getWorld(World.OVERWORLD);
            case "Nether":
                return server.getWorld(World.NETHER);
            case "The End":
                return server.getWorld(World.END);
            default:
                return null;
        }
    }
}
