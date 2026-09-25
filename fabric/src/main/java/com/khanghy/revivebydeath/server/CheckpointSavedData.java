package com.khanghy.revivebydeath.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public class CheckpointSavedData extends SavedData {
	private static final String ID = "revivebydeath_checkpoints";
	private static final SavedData.Factory<CheckpointSavedData> FACTORY = new SavedData.Factory<>(
			CheckpointSavedData::new,
			CheckpointSavedData::load,
			DataFixTypes.LEVEL);

	private final Map<UUID, Checkpoint> checkpoints = new HashMap<>();
	private final Map<UUID, CheckpointRiftData> checkpointRifts = new HashMap<>();
	private final Map<UUID, Checkpoint> pendingLoginRevives = new HashMap<>();

	public static CheckpointSavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
	}

	public Checkpoint getCheckpoint(UUID playerId) {
		return checkpoints.get(playerId);
	}

	public void setCheckpoint(UUID playerId, Checkpoint checkpoint) {
		checkpoints.put(playerId, checkpoint);
		setDirty();
	}

	public void removeCheckpoint(UUID playerId) {
		if (checkpoints.remove(playerId) != null) {
			setDirty();
		}
	}

	public boolean hasCheckpoint(UUID playerId) {
		return checkpoints.containsKey(playerId);
	}

	public CheckpointRiftData getRiftData(UUID playerId) {
		return checkpointRifts.get(playerId);
	}

	public CheckpointRiftData getOrCreateRiftData(UUID playerId) {
		CheckpointRiftData data = checkpointRifts.get(playerId);
		if (data == null) {
			data = new CheckpointRiftData();
			checkpointRifts.put(playerId, data);
			setDirty();
		}
		return data;
	}

	public CheckpointRiftData removeRiftData(UUID playerId) {
		CheckpointRiftData data = checkpointRifts.remove(playerId);
		if (data != null) {
			setDirty();
		}
		return data;
	}

	public void addPendingReviveOnLogin(UUID playerId, Checkpoint checkpoint) {
		pendingLoginRevives.put(playerId, checkpoint);
		setDirty();
	}

	public Checkpoint popPendingReviveOnLogin(UUID playerId) {
		Checkpoint checkpoint = pendingLoginRevives.remove(playerId);
		if (checkpoint != null) {
			setDirty();
		}
		return checkpoint;
	}

	public int revertAllRifts(MinecraftServer server) {
		int reverted = 0;
		for (UUID playerId : new java.util.ArrayList<>(checkpointRifts.keySet())) {
			Checkpoint checkpoint = checkpoints.get(playerId);
			if (checkpoint == null) {
				continue;
			}
			ServerLevel checkpointLevel = server.getLevel(checkpoint.dimension());
			if (checkpointLevel == null) {
				continue;
			}

			CheckpointRiftData riftData = checkpointRifts.get(playerId);
			for (Map.Entry<BlockPos, RiftRecord> entry : riftData.originalBlocks.entrySet()) {
				BlockPos pos = entry.getKey();
				RiftRecord record = entry.getValue();
				if (checkpointLevel.isInWorldBounds(pos)
						&& checkpointLevel.getBlockState(pos).equals(record.placedState)) {
					checkpointLevel.setBlockAndUpdate(pos, record.originalState);
				}
			}
			checkpointRifts.remove(playerId);
			setDirty();
			reverted++;
		}
		return reverted;
	}

	public void markDirtyData() {
		setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag checkpointsTag = new CompoundTag();
		for (Map.Entry<UUID, Checkpoint> entry : checkpoints.entrySet()) {
			checkpointsTag.put(entry.getKey().toString(), serializeCheckpoint(entry.getValue()));
		}
		tag.put("checkpoints", checkpointsTag);

		CompoundTag riftsTag = new CompoundTag();
		for (Map.Entry<UUID, CheckpointRiftData> entry : checkpointRifts.entrySet()) {
			riftsTag.put(entry.getKey().toString(), serializeRiftData(entry.getValue()));
		}
		tag.put("checkpointRifts", riftsTag);

		CompoundTag pendingTag = new CompoundTag();
		for (Map.Entry<UUID, Checkpoint> entry : pendingLoginRevives.entrySet()) {
			pendingTag.put(entry.getKey().toString(), serializeCheckpoint(entry.getValue()));
		}
		tag.put("pendingLoginRevives", pendingTag);

		return tag;
	}

	private static CheckpointSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		CheckpointSavedData data = new CheckpointSavedData();
		HolderGetter<Block> blockGetter = registries.lookupOrThrow(Registries.BLOCK);

		if (tag.contains("checkpoints", Tag.TAG_COMPOUND)) {
			CompoundTag checkpointsTag = tag.getCompound("checkpoints");
			for (String key : checkpointsTag.getAllKeys()) {
				try {
					UUID uuid = UUID.fromString(key);
					Checkpoint checkpoint = deserializeCheckpoint(checkpointsTag.getCompound(key));
					if (checkpoint != null) {
						data.checkpoints.put(uuid, checkpoint);
					}
				} catch (Exception ignored) {
				}
			}
		}

		if (tag.contains("checkpointRifts", Tag.TAG_COMPOUND)) {
			CompoundTag riftsTag = tag.getCompound("checkpointRifts");
			for (String key : riftsTag.getAllKeys()) {
				try {
					UUID uuid = UUID.fromString(key);
					CheckpointRiftData riftData = deserializeRiftData(riftsTag.getCompound(key), blockGetter);
					if (riftData != null) {
						data.checkpointRifts.put(uuid, riftData);
					}
				} catch (Exception ignored) {
				}
			}
		}

		if (tag.contains("pendingLoginRevives", Tag.TAG_COMPOUND)) {
			CompoundTag pendingTag = tag.getCompound("pendingLoginRevives");
			for (String key : pendingTag.getAllKeys()) {
				try {
					UUID uuid = UUID.fromString(key);
					Checkpoint checkpoint = deserializeCheckpoint(pendingTag.getCompound(key));
					if (checkpoint != null) {
						data.pendingLoginRevives.put(uuid, checkpoint);
					}
				} catch (Exception ignored) {
				}
			}
		}

		return data;
	}

	private static CompoundTag serializeCheckpoint(Checkpoint checkpoint) {
		CompoundTag tag = new CompoundTag();
		tag.putString("dim", checkpoint.dimension().location().toString());
		tag.putDouble("x", checkpoint.x());
		tag.putDouble("y", checkpoint.y());
		tag.putDouble("z", checkpoint.z());
		tag.putFloat("yaw", checkpoint.yaw());
		tag.putFloat("pitch", checkpoint.pitch());
		tag.putLong("tick", checkpoint.savedAtTick());
		return tag;
	}

	private static Checkpoint deserializeCheckpoint(CompoundTag tag) {
		if (!tag.contains("dim") || !tag.contains("x") || !tag.contains("y") || !tag.contains("z")) {
			return null;
		}
		ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dim")));
		return new Checkpoint(
				dim,
				tag.getDouble("x"),
				tag.getDouble("y"),
				tag.getDouble("z"),
				tag.getFloat("yaw"),
				tag.getFloat("pitch"),
				tag.getLong("tick"));
	}

	private static CompoundTag serializeRiftData(CheckpointRiftData riftData) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("reviveCount", riftData.reviveCount);
		ListTag blocksList = new ListTag();
		for (Map.Entry<BlockPos, RiftRecord> entry : riftData.originalBlocks.entrySet()) {
			CompoundTag blockTag = new CompoundTag();
			blockTag.putLong("pos", entry.getKey().asLong());
			blockTag.put("orig", NbtUtils.writeBlockState(entry.getValue().originalState));
			blockTag.put("placed", NbtUtils.writeBlockState(entry.getValue().placedState));
			blocksList.add(blockTag);
		}
		tag.put("blocks", blocksList);
		return tag;
	}

	private static CheckpointRiftData deserializeRiftData(CompoundTag tag, HolderGetter<Block> blockGetter) {
		CheckpointRiftData data = new CheckpointRiftData();
		data.reviveCount = tag.getInt("reviveCount");
		if (tag.contains("blocks", Tag.TAG_LIST)) {
			ListTag blocksList = tag.getList("blocks", Tag.TAG_COMPOUND);
			for (int i = 0; i < blocksList.size(); i++) {
				CompoundTag blockTag = blocksList.getCompound(i);
				BlockPos pos = BlockPos.of(blockTag.getLong("pos"));
				BlockState orig = NbtUtils.readBlockState(blockGetter, blockTag.getCompound("orig"));
				BlockState placed = NbtUtils.readBlockState(blockGetter, blockTag.getCompound("placed"));
				if (orig != null && placed != null) {
					data.originalBlocks.put(pos, new RiftRecord(orig, placed));
				}
			}
		}
		return data;
	}

	public static class RiftRecord {
		public final BlockState originalState;
		public final BlockState placedState;

		public RiftRecord(BlockState originalState, BlockState placedState) {
			this.originalState = originalState;
			this.placedState = placedState;
		}
	}

	public static class CheckpointRiftData {
		public int reviveCount = 0;
		public final Map<BlockPos, RiftRecord> originalBlocks = new HashMap<>();
	}
}
