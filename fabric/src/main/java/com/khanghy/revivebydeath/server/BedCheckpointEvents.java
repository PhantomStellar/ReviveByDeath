package com.khanghy.revivebydeath.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BedCheckpointEvents {
	private static final Map<UUID, PendingMessage> PENDING_MESSAGES = new HashMap<>();

	private BedCheckpointEvents() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			BlockState state = level.getBlockState(hitResult.getBlockPos());
			if (!(state.getBlock() instanceof BedBlock)) {
				return InteractionResult.PASS;
			}

			if (CheckpointManager.saveManualCheckpoint(serverPlayer)) {
				queueMessage(serverPlayer, Component.translatable("message.revivebydeath.checkpoint_set"));
			} else {
				queueMessage(serverPlayer, Component.translatable("message.revivebydeath.checkpoint_unsafe_ground"));
			}

			return InteractionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(BedCheckpointEvents::tickMessages);
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
