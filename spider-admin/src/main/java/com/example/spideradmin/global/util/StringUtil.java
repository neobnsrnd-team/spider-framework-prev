package com.example.spideradmin.global.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class StringUtil {

    private static final String DEFAULT_TRUNCATED_MARKER = "...(truncated)";

    private StringUtil() {}

    /**
     * UTF-8 바이트 기준으로 문자열을 절단한다. 마커 없이 깔끔하게 자른다.
     */
    public static String truncateBytes(String value, int maxBytes) {
        if (value == null || maxBytes <= 0) {
            return value;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        return truncateToByteLimit(value, maxBytes);
    }

    /**
     * UTF-8 바이트 기준으로 문자열을 절단하고 절단 마커를 붙인다.
     */
    public static String truncateBytesWithMarker(String value, int maxBytes) {
        return truncateBytesWithMarker(value, maxBytes, DEFAULT_TRUNCATED_MARKER);
    }

    /**
     * UTF-8 바이트 기준으로 문자열을 절단하고 지정된 마커를 붙인다.
     */
    public static String truncateBytesWithMarker(String value, int maxBytes, String marker) {
        if (value == null || maxBytes <= 0) {
            return value;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        int markerLen = marker.getBytes(StandardCharsets.UTF_8).length;
        int targetBytes = Math.max(0, maxBytes - markerLen);
        return truncateToByteLimit(value, targetBytes) + marker;
    }

    /**
     * 예외의 스택 트레이스를 문자열로 변환하고 바이트 기준으로 절단한다.
     */
    public static String truncateStackTrace(Exception ex, int maxBytes) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return truncateBytes(sw.toString(), maxBytes);
    }

    private static String truncateToByteLimit(String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        String truncated = new String(bytes, 0, Math.min(bytes.length, maxBytes), StandardCharsets.UTF_8);
        // UTF-8 멀티바이트 문자 경계 보정
        while (!truncated.isEmpty() && truncated.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated;
    }
}
