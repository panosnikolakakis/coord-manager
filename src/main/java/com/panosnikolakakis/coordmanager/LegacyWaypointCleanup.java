package com.panosnikolakakis.coordmanager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Coord Manager 1.2 marked waypoints with invisible armor stands, and a bug spawned
 * duplicates on every join. This stops any of those from loading, which removes them
 * from the world as each chunk is visited. It can be deleted in a few versions.
 */
final class LegacyWaypointCleanup {
	static final String LEGACY_TAG = "coordmanager_waypoint";

	private LegacyWaypointCleanup() {
	}

	static boolean allowLoad(Entity entity, ServerLevel level, EntitySpawnReason spawnReason, boolean loadedFromDisk) {
		if (loadedFromDisk && entity.entityTags().contains(LEGACY_TAG)) {
			CoordManager.LOGGER.info("Removed an old Coord Manager 1.x waypoint marker ('{}') at {} in {}.",
					entity.getName().getString(), entity.blockPosition().toShortString(), level.dimension().identifier());
			return false;
		}
		return true;
	}
}
