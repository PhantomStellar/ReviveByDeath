package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ReviveCutscenePayload(int durationTicks, int frameCount, int introFadeTicks) {
	public static final ResourceLocation ID = new ResourceLocation(ReviveByDeath.MOD_ID, "revive_cutscene");

	public static void encode(ReviveCutscenePayload payload, FriendlyByteBuf buffer) {
		buffer.writeVarInt(payload.durationTicks);
		buffer.writeVarInt(payload.frameCount);
		buffer.writeVarInt(payload.introFadeTicks);
	}

	public static ReviveCutscenePayload decode(FriendlyByteBuf buffer) {
		return new ReviveCutscenePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
	}
}
