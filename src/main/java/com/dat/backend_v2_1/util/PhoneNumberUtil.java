package com.dat.backend_v2_1.util;

import java.util.regex.Pattern;

public final class PhoneNumberUtil {
    private static final Pattern VIETNAM_MOBILE = Pattern.compile(
            "^0(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])\\d{7}$"
    );

    private PhoneNumberUtil() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.trim().replaceAll("[\\s.\\-()]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!VIETNAM_MOBILE.matcher(digits).matches()) {
            throw new IllegalArgumentException("Invalid Vietnamese phone number");
        }
        return digits;
    }
}
