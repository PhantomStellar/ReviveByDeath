package com.khanghy.revivebydeath.mixin;

import com.khanghy.revivebydeath.server.CheckpointManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerHurtMixin {
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void revivebydeath$ignoreDamageDuringRevive(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (CheckpointManager.isRevivePending(player) || CheckpointManager.hasPostReviveProtection(player)) {
			info.setReturnValue(false);
		}
	}
}
