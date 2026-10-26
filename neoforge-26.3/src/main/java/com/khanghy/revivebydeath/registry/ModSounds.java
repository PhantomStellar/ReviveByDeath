package com.khanghy.revivebydeath.registry;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ReviveByDeath.MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> CUTSCENE_HEARTBEAT = register("cutscene_heartbeat");
	public static final DeferredHolder<SoundEvent, SoundEvent> RETURN_WHOOSH = register("return_whoosh");

	private ModSounds() {
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
		ReviveByDeath.LOGGER.debug("Registered ReviveByDeath sound events.");
	}

	private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(ReviveByDeath.MOD_ID, path);
		return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
	}
}
