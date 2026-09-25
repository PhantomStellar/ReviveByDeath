package com.khanghy.revivebydeath.client.mixin;

import com.khanghy.revivebydeath.client.ReviveByDeathClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V",
					shift = At.Shift.BEFORE),
			remap = false)
	private void revivebydeath$renderCutsceneAboveEverything(DeltaTracker tickCounter, boolean renderLevel, CallbackInfo info) {
		if (!ReviveByDeathClient.hasOverlayToRender()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
		ReviveByDeathClient.renderOverlay(graphics, tickCounter.getGameTimeDeltaPartialTick(false));
	}
}
