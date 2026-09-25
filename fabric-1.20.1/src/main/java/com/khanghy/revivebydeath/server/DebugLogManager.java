package com.khanghy.revivebydeath.server;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DebugLogManager {
	private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

	private DebugLogManager() {
	}

	public static boolean isEnabled(ServerPlayer player) {
		return ENABLED_PLAYERS.contains(player.getUUID());
	}

	public static void setEnabled(ServerPlayer player, boolean enabled) {
		if (enabled) {
			ENABLED_PLAYERS.add(player.getUUID());
		} else {
			ENABLED_PLAYERS.remove(player.getUUID());
		}
	}

	public static void send(ServerPlayer player, String message) {
		if (isEnabled(player)) {
			player.sendSystemMessage(Component.literal("[ReviveByDeath] " + message));
		}
	}
}
