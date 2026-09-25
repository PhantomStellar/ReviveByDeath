package com.khanghy.revivebydeath.client;

import com.khanghy.revivebydeath.ReviveByDeath;
import com.khanghy.revivebydeath.registry.ModSounds;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class ReviveByDeathClient {
	private static final int CUTSCENE_TEXTURE_WIDTH = 854;
	private static final int CUTSCENE_TEXTURE_HEIGHT = 480;
	private static final SoundSource[] CUTSCENE_MUTED_SOURCES = {
			SoundSource.MUSIC,
			SoundSource.RECORDS,
			SoundSource.WEATHER,
			SoundSource.BLOCKS,
			SoundSource.HOSTILE,
			SoundSource.NEUTRAL,
			SoundSource.AMBIENT,
			SoundSource.VOICE
	};
	private static final Map<SoundSource, Float> previousSourceVolumes = new EnumMap<>(SoundSource.class);

	private static int introFadeTicks;
	private static int maxIntroFadeTicks = 1;
	private static int cutsceneTicks;
	private static int maxCutsceneTicks = 1;
	private static int cutsceneFrameCount = 1;
	private static int returnEffectTicks;
	private static int maxReturnEffectTicks = 1;
	private static boolean forcedHideGui;
	private static boolean previousHideGui;
	private static boolean mutedCutsceneAudio;
	private static boolean cutsceneSoundPlayed;

	private ReviveByDeathClient() {
	}

	public static void startCutscene(int durationTicks, int frameCount, int introTicks) {
		Minecraft client = Minecraft.getInstance();
		introFadeTicks = Math.max(0, introTicks);
		maxIntroFadeTicks = Math.max(1, introFadeTicks);
		cutsceneTicks = Math.max(0, durationTicks);
		maxCutsceneTicks = Math.max(1, cutsceneTicks);
		cutsceneFrameCount = Math.max(1, frameCount);
		cutsceneSoundPlayed = false;
		client.setScreen(null);

		if (introFadeTicks > 0 || cutsceneTicks > 0) {
			forceHideGui(client);
			muteEnvironment(client);

			if (introFadeTicks <= 0) {
				playCutsceneSound(client);
			}
		}

		if (client.player == null || client.level == null) {
			return;
		}

		spawnClientWhiteParticles(client, 18, 0.75D);
	}

	public static void startReturnEffect(int durationTicks) {
		Minecraft client = Minecraft.getInstance();
		returnEffectTicks = Math.max(1, durationTicks);
		maxReturnEffectTicks = returnEffectTicks;

		if (client.player == null || client.level == null) {
			return;
		}

		spawnClientWhiteParticles(client, 28, 0.7D);
	}

	public static void tickEffects() {
		Minecraft client = Minecraft.getInstance();
		if (introFadeTicks > 0) {
			introFadeTicks--;
			if (introFadeTicks == 0) {
				playCutsceneSound(client);
			}
		} else if (cutsceneTicks > 0) {
			cutsceneTicks--;
		}

		if (introFadeTicks <= 0 && cutsceneTicks <= 0) {
			restoreCutsceneState(client);
		}

		if (returnEffectTicks > 0) {
			returnEffectTicks--;
		}
	}

	public static void renderOverlay(GuiGraphics graphics, float tickDelta) {
		if (introFadeTicks > 0) {
			renderIntroFade(graphics, tickDelta);
		} else if (cutsceneTicks > 0) {
			renderCutscene(graphics, tickDelta);
		}

		if (returnEffectTicks > 0) {
			renderReturnOverlay(graphics, tickDelta);
		}
	}

	public static boolean hasOverlayToRender() {
		return introFadeTicks > 0 || cutsceneTicks > 0 || returnEffectTicks > 0;
	}

	private static void renderIntroFade(GuiGraphics graphics, float tickDelta) {
		float remaining = Math.max(0.0F, introFadeTicks - tickDelta);
		float progress = 1.0F - Mth.clamp(remaining / maxIntroFadeTicks, 0.0F, 1.0F);
		int alpha = (int) (255.0F * progress);
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
	}

	private static void renderCutscene(GuiGraphics graphics, float tickDelta) {
		float remaining = Math.max(0.0F, cutsceneTicks - tickDelta);
		float progress = 1.0F - Mth.clamp(remaining / maxCutsceneTicks, 0.0F, 1.0F);
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		ResourceLocation frame = currentCutsceneFrame(progress);
		if (hasTexture(frame)) {
			drawCutsceneFrame(graphics, frame, width, height);
		} else {
			drawProceduralCutscene(graphics, width, height, progress);
		}
	}

	private static void renderReturnOverlay(GuiGraphics graphics, float tickDelta) {
		float progress = Mth.clamp((returnEffectTicks - tickDelta) / maxReturnEffectTicks, 0.0F, 1.0F);
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int alpha = (int) (210.0F * progress);

		graphics.fill(0, 0, width, height, alpha << 24 | 0xFFFFFF);

	}

	private static ResourceLocation currentCutsceneFrame(float progress) {
		int frame = Mth.clamp((int) (progress * cutsceneFrameCount), 0, cutsceneFrameCount - 1);
		String path = String.format(Locale.ROOT, "textures/cutscene/heart_grasp_%04d.png", frame);
		return new ResourceLocation(ReviveByDeath.MOD_ID, path);
	}

	private static boolean hasTexture(ResourceLocation texture) {
		return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
	}

	private static void drawCutsceneFrame(GuiGraphics graphics, ResourceLocation texture, int width, int height) {
		int targetWidth = width;
		int targetHeight = width * 9 / 16;
		if (targetHeight > height) {
			targetHeight = height;
			targetWidth = height * 16 / 9;
		}

		int x = (width - targetWidth) / 2;
		int y = (height - targetHeight) / 2;
		graphics.fill(0, 0, width, height, 0xFF000000);
		graphics.blit(texture, x, y, targetWidth, targetHeight, 0.0F, 0.0F, CUTSCENE_TEXTURE_WIDTH, CUTSCENE_TEXTURE_HEIGHT, CUTSCENE_TEXTURE_WIDTH, CUTSCENE_TEXTURE_HEIGHT);
	}

	private static void drawProceduralCutscene(GuiGraphics graphics, int width, int height, float progress) {
		int darkAlpha = (int) (205.0F + 35.0F * Mth.sin(progress * Mth.PI));
		graphics.fill(0, 0, width, height, darkAlpha << 24 | 0x060309);

		drawVignette(graphics, width, height, progress);
		drawShadowHands(graphics, width, height, progress);
		drawHeartCore(graphics, width, height, progress);
		drawGlitch(graphics, width, height, progress);
	}

	private static void drawVignette(GuiGraphics graphics, int width, int height, float progress) {
		int alpha = (int) (165.0F + 60.0F * progress);
		int color = alpha << 24;
		int edge = Math.max(20, Math.min(width, height) / 7);
		graphics.fill(0, 0, width, edge, color);
		graphics.fill(0, height - edge, width, height, color);
		graphics.fill(0, 0, edge, height, color);
		graphics.fill(width - edge, 0, width, height, color);
	}

	private static void drawShadowHands(GuiGraphics graphics, int width, int height, float progress) {
		float squeeze = Mth.clamp((progress - 0.12F) / 0.56F, 0.0F, 1.0F);
		squeeze = 1.0F - (1.0F - squeeze) * (1.0F - squeeze);
		int centerX = width / 2;
		int centerY = height / 2;
		int reach = (int) (width * 0.34F * squeeze);
		int fingerHeight = Math.max(9, height / 34);
		int palmHeight = Math.max(32, height / 8);
		int black = 0xEA000000;

		graphics.fill(0, centerY - palmHeight / 2, reach, centerY + palmHeight / 2, black);
		graphics.fill(width - reach, centerY - palmHeight / 2, width, centerY + palmHeight / 2, black);

		for (int i = 0; i < 5; i++) {
			int y = centerY - palmHeight / 2 - fingerHeight * 2 + i * fingerHeight;
			int length = reach + (int) (Math.sin((progress * 9.0F + i) * 0.7F) * 9.0F);
			graphics.fill(0, y, length + width / 18, y + fingerHeight, black);
			graphics.fill(width - length - width / 18, y, width, y + fingerHeight, black);
		}

		int claw = (int) (42.0F * squeeze);
		graphics.fill(centerX - claw - 5, centerY - palmHeight, centerX - claw, centerY + palmHeight, 0xD8000000);
		graphics.fill(centerX + claw, centerY - palmHeight, centerX + claw + 5, centerY + palmHeight, 0xD8000000);
	}

	private static void drawHeartCore(GuiGraphics graphics, int width, int height, float progress) {
		int centerX = width / 2;
		int centerY = height / 2;
		float pulse = 0.5F + 0.5F * Mth.sin(progress * Mth.PI * 10.0F);
		float crush = Mth.clamp((progress - 0.42F) / 0.38F, 0.0F, 1.0F);
		int size = (int) ((26.0F + pulse * 11.0F) * (1.0F - crush * 0.45F));
		int glow = (int) (120.0F * (1.0F - crush * 0.35F));

		graphics.fill(centerX - size - 10, centerY - size - 10, centerX + size + 10, centerY + size + 10, glow << 24 | 0x7A0614);
		graphics.fill(centerX - size / 2, centerY - size, centerX + size / 2, centerY + size, 0xF0C0162E);
		graphics.fill(centerX - size, centerY - size / 3, centerX + size, centerY + size / 2, 0xF0A50E25);
		graphics.fill(centerX - size / 3, centerY + size / 2, centerX + size / 3, centerY + size, 0xF0880A1E);

		if (progress > 0.68F) {
			int flashAlpha = (int) (180.0F * Mth.clamp((progress - 0.68F) / 0.12F, 0.0F, 1.0F));
			graphics.fill(0, 0, width, height, flashAlpha << 24 | 0x210007);
		}
	}

	private static void drawGlitch(GuiGraphics graphics, int width, int height, float progress) {
		if (progress < 0.18F) {
			return;
		}

		int bars = 4 + (int) (progress * 6.0F);
		for (int i = 0; i < bars; i++) {
			int y = Math.floorMod((cutsceneTicks * 37 + i * 53), Math.max(1, height));
			int h = 2 + Math.floorMod(cutsceneTicks + i * 7, 7);
			int offset = Math.floorMod(cutsceneTicks * 11 + i * 17, 34) - 17;
			int alpha = 38 + Math.floorMod(i * 31 + cutsceneTicks * 3, 80);
			graphics.fill(Math.max(0, offset), y, Math.min(width, width + offset), Math.min(height, y + h), alpha << 24 | 0xDFFCF8);
		}
	}

	private static void spawnClientWhiteParticles(Minecraft client, int particles, double radiusScale) {
		for (int i = 0; i < particles; i++) {
			double angle = (Math.PI * 2.0D * i) / particles;
			double radius = radiusScale * (0.55D + client.level.random.nextDouble() * 0.75D);
			double x = client.player.getX() + Math.cos(angle) * radius;
			double y = client.player.getY() + 0.15D + client.level.random.nextDouble() * 1.75D;
			double z = client.player.getZ() + Math.sin(angle) * radius;
			double vx = (client.player.getX() - x) * 0.065D;
			double vz = (client.player.getZ() - z) * 0.065D;
			client.level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, 0.018D, vz);
		}

	}

	private static void forceHideGui(Minecraft client) {
		if (forcedHideGui) {
			return;
		}

		previousHideGui = client.options.hideGui;
		client.options.hideGui = true;
		forcedHideGui = true;
	}

	private static void restoreGui(Minecraft client) {
		if (!forcedHideGui) {
			return;
		}

		client.options.hideGui = previousHideGui;
		forcedHideGui = false;
	}

	private static void muteEnvironment(Minecraft client) {
		if (mutedCutsceneAudio) {
			return;
		}

		previousSourceVolumes.clear();
		for (SoundSource source : CUTSCENE_MUTED_SOURCES) {
			previousSourceVolumes.put(source, client.options.getSoundSourceVolume(source));
			client.getSoundManager().stop(null, source);
			client.getSoundManager().updateSourceVolume(source, 0.0F);
		}
		mutedCutsceneAudio = true;
	}

	private static void restoreEnvironmentAudio(Minecraft client) {
		if (!mutedCutsceneAudio) {
			return;
		}

		for (Map.Entry<SoundSource, Float> entry : previousSourceVolumes.entrySet()) {
			client.getSoundManager().updateSourceVolume(entry.getKey(), entry.getValue());
		}
		previousSourceVolumes.clear();
		mutedCutsceneAudio = false;
	}

	private static void playCutsceneSound(Minecraft client) {
		if (cutsceneSoundPlayed) {
			return;
		}

		client.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CUTSCENE_HEARTBEAT.get(), 1.0F, 1.0F));
		cutsceneSoundPlayed = true;
	}

	public static void onDisconnect() {
		Minecraft client = Minecraft.getInstance();
		introFadeTicks = 0;
		cutsceneTicks = 0;
		returnEffectTicks = 0;
		restoreCutsceneState(client);
	}

	private static void restoreCutsceneState(Minecraft client) {
		restoreGui(client);
		restoreEnvironmentAudio(client);
	}
}
