package com.example.spideradmin.global.util;

import java.util.Set;

/**
 * Interface Management 도메인 Validation Utils
 */
public final class ValidationUtils {

    private static final Set<String> IO_TYPES = Set.of("I", "O", "Q", "S");
    private static final Set<String> IO_TYPES_ADAPTER_LISTENER = Set.of("I", "O");
    private static final Set<String> OPER_MODE_TYPES = Set.of("D", "T", "R");

    private ValidationUtils() {}

    public static boolean isValidIoType(String code) {
        return code != null && IO_TYPES.contains(code);
    }

    public static boolean isValidAdapterListenerIoType(String code) {
        return code != null && IO_TYPES_ADAPTER_LISTENER.contains(code);
    }

    public static boolean isValidOperModeType(String code) {
        return code != null && OPER_MODE_TYPES.contains(code);
    }

    public static boolean isValidTimeHHmm(String value) {
        if (value == null || value.length() != 4) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        int hh = Integer.parseInt(value.substring(0, 2));
        int mm = Integer.parseInt(value.substring(2, 4));
        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }

    public static boolean isValidIpv4(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (!isValidIpv4Part(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIpv4Part(String part) {
        if (part.isEmpty() || part.length() > 3) {
            return false;
        }
        for (char c : part.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        int num = Integer.parseInt(part);
        return num >= 0 && num <= 255;
    }

    public static boolean isValidPort(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        int port = Integer.parseInt(value);
        return port >= 1 && port <= 65535;
    }
}
