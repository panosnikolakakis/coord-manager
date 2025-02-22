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
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.*;

import static com.panosnikolakakis.coordmanager.utils.Utils.*;

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

                                                Identifier dimensionId = context.getSource().getPlayer().getEntityWorld().getRegistryKey().getValue();
                                                String currentDimension = getDimensionName(dimensionId);

                                                ServerWorld world = getWorldByName(player, dimension);

                                                if (world != null) {
                                                    if (dimension.equalsIgnoreCase(currentDimension)) {
                                                        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), 0.0F, 0.0F, false);
                                                        Text message = Text.literal("Teleported to '")
                                                                .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                                .append("'.");
                                                        context.getSource().sendFeedback(() -> message, false);
                                                        return 1;
                                                    } else {
                                                        Text message = Text.literal("Location '")
                                                                .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                                                .append("' is not in the same dimension as the player.");
                                                        context.getSource().sendFeedback(() -> message, false);
                                                        return 0;
                                                    }
                                                } else {
                                                    Text message = Text.literal("Could not find the world for dimension '")
                                                            .append(Text.literal(dimension).setStyle(Style.EMPTY.withFormatting(Formatting.RED)))
                                                            .append("'.");
                                                    context.getSource().sendFeedback(() -> message, false);
                                                    return 0;
                                                }
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

    private static String getDimensionName(Identifier dimensionId) {
        String dimensionName = dimensionId.toString();

        switch (dimensionName) {
            case "minecraft:overworld":
                return "Overworld";
            case "minecraft:the_nether":
                return "Nether";
            case "minecraft:the_end":
                return "The End";
            default:
                return dimensionName; // For any other custom dimensions
        }
    }

    private static ServerWorld getWorldByName(ServerPlayerEntity player, String dimensionName) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }

        switch (dimensionName.toLowerCase()) {
            case "overworld":
                return server.getWorld(World.OVERWORLD);
            case "nether":
            case "the_nether":
                return server.getWorld(World.NETHER);
            case "the end":
            case "the_end":
                return server.getWorld(World.END);
            default:
                return null;
        }
    }
}