package com.khanghy.revivebydeath.client.mixin;

import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.config.RevivePriority;
import com.khanghy.revivebydeath.server.CheckpointManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonHooks.class, priority = 1100)
public class CommonHooksMixin {
	@Inject(method = "onLivingDeath", at = @At("HEAD"), cancellable = true, remap = false)
	private static void revivebydeath$rewindBeforeOtherReviveMods(LivingEntity entity, DamageSource source,
			CallbackInfoReturnable<Boolean> callback) {
		if (entity instanceof ServerPlayer player
				&& ReviveByDeathConfig.get().revivePriority() == RevivePriority.REWIND_FIRST
				&& CheckpointManager.beginReviveSequence(player, source)) {
			callback.setReturnValue(true);
		}
	}
}
