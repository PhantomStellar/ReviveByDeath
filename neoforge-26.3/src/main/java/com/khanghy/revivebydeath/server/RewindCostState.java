package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RewindCostState extends SavedData {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "rewind_costs");
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

	public static final Codec<RewindCostState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(UUID_CODEC, Codec.INT).optionalFieldOf("players", Map.of())
					.forGetter(state -> state.usesSinceRest)
	).apply(instance, RewindCostState::new));

	public static final SavedDataType<RewindCostState> TYPE = new SavedDataType<>(
			ID,
			RewindCostState::new,
			CODEC,
			DataFixTypes.LEVEL);

	private final Map<UUID, Integer> usesSinceRest = new HashMap<>();

	public RewindCostState() {
	}

	public RewindCostState(Map<UUID, Integer> usesSinceRest) {
		this.usesSinceRest.putAll(usesSinceRest);
	}

	public static RewindCostState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
}
