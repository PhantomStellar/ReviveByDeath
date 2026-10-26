package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.network.NeoForgeNetwork;
import com.khanghy.revivebydeath.network.ReviveCutscenePayload;
import com.khanghy.revivebydeath.network.ReviveEffectPayload;
import com.khanghy.revivebydeath.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public final class CheckpointManager {
	private static final Map<UUID, PendingRevive> PENDING_REVIVES = new HashMap<>();
	private static final Map<UUID, ReturnEffect> RETURN_EFFECTS = new HashMap<>();
	private static final Map<UUID, Integer> POST_REVIVE_PROTECTION = new HashMap<>();
	private static final Map<UUID, Integer> LAST_CHECKPOINT_CHECKS = new HashMap<>();
	private static final Map<UUID, Integer> ACTIVE_SHADOWS = new HashMap<>();
	private static final float REVIVE_HEALTH = 8.0F;
	private static final int REVIVE_FOOD = 8;

	private CheckpointManager() {
	}

	public static void tickServer(MinecraftServer server) {
		tickPendingRevives(server);
		tickReturnEffects(server);
		tickPostReviveProtection(server);
		tickActiveShadows(server);

		int serverTicks = server.getTickCount();
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		CheckpointSavedData savedData = CheckpointSavedData.get(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID uuid = player.getUUID();
			if (isRevivePending(player)) {
				continue;
			}

			boolean hasCheckpoint = savedData.hasCheckpoint(uuid);
			int interval = hasCheckpoint ? config.checkpointRecheckTicks : config.checkpointInitialCheckTicks;
			interval = Math.max(1, interval);
			int lastCheckedTick = LAST_CHECKPOINT_CHECKS.getOrDefault(uuid, serverTicks - interval);
			if (serverTicks - lastCheckedTick < interval) {
				continue;
			}

			LAST_CHECKPOINT_CHECKS.put(uuid, serverTicks);
			if (isSafeCheckpoint(player)) {
				saveCheckpoint(player, serverTicks);
			}
		}
	}

	public static boolean saveManualCheckpoint(ServerPlayer player) {
		if (isRevivePending(player) || !isSafeCheckpoint(player)) {
			return false;
		}

		int serverTicks = player.level().getServer().getTickCount();
		saveCheckpoint(player, serverTicks);
		LAST_CHECKPOINT_CHECKS.put(player.getUUID(), serverTicks);
		return true;
	}

	public static boolean beginReviveSequence(ServerPlayer player, DamageSource source) {
		if (isRevivePending(player)) {
			stabilizePendingPlayer(player);
			return true;
		}

		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}

		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		CheckpointSavedData savedData = CheckpointSavedData.get(server);
		Checkpoint checkpoint = savedData.getCheckpoint(player.getUUID());
		if (checkpoint == null) {
			DebugLogManager.send(player, "No safe checkpoint is saved yet.");
			return false;
		}

		ServerLevel level = server.getLevel(checkpoint.dimension());
		if (level == null || !isCheckpointStillSafe(level, checkpoint)) {
			revertRiftBlocks(player);
			savedData.removeCheckpoint(player.getUUID());
			DebugLogManager.send(player, "Saved checkpoint is no longer safe.");
			return false;
		}

		ActivationResult activation = ActivationRules.evaluate(player);
		if (!activation.allowed()) {
			ReviveByDeath.LOGGER.debug("Revive denied for {}: {}.", player.getPlainTextName(), activation.failureReason());
			DebugLogManager.send(player, "Activation failed: " + activation.failureReason() + ".");
			return false;
		}

		int introFadeTicks = cutsceneIntroFadeTicks(config);
		int cutsceneTicks = cutsceneTicks(config);
		int cutsceneFrames = cutsceneFrames(config);
		int pendingTicks = pendingTicks(config);

		PendingRevive pendingRevive = new PendingRevive(
				checkpoint,
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				pendingTicks,
				pendingTicks);
		PENDING_REVIVES.put(player.getUUID(), pendingRevive);
		activation.consume().run();
		stabilizePendingPlayer(player);
		if (config.spawnPastShadow) {
			spawnPastShadow(player, (ServerLevel) player.level());
		}
		playCutsceneStartFeedback(player, (ServerLevel) player.level(), cutsceneTicks, cutsceneFrames, introFadeTicks, config.enableCutscene);

		ReviveByDeath.LOGGER.debug("Started revive cutscene for {} from {} at checkpoint saved on tick {}.",
				player.getPlainTextName(), source.getMsgId(), checkpoint.savedAtTick());
		return true;
	}

	public static boolean isRevivePending(ServerPlayer player) {
		return PENDING_REVIVES.containsKey(player.getUUID());
	}

	public static boolean hasPostReviveProtection(ServerPlayer player) {
		return POST_REVIVE_PROTECTION.containsKey(player.getUUID());
	}

	public static boolean hasUsableCheckpoint(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return false;
		}

		Checkpoint checkpoint = CheckpointSavedData.get(server).getCheckpoint(player.getUUID());
		if (checkpoint == null) {
			return false;
		}

		ServerLevel level = server.getLevel(checkpoint.dimension());
		return level != null && isCheckpointStillSafe(level, checkpoint);
	}

	public static void onPlayerLoggedIn(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}

		Checkpoint pendingCheckpoint = CheckpointSavedData.get(server).popPendingReviveOnLogin(player.getUUID());
		if (pendingCheckpoint != null) {
			PendingRevive pendingRevive = new PendingRevive(
					pendingCheckpoint,
					player.getX(),
					player.getY(),
					player.getZ(),
					player.getYRot(),
					player.getXRot(),
					1,
					0);
			completeRevive(player, pendingRevive);
			player.sendSystemMessage(Component.translatable("message.revivebydeath.reconnect_completed"));
		}
	}

	private static void tickPendingRevives(MinecraftServer server) {
		if (PENDING_REVIVES.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, PendingRevive> entry : PENDING_REVIVES.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				CheckpointSavedData.get(server).addPendingReviveOnLogin(entry.getKey(), entry.getValue().checkpoint());
				finished.add(entry.getKey());
				continue;
			}

			PendingRevive pendingRevive = entry.getValue();
			stabilizePendingPlayer(player);
			player.teleportTo(pendingRevive.holdX(), pendingRevive.holdY(), pendingRevive.holdZ());

			int nextTicks = pendingRevive.remainingTicks() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
				completeRevive(player, pendingRevive);
			} else {
				entry.setValue(pendingRevive.withRemainingTicks(nextTicks));
			}
		}

		for (UUID uuid : finished) {
			PENDING_REVIVES.remove(uuid);
		}
	}

	private static void tickReturnEffects(MinecraftServer server) {
		if (RETURN_EFFECTS.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, ReturnEffect> entry : RETURN_EFFECTS.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				finished.add(entry.getKey());
				continue;
			}

			ReturnEffect effect = entry.getValue();
			int ageTicks = effect.totalTicks() - effect.remainingTicks();
			if (player.level() instanceof ServerLevel level) {
				ParticlePatterns.returnPulse(level, player, ageTicks, effect.totalTicks());
			}

			int nextTicks = effect.remainingTicks() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
			} else {
				entry.setValue(effect.withRemainingTicks(nextTicks));
			}
		}

		for (UUID uuid : finished) {
			RETURN_EFFECTS.remove(uuid);
		}
	}

	private static void tickPostReviveProtection(MinecraftServer server) {
		if (POST_REVIVE_PROTECTION.isEmpty()) {
			return;
		}

		List<UUID> finished = new ArrayList<>();
		for (Map.Entry<UUID, Integer> entry : POST_REVIVE_PROTECTION.entrySet()) {
			if (server.getPlayerList().getPlayer(entry.getKey()) == null) {
				finished.add(entry.getKey());
				continue;
			}

			int nextTicks = entry.getValue() - 1;
			if (nextTicks <= 0) {
				finished.add(entry.getKey());
			} else {
				entry.setValue(nextTicks);
			}
		}

		for (UUID uuid : finished) {
			POST_REVIVE_PROTECTION.remove(uuid);
		}
	}

	private static void tickActiveShadows(MinecraftServer server) {
		if (ACTIVE_SHADOWS.isEmpty()) {
			return;
		}

		List<UUID> toRemove = new ArrayList<>();
		for (Map.Entry<UUID, Integer> entry : ACTIVE_SHADOWS.entrySet()) {
			int nextTicks = entry.getValue() - 1;
			if (nextTicks <= 0) {
				toRemove.add(entry.getKey());
				Entity entity = server.overworld().getEntityInAnyDimension(entry.getKey());
				if (entity != null && entity.isAlive()) {
					if (entity.level() instanceof ServerLevel entityLevel) {
						entityLevel.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 10, 0.25D, 0.35D, 0.25D, 0.05D);
					}
					entity.discard();
				}
			} else {
				entry.setValue(nextTicks);
			}
		}

		for (UUID uuid : toRemove) {
			ACTIVE_SHADOWS.remove(uuid);
		}
	}

	private static void completeRevive(ServerPlayer player, PendingRevive pendingRevive) {
		Checkpoint checkpoint = pendingRevive.checkpoint();
		ServerLevel level = player.level().getServer().getLevel(checkpoint.dimension());
		boolean safe = level != null && isCheckpointStillSafe(level, checkpoint);

		player.deathTime = 0;
		player.hurtTime = 0;
		player.hurtDuration = 0;
		player.clearFire();
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		player.setHealth(Math.min(player.getMaxHealth(), REVIVE_HEALTH));
		player.setAbsorptionAmount(0.0F);

		FoodData foodData = player.getFoodData();
		foodData.setFoodLevel(Math.max(foodData.getFoodLevel(), REVIVE_FOOD));
		foodData.setSaturation(Math.max(foodData.getSaturationLevel(), 2.0F));

		if (safe) {
			player.teleportTo(level, checkpoint.x(), checkpoint.y(), checkpoint.z(), Set.of(), checkpoint.yaw(), checkpoint.pitch(), false);
			if (ReviveByDeathConfig.get().createDimensionalRift) {
				createDimensionalRift(level, checkpoint, player);
			}
		} else {
			ReviveByDeath.LOGGER.warn("Checkpoint for {} became unsafe or dimension unavailable; redirecting to safe fallback spawn.", player.getPlainTextName());
			teleportToSafeFallback(player);
		}

		int protectionTicks = Math.max(0, ReviveByDeathConfig.get().postReviveInvulnerabilityTicks);
		if (protectionTicks > 0) {
			POST_REVIVE_PROTECTION.put(player.getUUID(), protectionTicks);
		}
		int returnTicks = Math.max(1, ReviveByDeathConfig.get().returnEffectTicks);
		playReturnFeedback(player, (ServerLevel) player.level(), returnTicks);
		RETURN_EFFECTS.put(player.getUUID(), new ReturnEffect(returnTicks, returnTicks));
	}

	private static void teleportToSafeFallback(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}

		LevelData.RespawnData respawnData = player.getRespawnConfig().respawnData();
		if (respawnData != LevelData.RespawnData.DEFAULT && !respawnData.pos().equals(BlockPos.ZERO)) {
			ServerLevel respawnLevel = server.getLevel(respawnData.dimension());
			if (respawnLevel != null) {
				BlockPos feet = respawnData.pos().above();
				if (!respawnLevel.getBlockState(feet).isAir()) {
					feet = respawnData.pos();
				}
				player.teleportTo(respawnLevel, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, Set.of(), respawnData.yaw(), 0.0F, false);
				player.sendSystemMessage(Component.translatable("message.revivebydeath.checkpoint_fallback_spawn"));
				return;
			}
		}

		ServerLevel overworld = server.overworld();
		BlockPos sharedSpawn = overworld.getRespawnData().pos();
		BlockPos safePos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sharedSpawn);
		player.teleportTo(overworld, safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D, Set.of(), 0.0F, 0.0F, false);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.checkpoint_fallback_world"));
	}

	private static void stabilizePendingPlayer(ServerPlayer player) {
		player.deathTime = 0;
		player.hurtTime = 0;
		player.hurtDuration = 0;
		player.clearFire();
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		player.setHealth(Math.max(player.getHealth(), 1.0F));
	}

	private static void playCutsceneStartFeedback(ServerPlayer player, ServerLevel level, int cutsceneTicks, int cutsceneFrames, int introFadeTicks, boolean enabled) {
		if (enabled) {
			level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0D, player.getZ(), 10, 0.2D, 0.35D, 0.2D, 0.015D);
		}

		ReviveCutscenePayload payload = new ReviveCutscenePayload(cutsceneTicks, cutsceneFrames, introFadeTicks);
		NeoForgeNetwork.sendTo(player, payload);
	}

	private static void playReturnFeedback(ServerPlayer player, ServerLevel level, int returnTicks) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RETURN_WHOOSH.get(), SoundSource.PLAYERS, 0.28F, 1.0F);
		ParticlePatterns.returnBurst(level, player);
		ReviveEffectPayload payload = new ReviveEffectPayload(returnTicks);
		NeoForgeNetwork.sendTo(player, payload);
	}

	private static void saveCheckpoint(ServerPlayer player, int serverTicks) {
		revertRiftBlocks(player);
		Checkpoint checkpoint = new Checkpoint(
				player.level().dimension(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				serverTicks);
		CheckpointSavedData.get(player.level().getServer()).setCheckpoint(player.getUUID(), checkpoint);
	}

	private static boolean isSafeCheckpoint(ServerPlayer player) {
		if (!player.isAlive() || player.isDeadOrDying() || player.isSpectator() || !player.onGround()) {
			return false;
		}

		if (player.isInLava() || player.isOnFire() || player.getY() < player.level().getMinY() + 2) {
			return false;
		}

		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}

		return isCheckpointStillSafe(level, new Checkpoint(
				level.dimension(),
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYRot(),
				player.getXRot(),
				level.getServer().getTickCount()));
	}

	private static boolean isCheckpointStillSafe(ServerLevel level, Checkpoint checkpoint) {
		BlockPos feet = BlockPos.containing(checkpoint.x(), checkpoint.y(), checkpoint.z());
		BlockPos head = feet.above();
		BlockPos floor = feet.below();

		if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(head) || !level.isInWorldBounds(floor)) {
			return false;
		}

		if (level.getFluidState(feet).is(FluidTags.LAVA) || level.getFluidState(head).is(FluidTags.LAVA)) {
			return false;
		}

		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		BlockState floorState = level.getBlockState(floor);
		boolean feetClear = feetState.getCollisionShape(level, feet).isEmpty();
		boolean headClear = headState.getCollisionShape(level, head).isEmpty();

		return feetClear
				&& headClear
				&& !floorState.isAir()
				&& !floorState.liquid()
				&& floorState.isFaceSturdy(level, floor, Direction.UP);
	}

	private static int cutsceneIntroFadeTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? Math.max(0, config.cutsceneIntroFadeTicks) : 0;
	}

	private static int cutsceneTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? Math.max(1, config.cutsceneTicks) : 0;
	}

	private static int cutsceneFrames(ReviveByDeathConfig config) {
		return Math.max(1, config.cutsceneFrames);
	}

	private static int pendingTicks(ReviveByDeathConfig config) {
		return config.enableCutscene ? cutsceneIntroFadeTicks(config) + cutsceneTicks(config) : 1;
	}

	private static void spawnPastShadow(ServerPlayer player, ServerLevel level) {
		Zombie shadow = (Zombie) EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
		if (shadow != null) {
			shadow.absSnapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
			shadow.setCustomName(Component.literal(player.getPlainTextName() + "'s Shadow"));
			shadow.setCustomNameVisible(true);

			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					shadow.setItemSlot(slot, stack.copy());
					shadow.setDropChance(slot, 0.0F);
				}
			}

			shadow.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 1));    // Speed II permanent
			shadow.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0)); // Strength I permanent
			shadow.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)); // Glowing for 10 seconds
			shadow.setCanPickUpLoot(false);

			level.addFreshEntity(shadow);
			ACTIVE_SHADOWS.put(shadow.getUUID(), 1200);
		}
	}

	private static void createDimensionalRift(ServerLevel level, Checkpoint checkpoint, ServerPlayer player) {
		BlockPos centerFloor = BlockPos.containing(checkpoint.x(), checkpoint.y(), checkpoint.z()).below();
		CheckpointSavedData savedData = CheckpointSavedData.get(player.level().getServer());
		CheckpointSavedData.CheckpointRiftData riftData = savedData.getOrCreateRiftData(player.getUUID());
		riftData.reviveCount++;
		savedData.markDirtyData();

		int radius = Math.min(1 + (riftData.reviveCount - 1) / 2, 3);
		double replaceChance = Math.min(0.20 + (riftData.reviveCount - 1) * 0.15, 0.70);

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius + 1) {
					BlockPos pos = centerFloor.offset(dx, 0, dz);
					if (level.isInWorldBounds(pos)) {
						BlockState state = level.getBlockState(pos);

						// If already tracked, check if player modified it
						if (riftData.originalBlocks.containsKey(pos)) {
							CheckpointSavedData.RiftRecord record = riftData.originalBlocks.get(pos);
							if (!state.equals(record.placedState())) {
								riftData.originalBlocks.remove(pos);
								savedData.markDirtyData();
							}
							continue;
						}

						if (!state.isAir() && !state.liquid() && !state.hasBlockEntity()
								&& net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft")
								&& state.isFaceSturdy(level, pos, Direction.UP)) {
							if (level.getRandom().nextDouble() < replaceChance) {
								BlockState placedState = getRiftReplacementBlock(state);
								if (placedState != null && !placedState.equals(state)) {
									riftData.originalBlocks.putIfAbsent(pos, new CheckpointSavedData.RiftRecord(state, placedState));
									savedData.markDirtyData();
									level.setBlockAndUpdate(pos, placedState);
									level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 4, 0.2D, 0.1D, 0.2D, 0.05D);
									level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 0.3F, 0.8F);
								}
							}
						}
					}
				}
			}
		}
	}

	private static BlockState getRiftReplacementBlock(BlockState state) {
		if (state.is(Blocks.STONE_BRICKS)) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		} else if (state.is(Blocks.DEEPSLATE_BRICKS)) {
			return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
		} else if (state.is(Blocks.DEEPSLATE_TILES)) {
			return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
		} else if (state.is(Blocks.NETHER_BRICKS)) {
			return Blocks.CRACKED_NETHER_BRICKS.defaultBlockState();
		} else if (state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)) {
			return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		}

		if (state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.PLANKS)) {
			return Blocks.BASALT.defaultBlockState();
		}

		if (state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)) {
			return Blocks.TUFF.defaultBlockState();
		}

		if (state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
			return Blocks.COARSE_DIRT.defaultBlockState();
		}

		if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
			return Blocks.GRAVEL.defaultBlockState();
		}

		return null; // Leave unrecognized and modded blocks untouched.
	}

	public static void revertRiftBlocks(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}

		CheckpointSavedData savedData = CheckpointSavedData.get(server);
		CheckpointSavedData.CheckpointRiftData riftData = savedData.getRiftData(player.getUUID());
		if (riftData == null || riftData.originalBlocks.isEmpty()) {
			return;
		}

		Checkpoint activeCheckpoint = savedData.getCheckpoint(player.getUUID());
		if (activeCheckpoint == null) {
			return;
		}

		ServerLevel checkpointLevel = server.getLevel(activeCheckpoint.dimension());
		if (checkpointLevel == null) {
			return;
		}

		savedData.removeRiftData(player.getUUID());
		for (Map.Entry<BlockPos, CheckpointSavedData.RiftRecord> entry : riftData.originalBlocks.entrySet()) {
			BlockPos pos = entry.getKey();
			CheckpointSavedData.RiftRecord record = entry.getValue();

			if (checkpointLevel.isInWorldBounds(pos)) {
				BlockState currentState = checkpointLevel.getBlockState(pos);
				if (currentState.equals(record.placedState())) {
					checkpointLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 5, 0.2D, 0.2D, 0.2D, 0.02D);
					checkpointLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.2F);
					checkpointLevel.setBlockAndUpdate(pos, record.originalState());
				}
			}
		}
	}
}
