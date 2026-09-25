package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ReviveEffectPayload(int durationTicks) {
	public static final ResourceLocation ID = new ResourceLocation(ReviveByDeath.MOD_ID, "revive_effect");

	public static void encode(ReviveEffectPayload payload, FriendlyByteBuf buffer) {
		buffer.writeVarInt(payload.durationTicks);
	}

	public static ReviveEffectPayload decode(FriendlyByteBuf buffer) {
		return new ReviveEffectPayload(buffer.readVarInt());
	}
}
