package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import java.util.List;

import static com.panosnikolakakis.coordmanager.utils.Utils.getDimensionColor;

public class LocationList {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("locationlist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            List<String> lines = getAllLocationLines(player.getServer(), player.getWorld().getServer().getSavePath(WorldSavePath.ROOT));

                            if (!lines.isEmpty()) {
                                Text message = Text.literal("Saved locations:");
                                context.getSource().sendFeedback(() -> message, false);

                                for (String line : lines) {
                                    String[] parts = line.split(":", 3);
                                    if (parts.length == 3) {
                                        String locationName = parts[0].trim();
                                        String dimension = parts[1].trim();
                                        String coordinates = parts[2].trim().replace(" ", ", ");



                                        Text locationMessage = Text.literal("- ")
                                                .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                .append(": ")
                                                .append(Text.literal(dimension).setStyle(Style.EMPTY.withFormatting(getDimensionColor(dimension))))
                                                .append(": " + coordinates);

                                        context.getSource().sendFeedback(() -> locationMessage, false);
                                    }
                                }
                            } else {
                                Text message = Text.literal("No locations saved yet.");
                                context.getSource().sendFeedback(() -> message, false);
                            }

                            return 1;
                        })
        );
    }

    private static List<String> getAllLocationLines(MinecraftServer server, Path worldSavePath) {
        try {
            Path savePath = getSavePath(server, worldSavePath);
            Path filePath = savePath.resolve("locations.txt");

            if (Files.exists(filePath)) {
                return Files.readAllLines(filePath);
            } else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static Path getSavePath(MinecraftServer server, Path worldSavePath) {
        if (server.isDedicated()) {
            // Server
            return Paths.get("config").resolve("coordmanager");
        } else {
            // Singleplayer
            return worldSavePath.resolve("coordmanager");
        }
    }
}