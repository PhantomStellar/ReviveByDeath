package com.khanghy.revivebydeath.registry;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ReviveByDeath.MOD_ID);
	public static final RegistryObject<SoundEvent> CUTSCENE_HEARTBEAT = register("cutscene_heartbeat");
	public static final RegistryObject<SoundEvent> RETURN_WHOOSH = register("return_whoosh");

	private ModSounds() {
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
		ReviveByDeath.LOGGER.debug("Registered ReviveByDeath sound events.");
	}

	private static RegistryObject<SoundEvent> register(String path) {
		ResourceLocation id = new ResourceLocation(ReviveByDeath.MOD_ID, path);
		return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
	}
}
