package com.panosnikolakakis.coordmanager;

import com.panosnikolakakis.coordmanager.commands.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordManager implements ModInitializer {
	public static final String MOD_ID = "coordmanager";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loaded.");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LocationSave.register(dispatcher);
			LocationList.register(dispatcher);
			LocationDelete.register(dispatcher);
			LocationFind.register(dispatcher);
			LocationTp.register(dispatcher);
		});
	}
}