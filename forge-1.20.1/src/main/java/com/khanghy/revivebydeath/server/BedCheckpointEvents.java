package com.khanghy.revivebydeath.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class BedCheckpointEvents {
	private static final Map<UUID, PendingMessage> PENDING_MESSAGES = new HashMap<>();

	private BedCheckpointEvents() {
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
			return;
		}

		BlockState state = event.getLevel().getBlockState(event.getPos());
		if (!(state.getBlock() instanceof BedBlock)) {
			return;
		}

		if (CheckpointManager.saveManualCheckpoint(serverPlayer)) {
			queueMessage(serverPlayer, Component.literal("Rewind checkpoint set"));
		} else {
			queueMessage(serverPlayer, Component.literal("Rewind checkpoint needs safer ground"));
		}
	}

	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			tickMessages(event.getServer());
		}
	}

	private static void queueMessage(ServerPlayer player, Component message) {
		PENDING_MESSAGES.put(player.getUUID(), new PendingMessage(message, 1));
	}

	private static void tickMessages(MinecraftServer server) {
		if (PENDING_MESSAGES.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, PendingMessage>> iterator = PENDING_MESSAGES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PendingMessage> entry = iterator.next();
			PendingMessage pending = entry.getValue();
			if (pending.delayTicks() > 0) {
				entry.setValue(new PendingMessage(pending.message(), pending.delayTicks() - 1));
				continue;
			}

			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				player.sendSystemMessage(pending.message());
			}
			iterator.remove();
		}
	}

	private record PendingMessage(Component message, int delayTicks) {
	}
}
