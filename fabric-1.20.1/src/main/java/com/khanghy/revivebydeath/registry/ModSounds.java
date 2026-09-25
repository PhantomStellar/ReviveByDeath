package com.khanghy.revivebydeath.registry;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent CUTSCENE_HEARTBEAT = register("cutscene_heartbeat");
	public static final SoundEvent RETURN_WHOOSH = register("return_whoosh");

	private ModSounds() {
	}

	public static void register() {
		ReviveByDeath.LOGGER.debug("Registered ReviveByDeath sound events.");
	}

	private static SoundEvent register(String path) {
		ResourceLocation id = new ResourceLocation(ReviveByDeath.MOD_ID, path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
