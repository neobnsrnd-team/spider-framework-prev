package com.example.spider_admin.global.util;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdUtil {

    private static final String TRACE_ID = "traceId";

    private TraceIdUtil() {}

    public static String init() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    public static String get() {
        return MDC.get(TRACE_ID);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
