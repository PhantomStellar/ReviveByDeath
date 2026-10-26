package com.khanghy.revivebydeath.server;

public record ReturnEffect(int totalTicks, int remainingTicks) {
	public ReturnEffect withRemainingTicks(int ticks) {
		return new ReturnEffect(totalTicks, ticks);
	}
}
