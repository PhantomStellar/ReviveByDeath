package com.khanghy.revivebydeath.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
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
	public static final Codec<Checkpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(Checkpoint::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(Checkpoint::x),
			Codec.DOUBLE.fieldOf("y").forGetter(Checkpoint::y),
			Codec.DOUBLE.fieldOf("z").forGetter(Checkpoint::z),
			Codec.FLOAT.fieldOf("yaw").forGetter(Checkpoint::yaw),
			Codec.FLOAT.fieldOf("pitch").forGetter(Checkpoint::pitch),
			Codec.LONG.fieldOf("tick").forGetter(Checkpoint::savedAtTick)
	).apply(instance, Checkpoint::new));
}
