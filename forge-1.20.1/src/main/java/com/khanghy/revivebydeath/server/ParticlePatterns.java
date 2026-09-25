package com.khanghy.revivebydeath.server;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class ParticlePatterns {
	private ParticlePatterns() {
	}

	public static void deathVortex(ServerLevel level, Entity entity, int ageTicks, int totalTicks) {
		double progress = ageTicks / (double) Math.max(1, totalTicks);
		double baseY = entity.getY() + 0.25D;
		double radius = 1.4D - progress * 0.85D;
		double spin = progress * Math.PI * 7.0D;

		for (int i = 0; i < 10; i++) {
			double local = i / 10.0D;
			double angle = spin + local * Math.PI * 2.0D;
			double y = baseY + local * 1.65D;
			double x = entity.getX() + Math.cos(angle) * radius * (1.0D - local * 0.25D);
			double z = entity.getZ() + Math.sin(angle) * radius * (1.0D - local * 0.25D);
			double vx = (entity.getX() - x) * 0.045D;
			double vz = (entity.getZ() - z) * 0.045D;
			level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, vx, 0.01D, vz, 0.025D);
		}

		if (ageTicks % 8 == 0) {
			ring(level, ParticleTypes.POOF, entity.getX(), entity.getY() + 1.0D, entity.getZ(), radius, 24, 0.018D);
		}
	}

	public static void returnBurst(ServerLevel level, Entity entity) {
		ring(level, ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.06D, entity.getZ(), 0.55D, 28, 0.015D);
		level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.45D, entity.getZ(), 18, 0.28D, 0.25D, 0.28D, 0.012D);
	}

	public static void returnPulse(ServerLevel level, Entity entity, int ageTicks, int totalTicks) {
		if (ageTicks % 2 != 0) {
			return;
		}

		double progress = ageTicks / (double) Math.max(1, totalTicks);
		double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
		double radius = 0.35D + eased * 2.15D;
		double y = entity.getY() + 0.05D;
		int points = 28;

		ring(level, ParticleTypes.END_ROD, entity.getX(), y, entity.getZ(), radius, points, 0.012D);

		if (ageTicks % 6 == 0) {
			double innerRadius = Math.max(0.2D, radius * 0.55D);
			ring(level, ParticleTypes.POOF, entity.getX(), y + 0.08D, entity.getZ(), innerRadius, 16, 0.02D);
		}
	}

	private static void ring(ServerLevel level, ParticleOptions particle, double x, double y, double z, double radius, int points, double speed) {
		for (int i = 0; i < points; i++) {
			double angle = (Math.PI * 2.0D * i) / points;
			double px = x + Math.cos(angle) * radius;
			double pz = z + Math.sin(angle) * radius;
			double vx = Math.cos(angle) * speed;
			double vz = Math.sin(angle) * speed;
			level.sendParticles(particle, px, y, pz, 1, vx, 0.0D, vz, speed);
		}
	}
}
