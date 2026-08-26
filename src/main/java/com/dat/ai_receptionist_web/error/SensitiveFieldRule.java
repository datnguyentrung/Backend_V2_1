package com.dat.ai_receptionist_web.error;

import java.util.List;
import java.util.Locale;

public final class SensitiveFieldRule {
    private static final List<String> SENSITIVE_TOKENS = List.of(
            "password", "token", "accesstoken", "refreshtoken", "otp", "pin",
            "credential", "secret", "authorization", "card", "cvv", "balance", "amount"
    );

    private SensitiveFieldRule() {
    }

    public static boolean isSensitive(String field) {
        if (field == null || field.isBlank()) {
            return true;
        }
        String normalized = field.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(normalized::contains);
    }
}
