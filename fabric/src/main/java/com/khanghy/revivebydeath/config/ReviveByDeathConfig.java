package com.khanghy.revivebydeath.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khanghy.revivebydeath.ReviveByDeath;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import net.fabricmc.loader.api.FabricLoader;

public final class ReviveByDeathConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("revivebydeath.json");
	private static ReviveByDeathConfig INSTANCE;
	private static FileTime lastLoadedModifiedTime;

	public String activationMode = ActivationMode.ENCHANTMENT.configName();
	public String revivePriority = RevivePriority.DOWNED_FIRST.configName();
	public boolean replaceVanillaTotem = true;
	public boolean consumeTotem = true;
	public int enchantmentArmorDamage = 35;
	public boolean useXpCost = true;
	public int minimumXpLevelCost = 6;
	public int xpLevelCostIncrease = 2;
	public double xpLevelCostMultiplier = 1.18D;
	public boolean resetCostOnSleep = true;
	public boolean resetCostOnDeath = true;
	public boolean enableCutscene = true;
	public int cutsceneIntroFadeTicks = 4;
	public int cutsceneTicks = 30;
	public int cutsceneFrames = 52;
	public int returnEffectTicks = 14;
	public int postReviveInvulnerabilityTicks = 60;
	public int checkpointInitialCheckTicks = 10;
	public int checkpointRecheckTicks = 6000;
	public boolean spawnPastShadow = true;
	public boolean createDimensionalRift = true;

	public static ReviveByDeathConfig get() {
		if (INSTANCE == null || hasFileChanged()) {
			INSTANCE = load();
		}

		return INSTANCE;
	}

	public static void setActivationMode(ActivationMode mode) {
		ReviveByDeathConfig config = get();
		config.activationMode = mode.configName();
		config.save(PATH);
	}

	public static void setCutsceneEnabled(boolean enabled) {
		ReviveByDeathConfig config = get();
		config.enableCutscene = enabled;
		config.save(PATH);
	}

	public ActivationMode activationMode() {
		return ActivationMode.fromConfig(activationMode);
	}

	public RevivePriority revivePriority() {
		return RevivePriority.fromConfig(revivePriority);
	}

	private static ReviveByDeathConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				ReviveByDeathConfig config = GSON.fromJson(reader, ReviveByDeathConfig.class);
				if (config != null) {
					config.save(PATH);
					return config;
				}
			} catch (IOException exception) {
				ReviveByDeath.LOGGER.warn("Could not read revivebydeath.json. Using defaults.", exception);
			}
		}

		ReviveByDeathConfig config = new ReviveByDeathConfig();
		config.save(PATH);
		return config;
	}

	private void save(Path path) {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
			lastLoadedModifiedTime = Files.getLastModifiedTime(path);
		} catch (IOException exception) {
			ReviveByDeath.LOGGER.warn("Could not write revivebydeath.json.", exception);
		}
	}

	private static boolean hasFileChanged() {
		if (!Files.exists(PATH)) {
			return false;
		}

		try {
			FileTime modifiedTime = Files.getLastModifiedTime(PATH);
			return lastLoadedModifiedTime == null || !modifiedTime.equals(lastLoadedModifiedTime);
		} catch (IOException exception) {
			return false;
		}
	}
}
