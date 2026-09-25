package com.khanghy.revivebydeath.registry;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
	public static final ResourceKey<Enchantment> DEATH_REWIND = ResourceKey.create(
			Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(ReviveByDeath.MOD_ID, "death_rewind"));

	private ModEnchantments() {
	}

	public static void register() {
		ReviveByDeath.LOGGER.debug("Prepared ReviveByDeath enchantment keys.");
	}
}
