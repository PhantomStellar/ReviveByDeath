package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CheckpointSavedData extends SavedData {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "checkpoints");
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

	public static final Codec<CheckpointSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(UUID_CODEC, Checkpoint.CODEC).optionalFieldOf("checkpoints", Map.of())
					.forGetter(data -> data.checkpoints),
			Codec.unboundedMap(UUID_CODEC, CheckpointRiftData.CODEC).optionalFieldOf("checkpointRifts", Map.of())
					.forGetter(data -> data.checkpointRifts),
			Codec.unboundedMap(UUID_CODEC, Checkpoint.CODEC).optionalFieldOf("pendingLoginRevives", Map.of())
					.forGetter(data -> data.pendingLoginRevives)
	).apply(instance, CheckpointSavedData::new));

	public static final SavedDataType<CheckpointSavedData> TYPE = new SavedDataType<>(
			ID,
			CheckpointSavedData::new,
			CODEC,
			DataFixTypes.LEVEL);

	private final Map<UUID, Checkpoint> checkpoints = new HashMap<>();
	private final Map<UUID, CheckpointRiftData> checkpointRifts = new HashMap<>();
	private final Map<UUID, Checkpoint> pendingLoginRevives = new HashMap<>();

	public CheckpointSavedData() {
	}

	public CheckpointSavedData(Map<UUID, Checkpoint> checkpoints, Map<UUID, CheckpointRiftData> checkpointRifts,
			Map<UUID, Checkpoint> pendingLoginRevives) {
		this.checkpoints.putAll(checkpoints);
		this.checkpointRifts.putAll(checkpointRifts);
		this.pendingLoginRevives.putAll(pendingLoginRevives);
	}

	public static CheckpointSavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
		for (UUID playerId : new ArrayList<>(checkpointRifts.keySet())) {
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
						&& checkpointLevel.getBlockState(pos).equals(record.placedState())) {
					checkpointLevel.setBlockAndUpdate(pos, record.originalState());
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

	public record RiftRecord(BlockState originalState, BlockState placedState) {
	}

	public static class CheckpointRiftData {
		public static final Codec<CheckpointRiftData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("reviveCount", 0).forGetter(data -> data.reviveCount),
				Codec.list(RiftEntry.CODEC).optionalFieldOf("blocks", List.of())
						.forGetter(data -> {
							List<RiftEntry> entries = new ArrayList<>(data.originalBlocks.size());
							for (Map.Entry<BlockPos, RiftRecord> entry : data.originalBlocks.entrySet()) {
								entries.add(new RiftEntry(entry.getKey(), entry.getValue().originalState(),
										entry.getValue().placedState()));
							}
							return entries;
						})
		).apply(instance, CheckpointRiftData::new));

		public int reviveCount = 0;
		public final Map<BlockPos, RiftRecord> originalBlocks = new HashMap<>();

		public CheckpointRiftData() {
		}

		public CheckpointRiftData(int reviveCount, List<RiftEntry> blocks) {
			this.reviveCount = reviveCount;
			for (RiftEntry entry : blocks) {
				this.originalBlocks.put(entry.pos(), new RiftRecord(entry.originalState(), entry.placedState()));
			}
		}
	}

	private record RiftEntry(BlockPos pos, BlockState originalState, BlockState placedState) {
		public static final Codec<RiftEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(RiftEntry::pos),
				BlockState.CODEC.fieldOf("orig").forGetter(RiftEntry::originalState),
				BlockState.CODEC.fieldOf("placed").forGetter(RiftEntry::placedState)
		).apply(instance, RiftEntry::new));
	}
}
