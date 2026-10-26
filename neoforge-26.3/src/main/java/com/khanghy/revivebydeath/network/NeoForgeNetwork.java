package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetwork {
	private NeoForgeNetwork() {
	}

	public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("revivebydeath");

		registrar.playToClient(
				ReviveCutscenePayload.TYPE,
				ReviveCutscenePayload.CODEC,
				(payload, context) -> context.enqueueWork(() ->
						ReviveByDeathClient.startCutscene(payload.durationTicks(), payload.frameCount(), payload.introFadeTicks())
				)
		);

		registrar.playToClient(
				ReviveEffectPayload.TYPE,
				ReviveEffectPayload.CODEC,
				(payload, context) -> context.enqueueWork(() ->
						ReviveByDeathClient.startReturnEffect(payload.durationTicks())
				)
		);
	}

	public static void sendTo(ServerPlayer player, ReviveCutscenePayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	public static void sendTo(ServerPlayer player, ReviveEffectPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}
}
