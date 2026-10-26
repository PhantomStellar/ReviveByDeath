package com.khanghy.revivebydeath.command;

import com.khanghy.revivebydeath.config.ActivationMode;
import com.khanghy.revivebydeath.config.ReviveByDeathConfig;
import com.khanghy.revivebydeath.server.ActivationResult;
import com.khanghy.revivebydeath.server.ActivationRules;
import com.khanghy.revivebydeath.server.CheckpointManager;
import com.khanghy.revivebydeath.server.CheckpointSavedData;
import com.khanghy.revivebydeath.server.DebugLogManager;
import com.khanghy.revivebydeath.server.RewindCostManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ModCommands {
	private ModCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("revivebydeath")
						.then(Commands.literal("status")
								.executes(context -> status(context.getSource().getPlayerOrException())))
						.then(Commands.literal("log")
								.then(Commands.literal("on")
										.executes(context -> setDebugLog(true, context.getSource().getPlayerOrException())))
								.then(Commands.literal("off")
										.executes(context -> setDebugLog(false, context.getSource().getPlayerOrException()))))
						.then(Commands.literal("give_book")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.executes(context -> giveBook(context.getSource().getPlayerOrException())))
						.then(Commands.literal("reset_cost")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.executes(context -> resetCost(context.getSource().getPlayerOrException()))
								.then(Commands.argument("targets", EntityArgument.players())
										.executes(context -> resetCost(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
						.then(Commands.literal("mode")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.literal("always")
										.executes(context -> setMode(ActivationMode.ALWAYS, context.getSource().getPlayerOrException())))
								.then(Commands.literal("totem")
										.executes(context -> setMode(ActivationMode.TOTEM, context.getSource().getPlayerOrException())))
								.then(Commands.literal("enchantment")
										.executes(context -> setMode(ActivationMode.ENCHANTMENT, context.getSource().getPlayerOrException())))
								.then(Commands.literal("both")
										.executes(context -> setMode(ActivationMode.BOTH, context.getSource().getPlayerOrException()))))
						.then(Commands.literal("cutscene")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.literal("on")
										.executes(context -> setCutsceneEnabled(true, context.getSource().getPlayerOrException())))
								.then(Commands.literal("off")
										.executes(context -> setCutsceneEnabled(false, context.getSource().getPlayerOrException()))))
						.then(Commands.literal("revert_all_rifts")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.executes(context -> revertAllRifts(context.getSource()))));
	}

	private static int revertAllRifts(CommandSourceStack source) {
		int count = CheckpointSavedData.get(source.getServer()).revertAllRifts(source.getServer());
		source.sendSuccess(() -> Component.translatable("message.revivebydeath.revert_all_rifts_success", count), true);
		return count;
	}

	private static int giveBook(ServerPlayer player) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(ActivationRules.deathRewindHolder(player), 1);

		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
		player.getInventory().placeItemBackInInventory(book, Prediction.PREDICTED);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.gave_book"));
		return 1;
	}

	private static int setMode(ActivationMode mode, ServerPlayer player) {
		ReviveByDeathConfig.setActivationMode(mode);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.mode_set", mode.configName()));
		return 1;
	}

	private static int setCutsceneEnabled(boolean enabled, ServerPlayer player) {
		ReviveByDeathConfig.setCutsceneEnabled(enabled);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.cutscene_set", enabled ? "enabled" : "disabled"));
		return 1;
	}

	private static int setDebugLog(boolean enabled, ServerPlayer player) {
		DebugLogManager.setEnabled(player, enabled);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.debug_log_set", enabled ? "enabled" : "disabled"));
		return 1;
	}

	private static int status(ServerPlayer player) {
		ReviveByDeathConfig config = ReviveByDeathConfig.get();
		player.sendSystemMessage(Component.literal("ReviveByDeath mode: " + config.activationMode().configName()));
		player.sendSystemMessage(Component.translatable("message.revivebydeath.revive_priority_status",
				Component.translatable("option.revivebydeath.revive_priority." + config.revivePriority().configName())));
		player.sendSystemMessage(Component.literal("Cutscene: " + (config.enableCutscene ? "enabled" : "disabled")));
		player.sendSystemMessage(Component.literal("Debug log: " + (DebugLogManager.isEnabled(player) ? "enabled" : "disabled")));

		ActivationResult activation = ActivationRules.evaluate(player);
		player.sendSystemMessage(Component.literal("Checkpoint: " + (CheckpointManager.hasUsableCheckpoint(player) ? "ready" : "not ready")));
		player.sendSystemMessage(Component.literal("Activation: " + (activation.allowed() ? "ready" : "blocked - " + activation.failureReason())));
		player.sendSystemMessage(RewindCostManager.statusLine(player));
		return 1;
	}

	private static int resetCost(ServerPlayer player) {
		RewindCostManager.reset(player);
		player.sendSystemMessage(Component.translatable("message.revivebydeath.cost_reset"));
		return 1;
	}

	private static int resetCost(CommandSourceStack source, java.util.Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			RewindCostManager.reset(player);
			player.sendSystemMessage(Component.translatable("message.revivebydeath.cost_reset_admin"));
		}
		source.sendSuccess(() -> Component.translatable("message.revivebydeath.cost_reset_admin_success", targets.size()), true);
		return targets.size();
	}
}
