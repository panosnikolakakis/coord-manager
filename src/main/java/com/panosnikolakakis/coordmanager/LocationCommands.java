package com.panosnikolakakis.coordmanager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panosnikolakakis.coordmanager.core.Config;
import com.panosnikolakakis.coordmanager.core.Location;
import com.panosnikolakakis.coordmanager.core.LocationNames;
import com.panosnikolakakis.coordmanager.core.LocationSearch;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * All commands live under {@code /loc}.
 */
final class LocationCommands {
	private static final String NAME = "name";
	private static final int MAX_CHOICES = 8;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

	private LocationCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher, Config config) {
		Predicate<CommandSourceStack> canTeleport = source -> canTeleport(source, config);

		dispatcher.register(Commands.literal("loc")
				.then(Commands.literal("save").then(newName().executes(LocationCommands::save)))
				.then(Commands.literal("list")
						.executes(context -> list(context, 1))
						.then(Commands.argument("page", IntegerArgumentType.integer(1))
								.executes(context -> list(context, IntegerArgumentType.getInteger(context, "page")))))
				.then(Commands.literal("find").then(existingName().executes(LocationCommands::find)))
				.then(Commands.literal("tp").requires(canTeleport).then(existingName().executes(LocationCommands::teleport)))
				.then(Commands.literal("delete").then(existingName().executes(LocationCommands::delete)))
				.then(Commands.literal("waypoints")
						.then(Commands.literal("on").executes(context -> setWaypoints(context, true)))
						.then(Commands.literal("off").executes(context -> setWaypoints(context, false)))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> newName() {
		return Commands.argument(NAME, StringArgumentType.greedyString());
	}

	/** A location name argument whose tab completion matches any part of a name, not just the start. */
	private static RequiredArgumentBuilder<CommandSourceStack, String> existingName() {
		return Commands.argument(NAME, StringArgumentType.greedyString())
				.suggests((context, builder) -> {
					LocationService service = CoordManager.service();
					if (service != null) {
						// Vanilla's helper only offers names that start with the typed text, so suggest directly.
						LocationSearch.rank(builder.getRemaining(), service.names()).forEach(builder::suggest);
					}
					return builder.buildFuture();
				});
	}

	// ---- commands ----

	private static int save(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		LocationService service = requireService(source);
		if (service == null) return 0;

		String name = LocationNames.clean(StringArgumentType.getString(context, NAME));
		Optional<String> invalid = LocationNames.validate(name);
		if (invalid.isPresent()) {
			source.sendFailure(Component.literal(invalid.get()));
			return 0;
		}

		Optional<Location> existing = service.get(name);
		if (existing.isPresent() && !canModify(source, existing.get())) {
			source.sendFailure(notYours(existing.get()));
			return 0;
		}

		// Round y up so standing on a slab or path block saves the block you're standing in, not inside the one below.
		int x = (int) Math.floor(player.getX());
		int y = (int) Math.ceil(player.getY() - 1.0E-3);
		int z = (int) Math.floor(player.getZ());
		String dimension = player.level().dimension().identifier().toString();
		Location location = new Location(name, dimension, x, y, z,
				player.getUUID(), player.getGameProfile().name(), Instant.now());

		try {
			service.save(location);
		} catch (IOException e) {
			return storageFailure(source, e);
		}

		source.sendSuccess(() -> Chat.text(existing.isPresent() ? "Updated " : "Saved ")
				.append(Chat.clickableName(location))
				.append(" at ")
				.append(Chat.dimension(dimension))
				.append(" ")
				.append(Chat.coordinates(location)), false);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> context, int page) {
		CommandSourceStack source = context.getSource();
		LocationService service = requireService(source);
		if (service == null) return 0;

		List<Location> all = service.all();
		if (all.isEmpty()) {
			source.sendSuccess(() -> Chat.text("No locations saved yet. Use ")
					.append(Chat.suggestButton("/loc save <name>", ChatFormatting.YELLOW, "/loc save ", "Save where you're standing")), false);
			return 1;
		}

		int pageSize = CoordManager.config().listPageSize();
		int pages = (all.size() + pageSize - 1) / pageSize;
		if (page > pages) {
			source.sendFailure(Component.literal("There " + (pages == 1 ? "is only 1 page." : "are only " + pages + " pages.")));
			return 0;
		}

		ServerPlayer viewer = source.getPlayer();
		boolean showTeleport = canTeleport(source, CoordManager.config());

		MutableComponent message = Chat.text("Saved locations (" + all.size() + ")")
				.append(pages > 1 ? Chat.gray(" · page " + page + "/" + pages) : Component.empty());

		for (Location location : all.subList((page - 1) * pageSize, Math.min(page * pageSize, all.size()))) {
			MutableComponent line = Chat.text("\n • ")
					.append(Chat.clickableName(location))
					.append(Chat.gray("  "))
					.append(Chat.dimension(location.dimension()))
					.append(Chat.gray("  "))
					.append(Chat.coordinates(location));
			distanceFrom(viewer, location).ifPresent(distance -> line.append(Chat.gray("  " + distance)));
			if (showTeleport) {
				line.append(" ").append(Chat.runButton("TP", ChatFormatting.GREEN, "/loc tp " + location.name(), "Teleport to " + location.name()));
			}
			message.append(line);
		}

		if (pages > 1) {
			message.append("\n");
			if (page > 1) {
				message.append(Chat.runButton("« Previous", ChatFormatting.YELLOW, "/loc list " + (page - 1), "Page " + (page - 1))).append(" ");
			}
			if (page < pages) {
				message.append(Chat.runButton("Next »", ChatFormatting.YELLOW, "/loc list " + (page + 1), "Page " + (page + 1)));
			}
		}

		source.sendSuccess(() -> message, false);
		return all.size();
	}

	private static int find(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Optional<Location> found = lookup(context, "find", true);
		if (found.isEmpty()) return 0;
		Location location = found.get();

		MutableComponent message = Chat.clickableName(location)
				.append(Chat.gray(" · "))
				.append(Chat.dimension(location.dimension()))
				.append(Chat.gray(" · "))
				.append(Chat.coordinates(location));
		distanceFrom(source.getPlayer(), location).ifPresent(distance -> message.append(Chat.gray(" (" + distance + " away)")));

		if (location.creatorName() != null) {
			message.append(Chat.gray("\nSaved by " + location.creatorName()
					+ (location.createdAt() != null ? " on " + DATE.format(location.createdAt()) : "")));
		}

		message.append("\n");
		if (canTeleport(source, CoordManager.config())) {
			message.append(Chat.runButton("TP", ChatFormatting.GREEN, "/loc tp " + location.name(), "Teleport there")).append(" ");
		}
		message.append(Chat.copyButton(location));
		if (canModify(source, location)) {
			message.append(" ").append(Chat.suggestButton("Delete", ChatFormatting.RED, "/loc delete " + location.name(), "Puts the delete command in chat for you to confirm"));
		}

		source.sendSuccess(() -> message, false);
		return 1;
	}

	private static int teleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		Optional<Location> found = lookup(context, "tp", true);
		if (found.isEmpty()) return 0;
		Location location = found.get();

		ServerLevel level = CoordManager.service().level(location.dimension());
		if (level == null) {
			source.sendFailure(Component.literal("This server doesn't have the dimension '" + location.dimension() + "'."));
			return 0;
		}

		player.teleportTo(level, location.x() + 0.5, location.y(), location.z() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
		// Logged to operators like vanilla /tp, since it's a privileged action.
		source.sendSuccess(() -> Chat.text("Teleported to ").append(Chat.name(location.name())), true);
		return 1;
	}

	private static int delete(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Optional<Location> found = lookup(context, "delete", false);
		if (found.isEmpty()) return 0;
		Location location = found.get();

		if (!canModify(source, location)) {
			source.sendFailure(notYours(location));
			return 0;
		}

		try {
			CoordManager.service().delete(location.name());
		} catch (IOException e) {
			return storageFailure(source, e);
		}
		source.sendSuccess(() -> Chat.text("Deleted ").append(Chat.name(location.name())), false);
		return 1;
	}

	private static int setWaypoints(CommandContext<CommandSourceStack> context, boolean on) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		LocationService service = requireService(source);
		if (service == null) return 0;

		if (!service.waypoints().enabled()) {
			source.sendFailure(Component.literal("Waypoints are turned off on this server."));
			return 0;
		}

		try {
			service.setWaypointsHidden(player.getUUID(), !on);
		} catch (IOException e) {
			return storageFailure(source, e);
		}
		service.waypoints().refresh(player);
		source.sendSuccess(() -> Chat.text(on ? "Saved locations will show on your locator bar." : "Saved locations are hidden from your locator bar."), false);
		return 1;
	}

	// ---- helpers ----

	/**
	 * Finds the location a command refers to. With {@code allowPartial}, part of a name is enough
	 * if only one location matches ({@code /loc find th} finds "over there"). Otherwise, and when
	 * several match, the player gets clickable choices instead of a guess.
	 */
	private static Optional<Location> lookup(CommandContext<CommandSourceStack> context, String action, boolean allowPartial) {
		CommandSourceStack source = context.getSource();
		LocationService service = requireService(source);
		if (service == null) return Optional.empty();

		String typed = LocationNames.clean(StringArgumentType.getString(context, NAME));
		LocationSearch.Result result = LocationSearch.resolve(typed, service.names());

		if (result instanceof LocationSearch.Result.Found found && (found.exact() || allowPartial)) {
			return service.get(found.name());
		}

		List<String> candidates = switch (result) {
			case LocationSearch.Result.Found match -> List.of(match.name());
			case LocationSearch.Result.Ambiguous matches -> matches.names();
			case LocationSearch.Result.None nothing -> List.of();
		};

		if (candidates.isEmpty()) {
			source.sendFailure(Component.literal("No location named '" + typed + "'. ")
					.append(Chat.runButton("See all", ChatFormatting.YELLOW, "/loc list", "List saved locations")));
			return Optional.empty();
		}

		MutableComponent message = Component.literal(candidates.size() == 1
				? "No location named '" + typed + "'. Did you mean "
				: "Several locations match '" + typed + "': ");
		int shown = Math.min(candidates.size(), MAX_CHOICES);
		for (int i = 0; i < shown; i++) {
			String name = candidates.get(i);
			String command = "/loc " + action + " " + name;
			// Deleting is never one click away: the button only fills in the command.
			message.append(allowPartial
					? Chat.runButton(name, ChatFormatting.GOLD, command, command)
					: Chat.suggestButton(name, ChatFormatting.GOLD, command, "Puts " + command + " in chat for you to confirm"));
			if (i < shown - 1) message.append(" ");
		}
		if (candidates.size() > shown) {
			message.append(Chat.gray(" and " + (candidates.size() - shown) + " more"));
		}
		if (candidates.size() == 1) {
			message.append("?");
		}
		source.sendFailure(message);
		return Optional.empty();
	}

	private static LocationService requireService(CommandSourceStack source) {
		LocationService service = CoordManager.service();
		if (service == null) {
			source.sendFailure(Component.literal("Coord Manager couldn't load its saved locations. Check the server log."));
		}
		return service;
	}

	private static boolean isOperator(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private static boolean canTeleport(CommandSourceStack source, Config config) {
		return !config.teleportRequiresOp() || isOperator(source);
	}

	/** Players can change their own locations (and unowned ones from 1.x); operators and the console can change any. */
	private static boolean canModify(CommandSourceStack source, Location location) {
		if (!CoordManager.config().protectOthersLocations() || isOperator(source)) {
			return true;
		}
		ServerPlayer player = source.getPlayer();
		return player == null || location.isOwnedByOrUnowned(player.getUUID());
	}

	private static Component notYours(Location location) {
		return Component.literal("'" + location.name() + "' was saved by " + Objects.requireNonNullElse(location.creatorName(), "another player")
				+ ". Only they or an operator can change it.");
	}

	private static Optional<String> distanceFrom(ServerPlayer player, Location location) {
		if (player == null || !player.level().dimension().identifier().toString().equals(location.dimension())) {
			return Optional.empty();
		}
		return Optional.of(Chat.distance(location.distanceTo(player.getX(), player.getY(), player.getZ())));
	}

	private static int storageFailure(CommandSourceStack source, IOException e) {
		CoordManager.LOGGER.error("Couldn't write saved locations", e);
		source.sendFailure(Component.literal("Couldn't save the change: " + e.getMessage() + ". Check the server log."));
		return 0;
	}
}
