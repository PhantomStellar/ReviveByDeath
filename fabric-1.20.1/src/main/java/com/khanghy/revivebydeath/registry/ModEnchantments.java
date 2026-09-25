package com.khanghy.revivebydeath.registry;

import com.khanghy.revivebydeath.ReviveByDeath;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ModEnchantments {
	public static final Enchantment DEATH_REWIND = new Enchantment(
			Enchantment.Rarity.VERY_RARE,
			EnchantmentCategory.ARMOR,
			new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}
	) {
		@Override
		public int getMinCost(int level) {
			return 15;
		}

		@Override
		public int getMaxCost(int level) {
			return 50;
		}

		@Override
		public int getMaxLevel() {
			return 1;
		}

		@Override
		public boolean isTreasureOnly() {
			return true;
		}
	};

	private ModEnchantments() {
	}

	public static void register() {
		Registry.register(
				BuiltInRegistries.ENCHANTMENT,
				new ResourceLocation(ReviveByDeath.MOD_ID, "death_rewind"),
				DEATH_REWIND
		);
		ReviveByDeath.LOGGER.debug("Registered ReviveByDeath enchantment.");
	}
}
