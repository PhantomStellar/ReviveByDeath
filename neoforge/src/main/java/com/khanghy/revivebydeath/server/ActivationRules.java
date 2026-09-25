package com.khanghy.revivebydeath.server;

import com.khanghy.revivebydeath.config.ActivationMode;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ActivationRules {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET
	};

	private ActivationRules() {
	}

	public static ActivationResult evaluate(net.minecraft.server.level.ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		return switch (config.activationMode()) {
			case ALWAYS -> withXpCost(player, ActivationResult.allow(() -> {
			}));
			case TOTEM -> evaluateTotem(player, config);
			case ENCHANTMENT -> withXpCost(player, evaluateEnchantment(player, config));
			case BOTH -> evaluateBoth(player, config);
		};
	}

	private static ActivationResult withXpCost(net.minecraft.server.level.ServerPlayer player, ActivationResult activation) {
		if (!activation.allowed()) {
			return activation;
		}

		RewindCostManager.CostCheck costCheck = RewindCostManager.check(player);
		if (!costCheck.allowed()) {
			return ActivationResult.deny("need " + costCheck.cost() + " XP levels");
		}

		return ActivationResult.allow(() -> {
			activation.consume().run();
			RewindCostManager.charge(player);
		});
	}

	public static boolean shouldSuppressVanillaTotem(net.minecraft.server.level.ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		return config.replaceVanillaTotem
				&& (config.activationMode() == ActivationMode.TOTEM || config.activationMode() == ActivationMode.BOTH)
				&& findTotemHand(player) != null;
	}

	private static ActivationResult evaluateBoth(net.minecraft.server.level.ServerPlayer player, ReviveByDeathConfig config) {
		ActivationResult enchantment = evaluateEnchantment(player, config);
		ActivationResult paidEnchantment = withXpCost(player, enchantment);
		if (paidEnchantment.allowed()) {
			return paidEnchantment;
		}

		ActivationResult totem = evaluateTotem(player, config);
		if (totem.allowed()) {
			return totem;
		}

		if (enchantment.allowed()) {
			return paidEnchantment;
		}

		return ActivationResult.deny("Death Rewind armor enchantment or totem required");
	}

	private static ActivationResult evaluateTotem(net.minecraft.server.level.ServerPlayer player, ReviveByDeathConfig config) {
		InteractionHand hand = findTotemHand(player);
		if (hand == null) {
			return ActivationResult.deny("totem required");
		}

		return ActivationResult.allow(() -> {
			if (!config.consumeTotem) {
				return;
			}

			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Items.TOTEM_OF_UNDYING)) {
				stack.shrink(1);
				if (stack.isEmpty()) {
					player.setItemInHand(hand, ItemStack.EMPTY);
				}
			}
		});
	}

	private static InteractionHand findTotemHand(net.minecraft.server.level.ServerPlayer player) {
		if (player.getItemInHand(InteractionHand.OFF_HAND).is(Items.TOTEM_OF_UNDYING)) {
			return InteractionHand.OFF_HAND;
		}

		if (player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.TOTEM_OF_UNDYING)) {
			return InteractionHand.MAIN_HAND;
		}

		return null;
	}

	private static ActivationResult evaluateEnchantment(net.minecraft.server.level.ServerPlayer player, ReviveByDeathConfig config) {
		Holder<Enchantment> enchantment = player.registryAccess()
				.registryOrThrow(Registries.ENCHANTMENT)
				.getHolderOrThrow(ModEnchantments.DEATH_REWIND);

		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!stack.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0) {
				return ActivationResult.allow(() -> {
					if (config.enchantmentArmorDamage > 0 && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
						stack.hurtAndBreak(config.enchantmentArmorDamage, level, player, item -> player.onEquippedItemBroken(item, slot));
					}
				});
			}
		}

		return ActivationResult.deny("Death Rewind armor enchantment required");
	}
}
