package com.panosnikolakakis.coordmanager.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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

                                    Identifier dimensionId = context.getSource().getPlayer().getWorld().getRegistryKey().getValue();

                                    saveLocationToFile(player, locationName, adjustedXPos, adjustedYPos, adjustedZPos, dimensionId);

                                    Text message = Text.literal("Location '")
                                            .append(Text.literal(locationName).setStyle(Style.EMPTY.withFormatting(Formatting.GOLD)))
                                            .append(Text.literal("' saved at ").setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)))
                                            .append(Text.literal(getDimensionName(dimensionId)).setStyle(Style.EMPTY.withFormatting(getDimensionColor(dimensionId))))
                                            .append(Text.literal(" coordinates: ").setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)))
                                            .append(Text.literal(adjustedXPos + ", " + adjustedYPos + ", " + adjustedZPos).setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)));

                                    context.getSource().sendFeedback(() -> message, false);
                                    makeWaypoint(player, locationName, new BlockPos(adjustedXPos, adjustedYPos, adjustedZPos), dimensionId);

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

    private static void makeWaypoint(ServerPlayerEntity player, String name, BlockPos pos, Identifier dimensionId) {
        ServerWorld world = player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (world == null) return;

        ArmorStandEntity marker = new ArmorStandEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        marker.setCustomName(Text.literal(name));
        marker.setCustomNameVisible(false);
        marker.addCommandTag("coordmanager_waypoint");
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setSilent(true);

        world.spawnEntity(marker);
        EntityAttributeInstance markerAttr = marker.getAttributeInstance(EntityAttributes.WAYPOINT_TRANSMIT_RANGE);
        markerAttr.setBaseValue(300);
    }

    public static void syncWaypoints(ServerPlayerEntity player, Identifier targetDimension) {
        Path filePath = getSavePath(player.getServer(), player.getWorld().getServer().getSavePath(WorldSavePath.ROOT)).resolve("locations.txt");

        if (!Files.exists(filePath)) return;

        try {
            List<String> lines = Files.readAllLines(filePath);

            for (String line : lines) {
                // Format: name: DimensionName: x y z
                String[] parts = line.split(":");
                if (parts.length != 3) continue;

                String name = parts[0].trim();
                String dimensionName = parts[1].trim().toLowerCase();
                String[] coords = parts[2].trim().split(" ");
                if (coords.length != 3) continue;

                // Convert dimensionName to Identifier
                Identifier dimId;
                switch (dimensionName) {
                    case "nether" -> dimId = Identifier.of("minecraft", "the_nether");
                    case "the end" -> dimId = Identifier.of("minecraft", "the_end");
                    case "overworld" -> dimId = Identifier.of("minecraft", "overworld");
                    default -> {
                        System.err.println("[CoordManager] Unknown dimension in line: " + line);
                        continue;
                    }
                }

                // Only continue if this line’s dimension matches the targetDimension
                if (!dimId.equals(targetDimension)) continue;

                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                BlockPos pos = new BlockPos(x, y, z);

                ServerWorld world = player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
                if (world == null) {
                    System.err.println("[CoordManager] Could not get world for dimension ID: " + dimId);
                    continue;
                }

                Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                double radius = 1.0;

                Box searchBox = new Box(
                        center.x - radius, center.y - radius, center.z - radius,
                        center.x + radius, center.y + radius, center.z + radius
                );

                List<ArmorStandEntity> nearbyStands = world.getEntitiesByClass(
                        ArmorStandEntity.class,
                        searchBox,
                        e -> true
                );

                boolean exists = nearbyStands.stream().anyMatch(e ->
                        e.getCommandTags().contains("coordmanager_waypoint") &&
                                e.getCustomName() != null &&
                                e.getCustomName().getString().trim().equals(name.trim())
                );

                if (!exists) {
                    System.out.println("[CoordManager] Spawning missing waypoint '" + name + "' at " + pos + " in " + dimId);
                    makeWaypoint(player, name, pos, dimId);
                }
            }

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }


}