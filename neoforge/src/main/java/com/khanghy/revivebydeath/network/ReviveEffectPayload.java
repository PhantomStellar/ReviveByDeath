package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviveEffectPayload(int durationTicks) implements CustomPacketPayload {
	public static final Type<ReviveEffectPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "revive_effect"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ReviveEffectPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ReviveEffectPayload::durationTicks,
			ReviveEffectPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
