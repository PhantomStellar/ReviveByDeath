package com.khanghy.revivebydeath.server;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record Checkpoint(
		ResourceKey<Level> dimension,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		long savedAtTick) {
}
