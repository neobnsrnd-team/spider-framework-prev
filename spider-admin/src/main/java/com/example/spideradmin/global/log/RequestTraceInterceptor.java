package com.example.spideradmin.global.log;

import com.example.spideradmin.global.log.event.AccessLogEvent;
import com.example.spideradmin.global.util.SecurityUtil;
import com.example.spideradmin.global.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class RequestTraceInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "apiStartTime";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = TraceIdUtil.init();
        request.setAttribute("traceId", traceId);
        request.setAttribute(START_TIME, System.currentTimeMillis());

        if (handler instanceof HandlerMethod) {
            try {
                String userId = SecurityUtil.getCurrentUserIdOrAnonymous();
                String now = LocalDateTime.now().format(FORMATTER);

                AccessLogEvent event = new AccessLogEvent(
                        traceId,
                        "REQ",
                        request.getMethod(),
                        request.getRequestURI(),
                        userId,
                        request.getRemoteAddr(),
                        now,
                        extractData(request, request.getMethod(), "REQ"),
                        0,
                        -1,
                        null,
                        null);

                eventPublisher.publishEvent(event);
            } catch (Exception ignored) {
                // 로깅이 비즈니스 흐름을 방해하면 안 됨
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (handler instanceof HandlerMethod) {
                Long startTime = (Long) request.getAttribute(START_TIME);
                long durationMs = (startTime != null) ? System.currentTimeMillis() - startTime : -1;
                int status = response.getStatus();
                boolean isError = status >= 400;

                String traceId = (String) request.getAttribute("traceId");
                String userId = SecurityUtil.getCurrentUserIdOrAnonymous();
                String now = LocalDateTime.now().format(FORMATTER);

                // 에러 메시지: GlobalExceptionHandler에서 설정한 attribute 우선
                String errorMessage = null;
                if (isError) {
                    Object attrErrorMessage = request.getAttribute("log.errorMessage");
                    if (attrErrorMessage != null) {
                        errorMessage = (String) attrErrorMessage;
                    } else if (ex != null) {
                        errorMessage = ex.getMessage();
                    }
                }

                AccessLogEvent event = new AccessLogEvent(
                        traceId,
                        "RES",
                        request.getMethod(),
                        request.getRequestURI(),
                        userId,
                        request.getRemoteAddr(),
                        now,
                        extractData(request, request.getMethod(), "RES"),
                        status,
                        durationMs,
                        isError ? "ERROR" : "SUCCESS",
                        errorMessage);

                eventPublisher.publishEvent(event);
            }
        } catch (Exception ignored) {
            // 로깅이 비즈니스 흐름을 방해하면 안 됨
        } finally {
            TraceIdUtil.clear();
        }
    }

    private String extractData(HttpServletRequest request, String httpMethod, String phase) {
        if ("GET".equalsIgnoreCase(httpMethod)) {
            String queryString = request.getQueryString();
            return queryString != null ? queryString : "";
        }

        if ("RES".equals(phase)) {
            ContentCachingRequestWrapper wrapper =
                    WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
            if (wrapper != null) {
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String body = new String(content, StandardCharsets.UTF_8);
                    return maskSensitiveData(body);
                }
            }
        }

        return "";
    }

    private String maskSensitiveData(String data) {
        if (data == null) {
            return null;
        }
        return data.replaceAll("(?i)\"(password|passwd|pwd|secret|token)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"****\"");
    }
}
