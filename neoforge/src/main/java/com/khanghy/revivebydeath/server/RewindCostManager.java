package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RewindCostManager {
	private RewindCostManager() {
	}

	public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && ReviveByDeathConfig.get().resetCostOnSleep) {
			if (player.isSleepingLongEnough()) {
				reset(player);
			}
		}
	}

	public static CostCheck check(ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		if (!config.useXpCost) {
			return new CostCheck(true, 0, usesSinceRest(player));
		}

		int cost = nextLevelCost(player);
		return new CostCheck(player.experienceLevel >= cost, cost, usesSinceRest(player));
	}

	public static void charge(ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		if (!config.useXpCost) {
			return;
		}

		int cost = nextLevelCost(player);
		player.giveExperienceLevels(-cost);
		RewindCostState.get(player.getServer()).incrementUses(player.getUUID());
	}

	public static void reset(ServerPlayer player) {
		RewindCostState.get(player.getServer()).resetUses(player.getUUID());
		CheckpointManager.revertRiftBlocks(player);
	}

	public static void resetOnDeath(ServerPlayer player) {
		if (ReviveByDeathConfig.get().resetCostOnDeath) {
			reset(player);
		}
	}

	public static int nextLevelCost(ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		int uses = usesSinceRest(player);
		double scaled = (config.minimumXpLevelCost + (double) uses * config.xpLevelCostIncrease)
				* Math.pow(config.xpLevelCostMultiplier, uses);
		return Math.max(0, (int) Math.ceil(scaled));
	}

	public static int usesSinceRest(ServerPlayer player) {
		return RewindCostState.get(player.getServer()).getUsesSinceRest(player.getUUID());
	}

	public static Component statusLine(ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		if (!config.useXpCost) {
			return Component.literal("XP cost: disabled");
		}

		int cost = nextLevelCost(player);
		return Component.literal("Next rewind cost: " + cost + " levels (" + player.experienceLevel + " available, " + usesSinceRest(player) + " used since rest)");
	}

	public record CostCheck(boolean allowed, int cost, int usesSinceRest) {
	}
}
