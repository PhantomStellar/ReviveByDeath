package com.khanghy.revivebydeath.mixin;

import com.khanghy.revivebydeath.server.ActivationRules;
import com.khanghy.revivebydeath.server.CheckpointManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityTotemMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void revivebydeath$letReviveSequenceReplaceVanillaTotem(DamageSource source, CallbackInfoReturnable<Boolean> info) {
		if ((Object) this instanceof ServerPlayer player
				&& !CheckpointManager.isRevivePending(player)
				&& CheckpointManager.hasUsableCheckpoint(player)
				&& ActivationRules.shouldSuppressVanillaTotem(player)) {
			info.setReturnValue(false);
		}
	}
}
