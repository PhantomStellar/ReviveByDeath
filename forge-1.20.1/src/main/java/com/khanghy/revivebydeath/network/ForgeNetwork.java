package com.khanghy.revivebydeath.network;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ForgeNetwork {
	private static final String PROTOCOL_VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(ReviveByDeath.MOD_ID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	private ForgeNetwork() {
	}

	public static void register() {
		CHANNEL.messageBuilder(ReviveCutscenePayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(ReviveCutscenePayload::encode)
				.decoder(ReviveCutscenePayload::decode)
				.consumerMainThread((payload, contextSupplier) -> {
					contextSupplier.get().enqueueWork(() -> handleCutscene(payload));
					contextSupplier.get().setPacketHandled(true);
				})
				.add();

		CHANNEL.messageBuilder(ReviveEffectPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(ReviveEffectPayload::encode)
				.decoder(ReviveEffectPayload::decode)
				.consumerMainThread((payload, contextSupplier) -> {
					contextSupplier.get().enqueueWork(() -> handleEffect(payload));
					contextSupplier.get().setPacketHandled(true);
				})
				.add();
	}

	public static void sendTo(ServerPlayer player, ReviveCutscenePayload payload) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
	}

	public static void sendTo(ServerPlayer player, ReviveEffectPayload payload) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleCutscene(ReviveCutscenePayload payload) {
		ReviveByDeathClient.startCutscene(payload.durationTicks(), payload.frameCount(), payload.introFadeTicks());
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleEffect(ReviveEffectPayload payload) {
		ReviveByDeathClient.startReturnEffect(payload.durationTicks());
	}
}
