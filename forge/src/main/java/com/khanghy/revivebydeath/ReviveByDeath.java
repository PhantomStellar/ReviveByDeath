package com.khanghy.revivebydeath;

import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import com.khanghy.revivebydeath.command.ModCommands;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.config.RevivePriority;
import com.khanghy.revivebydeath.network.ForgeNetwork;
import com.khanghy.revivebydeath.registry.ModEnchantments;
import com.khanghy.revivebydeath.registry.ModSounds;
import com.khanghy.revivebydeath.server.ActivationRules;
import com.khanghy.revivebydeath.server.BedCheckpointEvents;
import com.khanghy.revivebydeath.server.CheckpointManager;
import com.khanghy.revivebydeath.server.RewindCostManager;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ReviveByDeath.MOD_ID)
public class ReviveByDeath {
	public static final String MOD_ID = "revivebydeath";
	public static final Logger LOGGER = LogUtils.getLogger();

	public ReviveByDeath(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();
		modEventBus.addListener(this::commonSetup);
		ModSounds.register(modEventBus);
		ModEnchantments.register();
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		ForgeNetwork.register();
		LOGGER.info("ReviveByDeath Forge initialized.");
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		ModCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent.Post event) {
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
	public void onLivingHurt(LivingHurtEvent event) {
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

	@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
	public static final class ClientEvents {
		private ClientEvents() {
		}

		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
			ReviveByDeathClient.tickEffects();
		}

		@SubscribeEvent
		public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
			ReviveByDeathClient.onDisconnect();
		}
	}
}
