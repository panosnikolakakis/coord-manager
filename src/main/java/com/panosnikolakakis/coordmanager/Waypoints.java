package com.panosnikolakakis.coordmanager;

import com.panosnikolakakis.coordmanager.core.Location;
import com.panosnikolakakis.coordmanager.core.LocationRepository;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.Waypoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shows saved locations on the vanilla locator bar by sending waypoint packets straight
 * to each player. Nothing is spawned in the world, so there's nothing to duplicate,
 * orphan or clean up, and vanilla clients understand it without installing the mod.
 *
 * <p>This is the only class that touches the waypoint network API; if a Minecraft
 * update changes it, this is the file to fix.
 */
final class Waypoints {
	private final boolean enabled;
	private final LocationRepository repository;
	/** Waypoint ids currently shown to each player, so they can be removed precisely. */
	private final Map<UUID, Set<UUID>> shown = new HashMap<>();

	Waypoints(boolean enabled, LocationRepository repository) {
		this.enabled = enabled;
		this.repository = repository;
	}

	boolean enabled() {
		return enabled;
	}

	/** Clears what this player was shown and sends the locations in their current dimension. */
	void refresh(ServerPlayer player) {
		Set<UUID> previous = shown.remove(player.getUUID());
		if (previous != null) {
			for (UUID id : previous) {
				player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(id));
			}
		}

		if (!enabled || repository.waypointsHidden(player.getUUID())) {
			return;
		}

		String dimension = player.level().dimension().identifier().toString();
		Set<UUID> now = new HashSet<>();
		for (Location location : repository.inDimension(dimension)) {
			UUID id = idFor(location);
			player.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(
					id, new Waypoint.Icon(), new Vec3i(location.x(), location.y(), location.z())));
			now.add(id);
		}
		shown.put(player.getUUID(), now);
	}

	void refreshAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			refresh(player);
		}
	}

	void forget(ServerPlayer player) {
		shown.remove(player.getUUID());
	}

	/**
	 * A stable id per location name. It never collides with a real entity's UUID (those are
	 * random, version 4; this is name-based, version 3) and it stays the same across restarts.
	 * The client also picks the waypoint's colour from it, so each location keeps its colour.
	 */
	static UUID idFor(Location location) {
		return UUID.nameUUIDFromBytes((CoordManager.MOD_ID + ":" + location.key()).getBytes(StandardCharsets.UTF_8));
	}
}
