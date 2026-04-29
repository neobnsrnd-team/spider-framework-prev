package com.example.spideradmin.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AuditUtil {

    private AuditUtil() {}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String currentUserId() {
        return SecurityUtil.getCurrentUserIdOrSystem();
    }
}
