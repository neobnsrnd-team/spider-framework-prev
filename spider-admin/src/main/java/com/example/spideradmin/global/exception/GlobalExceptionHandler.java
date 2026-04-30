package com.example.spideradmin.global.exception;

import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.exception.base.BaseException;
import com.example.spideradmin.global.log.event.ErrorLogEvent;
import com.example.spideradmin.global.util.SecurityUtil;
import com.example.spideradmin.global.util.StringUtil;
import com.example.spideradmin.global.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 핸들러.
 *
 * <p>로깅은 직접 수행하지 않고 {@link ErrorLogEvent}를 발행하여
 * {@code Slf4jLogListener}, {@code RdbErrorLogListener}에 위임합니다.</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_STACK_TRACE_LENGTH = 4000;

    private static final Map<String, String> CONSTRAINT_MESSAGE_MAP = Map.of(
            "PK_FWK_MONITOR", "이미 존재하는 모니터ID입니다. 다른 ID를 입력해주세요.",
            "PK_FWK_USER", "이미 존재하는 사용자ID입니다. 다른 ID를 입력해주세요.",
            "PK_FWK_ROLE", "이미 존재하는 역할ID입니다. 다른 ID를 입력해주세요.",
            "PK_FWK_MENU", "이미 존재하는 메뉴ID입니다. 다른 ID를 입력해주세요.");

    private final ApplicationEventPublisher eventPublisher;

    // ==================== 카테고리 예외 (4개) ====================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return buildResponse(ex);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateException(
            DuplicateException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return buildResponse(ex);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInputException(
            InvalidInputException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return buildResponse(ex);
    }

    @ExceptionHandler(InternalException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalException(InternalException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return buildResponse(ex);
    }

    // ==================== BaseException fallback ====================

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return buildResponse(ex);
    }

    // ==================== Spring / DB 예외 ====================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        // JSON 파싱 실패(잘못된 Enum 값, 타입 불일치 등) → 클라이언트 오류
        publishErrorEvent(ex, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("요청 본문을 읽을 수 없습니다. 입력값을 확인해주세요.", 400));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("필수 파라미터가 누락되었습니다: " + ex.getParameterName(), 400));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        publishErrorEvent(ex, request);

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값 검증 실패");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message, 400));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(
            DuplicateKeyException ex, HttpServletRequest request) {

        String errorMessage = "이미 존재하는 데이터입니다.";
        String exMessage = ex.getMessage();

        if (exMessage != null) {
            errorMessage = CONSTRAINT_MESSAGE_MAP.entrySet().stream()
                    .filter(entry -> exMessage.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(errorMessage);
        }

        request.setAttribute("log.errorMessage", errorMessage);
        publishErrorEvent(ex, request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(errorMessage, 409));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        publishErrorEvent(ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("데이터 처리 중 오류가 발생했습니다.", 500));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) throws NoResourceFoundException {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("요청하신 리소스를 찾을 수 없습니다.", 404));
        }
        throw ex;
    }

    // ==================== 최종 fallback ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request)
            throws AccessDeniedException, AuthenticationException {
        // Spring Security 예외는 시큐리티 필터 체인(AccessDeniedHandler, AuthenticationEntryPoint)에 위임
        if (ex instanceof AccessDeniedException ade) throw ade;
        if (ex instanceof AuthenticationException ae) throw ae;

        publishErrorEvent(ex, request);

        if (!request.getRequestURI().startsWith("/api/")) {
            throw new RuntimeException(ex);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("예상치 못한 오류가 발생했습니다", 500));
    }

    // ==================== 내부 헬퍼 ====================

    private ResponseEntity<ApiResponse<Void>> buildResponse(BaseException ex) {
        ErrorType errorType = ex.getErrorType();
        HttpStatus status = errorType.getHttpStatus();
        // detailMessage가 있으면 우선 사용 — 비즈니스 맥락에 맞는 안내 메시지 전달
        String message = ex.getDetailMessage() != null ? ex.getDetailMessage() : errorType.getMessage();
        return ResponseEntity.status(status).body(ApiResponse.error(message, status.value()));
    }

    private void publishErrorEvent(Exception ex, HttpServletRequest request) {
        String errorCode;
        String errorMessage = ex.getMessage();
        LogLevel logLevel = LogLevel.ERROR;

        if (ex instanceof BaseException base) {
            errorCode = base.getErrorType().getCode();
            logLevel = base.getErrorType().getLogLevel();
            errorMessage = base.getDetailMessage() != null
                    ? base.getDetailMessage()
                    : base.getErrorType().getMessage();
        } else {
            errorCode = "UNHANDLED";
        }

        request.setAttribute("log.errorMessage", errorMessage);

        try {
            ErrorLogEvent event = new ErrorLogEvent(
                    TraceIdUtil.get(),
                    errorCode,
                    errorMessage,
                    StringUtil.truncateStackTrace(ex, MAX_STACK_TRACE_LENGTH),
                    SecurityUtil.getCurrentUserIdOrAnonymous(),
                    request.getRequestURI(),
                    request.getMethod(),
                    request.getRemoteAddr(),
                    LocalDateTime.now().format(FORMATTER),
                    logLevel);
            eventPublisher.publishEvent(event);
        } catch (Exception ignored) {
            // 로깅이 비즈니스 흐름을 방해하면 안 됨
        }
    }
}
