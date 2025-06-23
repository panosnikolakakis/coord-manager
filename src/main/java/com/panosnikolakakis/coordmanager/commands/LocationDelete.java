package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.panosnikolakakis.coordmanager.mixin.EntityCommandTagAccessor;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.panosnikolakakis.coordmanager.utils.Utils.getFilePath;
import static com.panosnikolakakis.coordmanager.utils.Utils.getLocationNames;

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
                                        removeWaypointArmorStand(context.getSource().getServer(), locationName);
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

                        String[] parts = line.split(":");
                        if (parts.length == 3) {
                            String dimension = parts[1];
                            String[] coordinates = parts[2].split(" ");
                            if (coordinates.length == 3) {
                                int x = Integer.parseInt(coordinates[0]);
                                int y = Integer.parseInt(coordinates[1]);
                                int z = Integer.parseInt(coordinates[2]);
                            }
                        }

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

    private static void removeWaypointArmorStand(MinecraftServer server, String locationName) {

        for (ServerWorld world : server.getWorlds()) {
            Box searchArea = new Box(new Vec3d(-1000000, -1000000, -1000000), new Vec3d(1000000, 1000000, 1000000));

            List<ArmorStandEntity> allArmorStands = world.getEntitiesByClass(ArmorStandEntity.class, searchArea, e -> true);

            List<ArmorStandEntity> filtered = new ArrayList<>();

            for (ArmorStandEntity entity : allArmorStands) {
                EntityCommandTagAccessor tagAccessor = (EntityCommandTagAccessor) (Object) entity;
                Set<String> tags = tagAccessor.coordmanager$getCommandTags();

                if (tags.contains("coordmanager_waypoint") && locationName.equals(entity.getName().getString())) {
                    filtered.add(entity);
                }
            }

            for (ArmorStandEntity armorStand : filtered) {
                armorStand.discard();
            }
        }
    }

    private static SuggestionProvider<ServerCommandSource> getLocationSuggestions() {
        return (context, builder) -> {
            List<String> suggestions = getLocationNames(context.getSource().getServer(), context.getSource().getWorld().getServer().getSavePath(WorldSavePath.ROOT));
            return CommandSource.suggestMatching(suggestions, builder);
        };
    }
}