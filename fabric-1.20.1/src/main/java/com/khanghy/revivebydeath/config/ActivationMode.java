package com.khanghy.revivebydeath.config;

import java.util.Locale;

public enum ActivationMode {
	ALWAYS,
	TOTEM,
	ENCHANTMENT,
	BOTH;

	public static ActivationMode fromConfig(String value) {
		for (ActivationMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value)) {
				return mode;
			}
		}

		return ENCHANTMENT;
	}

	public String configName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
