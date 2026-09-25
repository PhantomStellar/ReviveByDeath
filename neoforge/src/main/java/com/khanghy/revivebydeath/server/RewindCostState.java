package com.khanghy.revivebydeath.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class RewindCostState extends SavedData {
	private static final String ID = "revivebydeath_rewind_costs";
	private static final SavedData.Factory<RewindCostState> FACTORY = new SavedData.Factory<>(
			RewindCostState::new,
			RewindCostState::load,
			DataFixTypes.LEVEL);

	private final Map<UUID, Integer> usesSinceRest = new HashMap<>();

	public static RewindCostState get(net.minecraft.server.MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
	}

	public int getUsesSinceRest(UUID playerId) {
		return usesSinceRest.getOrDefault(playerId, 0);
	}

	public void incrementUses(UUID playerId) {
		usesSinceRest.put(playerId, getUsesSinceRest(playerId) + 1);
		setDirty();
	}

	public void resetUses(UUID playerId) {
		if (usesSinceRest.remove(playerId) != null) {
			setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag players = new CompoundTag();
		for (Map.Entry<UUID, Integer> entry : usesSinceRest.entrySet()) {
			players.putInt(entry.getKey().toString(), entry.getValue());
		}

		tag.put("players", players);
		return tag;
	}

	private static RewindCostState load(CompoundTag tag, HolderLookup.Provider registries) {
		RewindCostState state = new RewindCostState();
		CompoundTag players = tag.getCompound("players");
		for (String key : players.getAllKeys()) {
			try {
				state.usesSinceRest.put(UUID.fromString(key), players.getInt(key));
			} catch (IllegalArgumentException ignored) {
			}
		}

		return state;
	}
}
