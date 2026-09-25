package com.khanghy.revivebydeath;

import com.khanghy.revivebydeath.command.ModCommands;
import com.khanghy.revivebydeath.network.ReviveEffectPayload;
import com.khanghy.revivebydeath.network.ReviveCutscenePayload;
import com.khanghy.revivebydeath.registry.ModEnchantments;
import com.khanghy.revivebydeath.registry.ModSounds;
import com.khanghy.revivebydeath.server.BedCheckpointEvents;
import com.khanghy.revivebydeath.server.CheckpointManager;
import com.khanghy.revivebydeath.server.RewindCostManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviveByDeath implements ModInitializer {
	public static final String MOD_ID = "revivebydeath";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantments.register();
		ModSounds.register();
		ModCommands.register();
		BedCheckpointEvents.register();
		RewindCostManager.registerEvents();
		PayloadTypeRegistry.playS2C().register(ReviveCutscenePayload.TYPE, ReviveCutscenePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ReviveEffectPayload.TYPE, ReviveEffectPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(CheckpointManager::tickServer);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CheckpointManager.onPlayerLoggedIn(handler.player));

		LOGGER.info("ReviveByDeath initialized.");
	}
}
