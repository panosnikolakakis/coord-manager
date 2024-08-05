package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocationSave {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("locationsave")
                        .then(CommandManager.argument("LocationName", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String locationName = StringArgumentType.getString(context, "LocationName");

                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    Vec3d playerPos = player.getPos();
                                    int adjustedXPos = (int) Math.floor(playerPos.x);
                                    int adjustedYPos = (int) Math.ceil(playerPos.y);
                                    int adjustedZPos = (int) Math.floor(playerPos.z);

                                    Identifier dimensionId = context.getSource().getPlayer().getEntityWorld().getRegistryKey().getValue();

                                    saveLocationToFile(player, locationName, adjustedXPos, adjustedYPos, adjustedZPos, dimensionId);

                                    Text message = Text.literal("Location '")
                                            .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                            .append(Text.literal("' saved at ").setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)))
                                            .append(Text.literal(getDimensionName(dimensionId)).setStyle(Style.EMPTY.withFormatting(getDimensionColor(dimensionId))))
                                            .append(Text.literal(" coordinates: ").setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)))
                                            .append(Text.literal(adjustedXPos + ", " + adjustedYPos + ", " + adjustedZPos).setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)));

                                    context.getSource().sendFeedback(() -> message, false);
                                    return 1;
                                })
                        )
        );
    }

    private static synchronized void saveLocationToFile(ServerPlayerEntity player, String locationName, int x, int y, int z, Identifier dimensionId) {
        try {
            Path savePath = getSavePath(player.getServer(), player.getWorld().getServer().getSavePath(WorldSavePath.ROOT));

            if (!Files.exists(savePath)) {
                Files.createDirectories(savePath);
            }

            Path filePath = savePath.resolve("locations.txt");

            List<String> lines = new ArrayList<>();
            if (Files.exists(filePath)) {
                lines = Files.readAllLines(filePath);
            }

            boolean locationExists = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(locationName + ":")) {
                    lines.set(i, locationName + ": " + getDimensionName(dimensionId) + ": " + x + " " + y + " " + z);
                    locationExists = true;
                    break;
                }
            }

            if (!locationExists) {
                lines.add(locationName + ": " + getDimensionName(dimensionId) + ": " + x + " " + y + " " + z);
            }

            Files.write(filePath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static String getDimensionName(Identifier dimensionId) {
        String dimensionName = dimensionId.getPath();

        switch (dimensionName) {
            case "overworld":
                return "Overworld";
            case "the_nether":
                return "Nether";
            case "the_end":
                return "The End";
            default:
                return dimensionName;
        }
    }

    private static Formatting getDimensionColor(Identifier dimensionId) {
        String dimensionName = dimensionId.getPath();

        switch (dimensionName) {
            case "overworld":
                return Formatting.GREEN;
            case "the_nether":
                return Formatting.RED;
            case "the_end":
                return Formatting.DARK_PURPLE;
            default:
                return Formatting.WHITE;
        }
    }
}