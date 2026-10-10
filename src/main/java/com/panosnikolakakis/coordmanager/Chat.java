package com.panosnikolakakis.coordmanager;

import com.panosnikolakakis.coordmanager.core.Dimensions;
import com.panosnikolakakis.coordmanager.core.Location;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Builds the chat text for commands, so every message looks the same. */
final class Chat {
	private Chat() {
	}

	static MutableComponent text(String text) {
		return Component.literal(text);
	}

	static MutableComponent gray(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	static MutableComponent name(String name) {
		return Component.literal(name).withStyle(ChatFormatting.GOLD);
	}

	/** Location name that opens its details when clicked. */
	static MutableComponent clickableName(Location location) {
		return name(location.name()).withStyle(style -> style
				.withClickEvent(new ClickEvent.RunCommand("/loc find " + location.name()))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Show details"))));
	}

	static MutableComponent dimension(String dimensionId) {
		return Component.literal(Dimensions.displayName(dimensionId)).withStyle(dimensionColor(dimensionId));
	}

	/** Coordinates that copy themselves as "x y z" when clicked, ready to paste into a command. */
	static MutableComponent coordinates(Location location) {
		return Component.literal(location.x() + ", " + location.y() + ", " + location.z()).withStyle(style -> style
				.withColor(ChatFormatting.WHITE)
				.withClickEvent(new ClickEvent.CopyToClipboard(location.coordinates()))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
	}

	static MutableComponent runButton(String label, ChatFormatting color, String command, String hover) {
		return button(label, color, new ClickEvent.RunCommand(command), hover);
	}

	static MutableComponent suggestButton(String label, ChatFormatting color, String command, String hover) {
		return button(label, color, new ClickEvent.SuggestCommand(command), hover);
	}

	static MutableComponent copyButton(Location location) {
		return button("Copy", ChatFormatting.AQUA, new ClickEvent.CopyToClipboard(location.coordinates()), "Copy \"" + location.coordinates() + "\"");
	}

	private static MutableComponent button(String label, ChatFormatting color, ClickEvent click, String hover) {
		return Component.literal("[" + label + "]").withStyle(style -> style
				.withColor(color)
				.withClickEvent(click)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	static String distance(double blocks) {
		if (blocks >= 10_000) {
			return String.format("%.1f km", blocks / 1000);
		}
		return Math.round(blocks) + " m";
	}

	private static ChatFormatting dimensionColor(String dimensionId) {
		return switch (dimensionId) {
			case Dimensions.OVERWORLD -> ChatFormatting.GREEN;
			case Dimensions.NETHER -> ChatFormatting.RED;
			case Dimensions.END -> ChatFormatting.LIGHT_PURPLE;
			default -> ChatFormatting.AQUA;
		};
	}
}
