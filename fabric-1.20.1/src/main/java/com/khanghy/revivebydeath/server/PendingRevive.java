package com.khanghy.revivebydeath.server;

public record PendingRevive(
		Checkpoint checkpoint,
		double holdX,
		double holdY,
		double holdZ,
		float holdYaw,
		float holdPitch,
		int totalTicks,
		int remainingTicks) {
	public PendingRevive withRemainingTicks(int ticks) {
		return new PendingRevive(checkpoint, holdX, holdY, holdZ, holdYaw, holdPitch, totalTicks, ticks);
	}
}
