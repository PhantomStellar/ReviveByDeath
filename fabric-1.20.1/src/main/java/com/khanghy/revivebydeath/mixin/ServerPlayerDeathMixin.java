package com.khanghy.revivebydeath.mixin;

import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.config.RevivePriority;
import com.khanghy.revivebydeath.server.CheckpointManager;
import com.khanghy.revivebydeath.server.RewindCostManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class, priority = 1100)
public class ServerPlayerDeathMixin {
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
	private void revivebydeath$reviveInsteadOfDeath(DamageSource source, CallbackInfo info) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (ReviveByDeathConfig.get().revivePriority() != RevivePriority.REWIND_FIRST) {
			return;
		}
		if (CheckpointManager.beginReviveSequence(player, source)) {
			info.cancel();
		} else {
			RewindCostManager.resetOnDeath(player);
		}
	}
}
