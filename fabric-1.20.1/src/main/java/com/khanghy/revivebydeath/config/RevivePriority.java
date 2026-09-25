package com.khanghy.revivebydeath.config;

import java.util.Locale;

public enum RevivePriority {
	REWIND_FIRST("rewind_first"),
	DOWNED_FIRST("downed_first");

	private final String configName;

	RevivePriority(String configName) {
		this.configName = configName;
	}

	public String configName() {
		return configName;
	}

	public static RevivePriority fromConfig(String value) {
		if (value != null && value.trim().toLowerCase(Locale.ROOT).equals(REWIND_FIRST.configName)) {
			return REWIND_FIRST;
		}
		return DOWNED_FIRST;
	}
}
