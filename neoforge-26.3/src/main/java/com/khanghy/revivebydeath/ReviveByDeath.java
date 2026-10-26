package com.khanghy.revivebydeath;

import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import com.khanghy.revivebydeath.command.ModCommands;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.config.RevivePriority;
import com.khanghy.revivebydeath.network.NeoForgeNetwork;
import com.khanghy.revivebydeath.registry.ModEnchantments;
import com.khanghy.revivebydeath.registry.ModSounds;
import com.khanghy.revivebydeath.server.ActivationRules;
import com.khanghy.revivebydeath.server.BedCheckpointEvents;
import com.khanghy.revivebydeath.server.CheckpointManager;
import com.khanghy.revivebydeath.server.RewindCostManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(ReviveByDeath.MOD_ID)
public class ReviveByDeath {
	public static final String MOD_ID = "revivebydeath";
	public static final Logger LOGGER = LogUtils.getLogger();

	public ReviveByDeath(IEventBus modEventBus) {
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::registerPayloads);
		ModSounds.register(modEventBus);
		ModEnchantments.register();
		NeoForge.EVENT_BUS.register(this);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		LOGGER.info("ReviveByDeath NeoForge initialized.");
	}

	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		NeoForgeNetwork.onRegisterPayloadHandlers(event);
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		ModCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void onServerTick(ServerTickEvent.Post event) {
		CheckpointManager.tickServer(event.getServer());
		BedCheckpointEvents.onServerTick(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLivingDeathRewindFirst(LivingDeathEvent event) {
		if (ReviveByDeathConfig.get().revivePriority() == RevivePriority.REWIND_FIRST) {
			handleLivingDeath(event);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onLivingDeathDownedFirst(LivingDeathEvent event) {
		if (ReviveByDeathConfig.get().revivePriority() == RevivePriority.DOWNED_FIRST) {
			handleLivingDeath(event);
		}
	}

	private void handleLivingDeath(LivingDeathEvent event) {
		if (event.isCanceled()) {
			return;
		}
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		if (CheckpointManager.beginReviveSequence(player, event.getSource())) {
			event.setCanceled(true);
		} else {
			RewindCostManager.resetOnDeath(player);
		}
	}

	@SubscribeEvent
	public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& (CheckpointManager.isRevivePending(player) || CheckpointManager.hasPostReviveProtection(player))) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onLivingUseTotem(LivingUseTotemEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& !CheckpointManager.isRevivePending(player)
				&& CheckpointManager.hasUsableCheckpoint(player)
				&& ActivationRules.shouldSuppressVanillaTotem(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		BedCheckpointEvents.onRightClickBlock(event);
	}

	@SubscribeEvent
	public void onPlayerWakeUp(PlayerWakeUpEvent event) {
		RewindCostManager.onPlayerWakeUp(event);
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			CheckpointManager.onPlayerLoggedIn(player);
		}
	}

	@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
	public static final class ClientEvents {
		private ClientEvents() {
		}

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			ReviveByDeathClient.tickEffects();
		}

		@SubscribeEvent
		public static void onRenderGui(RenderGuiEvent.Post event) {
			if (ReviveByDeathClient.hasOverlayToRender()) {
				DeltaTracker deltaTracker = event.getPartialTick();
				ReviveByDeathClient.renderOverlay(event.getGuiGraphics(), deltaTracker.getGameTimeDeltaPartialTick(false));
			}
		}

		@SubscribeEvent
		public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
			ReviveByDeathClient.onDisconnect();
		}
	}
}
