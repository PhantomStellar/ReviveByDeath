package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviveCutscenePayload(int durationTicks, int frameCount, int introFadeTicks) implements CustomPacketPayload {
	public static final Type<ReviveCutscenePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "revive_cutscene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ReviveCutscenePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ReviveCutscenePayload::durationTicks,
			ByteBufCodecs.VAR_INT,
			ReviveCutscenePayload::frameCount,
			ByteBufCodecs.VAR_INT,
			ReviveCutscenePayload::introFadeTicks,
			ReviveCutscenePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
