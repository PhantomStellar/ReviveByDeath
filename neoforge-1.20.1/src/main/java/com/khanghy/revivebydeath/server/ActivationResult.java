package com.khanghy.revivebydeath.server;

public record ActivationResult(boolean allowed, Runnable consume, String failureReason) {
	public static final ActivationResult DENY = deny("activation requirement not met");

	public static ActivationResult allow(Runnable consume) {
		return new ActivationResult(true, consume, "");
	}

	public static ActivationResult deny(String reason) {
		return new ActivationResult(false, () -> {
		}, reason);
	}
}
