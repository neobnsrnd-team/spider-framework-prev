package com.example.bizchannel.web.controller;

import com.example.bizchannel.client.BizClient;
import com.example.bizcommon.BizCommands;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 REST 컨트롤러.
 *
 * <p>{@code /api/auth/*} 경로의 HTTP 요청을 처리한다.
 * 로그인·사용자 조회는 spider-link {@link BizClient} 를 통해 인증AP(biz-auth, TCP 19100) 에 위임하고,
 * JWT 액세스 토큰·리프레시 토큰의 발급·검증·폐기는 이 컨트롤러에서 직접 담당한다.</p>
 *
 * <ul>
 *   <li>액세스 토큰: Authorization 헤더로 전달, TTL 기본 30분</li>
 *   <li>리프레시 토큰: httpOnly 쿠키({@code refreshToken})로 전달, TTL 기본 7일, path=/api/auth</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BizClient bizClient;

    /** 액세스 토큰 서명 키 */
    private final SecretKey accessKey;

    /** 리프레시 토큰 서명 키 */
    private final SecretKey refreshKey;

    /** 액세스 토큰 유효 시간(ms) */
    private final long accessTokenTtlMs;

    /** 리프레시 토큰 유효 시간(ms) */
    private final long refreshTokenTtlMs;

    /**
     * 인메모리 리프레시 토큰 저장소.
     * key: userId, value: 발급된 리프레시 토큰 문자열
     * (실제 운영 환경에서는 Redis 등 외부 저장소로 교체 권장)
     */
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    /**
     * @param bizClient          TCP 클라이언트 래퍼
     * @param jwtSecret          액세스 토큰 서명 비밀 키 (application.yml: jwt.secret)
     * @param jwtRefreshSecret   리프레시 토큰 서명 비밀 키 (application.yml: jwt.refresh-secret)
     * @param accessExpiresIn    액세스 토큰 TTL(ms) (application.yml: jwt.access-expires-in)
     * @param refreshExpiresIn   리프레시 토큰 TTL(ms) (application.yml: jwt.refresh-expires-in)
     */
    public AuthController(
            BizClient bizClient,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.refresh-secret}") String jwtRefreshSecret,
            @Value("${jwt.access-expires-in:1800000}") long accessExpiresIn,
            @Value("${jwt.refresh-expires-in:604800000}") long refreshExpiresIn) {

        this.bizClient = bizClient;
        // 문자열 비밀 키를 UTF-8 바이트로 변환하여 HMAC 키 생성
        this.accessKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMs = accessExpiresIn;
        this.refreshTokenTtlMs = refreshExpiresIn;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 사용자 로그인 처리.
     *
     * <p>인증AP 에 AUTH_LOGIN 커맨드를 전송하고, 성공 시 JWT 액세스 토큰과
     * httpOnly 리프레시 토큰 쿠키를 발급한다.</p>
     *
     * @param body     요청 바디 ({@code userId}, {@code password})
     * @param response HttpServletResponse — 쿠키 설정에 사용
     * @return 로그인 결과 ({@code success, token, userId, userName, userGrade, lastLogin})
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String userId = (String) body.get("userId");
        String password = (String) body.get("password");
        // HttpLoggingInterceptor가 HTTP 수신 시 생성한 UUID — TCP 구간과 동일한 TRX_TRACKING_NO로 연결
        String requestId = (String) request.getAttribute("requestId");

        log.info("[AuthController] 로그인 요청: userId={}", userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("password", password);

        try {
            JsonCommandResponse authResp = bizClient.sendToAuth(BizCommands.AUTH_LOGIN, payload, requestId);

            if (!authResp.isSuccess()) {
                // 인증AP 가 실패 응답을 반환한 경우 (잘못된 자격증명 등)
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", authResp.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }

            // isSuccess=true 이지만 payload가 null인 경우 방어 (인증AP 버그 또는 빈 응답)
            if (authResp.getPayload() == null) {
                log.warn("[AuthController] 인증AP 응답 payload null: userId={}", userId);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "인증 서버 응답 오류"));
            }

            Map<String, Object> authPayload = authResp.getPayload();
            String userName = (String) authPayload.getOrDefault("userName", "");
            String userGrade = (String) authPayload.getOrDefault("userGrade", "");
            // 인증AP 는 lastLoginDtime(YYYYMMDDHH24MISS) 형식으로 반환
            String lastLoginDtime = (String) authPayload.getOrDefault("lastLoginDtime", "");
            String lastLogin = formatLoginDtime(lastLoginDtime);

            // JWT 액세스 토큰 발급
            String accessToken = createAccessToken(userId, userName, userGrade);
            // JWT 리프레시 토큰 발급 — 갱신 시 재조회 없이 새 액세스 토큰 발급을 위해 claims 포함
            String refreshToken = createRefreshToken(userId, userName, userGrade);

            // 리프레시 토큰 인메모리 저장 (이전 토큰 덮어쓰기)
            refreshTokenStore.put(userId, refreshToken);

            // HttpLoggingInterceptor.afterCompletion()에서 RES 로그 userId 기록에 사용
            request.setAttribute("loginUserId", userId);

            // 리프레시 토큰을 httpOnly 쿠키로 설정
            setRefreshTokenCookie(response, refreshToken);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", accessToken);
            result.put("userId", userId);
            result.put("userName", userName);
            result.put("userGrade", userGrade);
            result.put("lastLogin", lastLogin);

            log.info("[AuthController] 로그인 성공: userId={}", userId);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("[AuthController] 인증AP 통신 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "인증 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/auth/me
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 현재 로그인 사용자 정보 조회.
     *
     * <p>JWT 필터에서 검증된 {@code userId} 를 기반으로 인증AP 에
     * AUTH_ME 커맨드를 전송하여 최신 사용자 정보를 조회한다.</p>
     *
     * @param request HTTP 요청 — JWT 필터가 설정한 {@code userId} 속성 참조
     * @return 사용자 정보 ({@code userId, userName, userGrade, lastLogin})
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(HttpServletRequest request) {
        // JWT 필터가 request 속성으로 설정한 userId
        String userId = (String) request.getAttribute("userId");
        // HttpLoggingInterceptor가 HTTP 수신 시 생성한 UUID
        String requestId = (String) request.getAttribute("requestId");
        log.debug("[AuthController] 사용자 정보 조회: userId={}", userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);

        try {
            JsonCommandResponse authResp = bizClient.sendToAuth(BizCommands.AUTH_ME, payload, requestId);

            if (!authResp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", authResp.getMessage()));
            }

            // isSuccess=true 이지만 payload가 null인 경우 방어
            if (authResp.getPayload() == null) {
                log.warn("[AuthController] 인증AP 응답 payload null (me): userId={}", userId);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "인증 서버 응답 오류"));
            }

            Map<String, Object> authPayload = authResp.getPayload();
            String lastLoginDtime = (String) authPayload.getOrDefault("lastLoginDtime", "");

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("userName", authPayload.getOrDefault("userName", ""));
            result.put("userGrade", authPayload.getOrDefault("userGrade", ""));
            result.put("lastLogin", formatLoginDtime(lastLoginDtime));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("[AuthController] 인증AP 통신 오류 (me): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "인증 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/auth/refresh
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 리프레시 토큰으로 새 액세스 토큰 발급.
     *
     * <p>httpOnly 쿠키({@code refreshToken})에서 토큰을 추출하여 서명·만료를 검증하고,
     * 저장된 토큰과 일치하면 새 액세스 토큰을 발급한다.</p>
     *
     * @param request  HTTP 요청 — 쿠키에서 refreshToken 추출
     * @param response HTTP 응답 — 갱신된 리프레시 토큰 쿠키 재설정
     * @return 새 액세스 토큰 ({@code accessToken, lastLogin})
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 쿠키 배열에서 refreshToken 쿠키를 찾아 값 추출
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "리프레시 토큰이 없습니다."));
        }

        try {
            // 리프레시 토큰 서명·만료 검증
            Claims claims = Jwts.parser()
                    .verifyWith(refreshKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String userId = claims.getSubject();

            // 저장된 토큰과 비교 (토큰 재사용 공격 방지)
            String storedToken = refreshTokenStore.get(userId);
            if (!refreshToken.equals(storedToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "유효하지 않은 리프레시 토큰입니다."));
            }

            String userName = (String) claims.get("userName");
            String userGrade = (String) claims.get("userGrade");

            // 새 액세스 토큰 발급
            String newAccessToken = createAccessToken(userId, userName, userGrade);

            // 리프레시 토큰은 재사용 (만료 시점 연장이 필요하면 여기서 새로 발급 가능)
            // 현재는 기존 쿠키를 갱신하지 않음 (원래 만료 시점 유지)

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            // lastLogin 은 리프레시 응답에서는 포함하지 않음 (me API 에서 조회)

            log.debug("[AuthController] 토큰 갱신 성공: userId={}", userId);
            return ResponseEntity.ok(result);

        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("[AuthController] 리프레시 토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "리프레시 토큰이 만료되었거나 유효하지 않습니다."));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/auth/logout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 로그아웃 처리.
     *
     * <p>인메모리 리프레시 토큰 저장소에서 해당 사용자의 토큰을 제거하고,
     * 리프레시 토큰 쿠키를 만료 처리한다.</p>
     *
     * @param request  HTTP 요청 — JWT 필터가 설정한 {@code userId} 속성 참조
     * @param response HTTP 응답 — 쿠키 만료 설정
     * @return {@code {success: true}}
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String userId = (String) request.getAttribute("userId");
        log.info("[AuthController] 로그아웃: userId={}", userId);

        // 인메모리 리프레시 토큰 제거
        refreshTokenStore.remove(userId);

        // 리프레시 토큰 쿠키 만료 (maxAge=0)
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0) // 즉시 만료
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 유틸리티 메서드
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * JWT 액세스 토큰을 생성한다.
     *
     * @param userId    사용자 ID (subject)
     * @param userName  사용자 이름 (claim)
     * @param userGrade 사용자 등급 (claim)
     * @return 서명된 JWT 액세스 토큰 문자열
     */
    private String createAccessToken(String userId, String userName, String userGrade) {
        return Jwts.builder()
                .subject(userId)
                .claim("userName", userName)
                .claim("userGrade", userGrade)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenTtlMs))
                .signWith(accessKey)
                .compact();
    }

    /**
     * JWT 리프레시 토큰을 생성한다.
     *
     * <p>리프레시 토큰에 userName, userGrade 를 포함하여
     * 갱신 시 인증AP 에 재조회 없이 새 액세스 토큰을 발급할 수 있게 한다.</p>
     *
     * @param userId    사용자 ID (subject)
     * @param userName  사용자 이름 (claim — 갱신 시 액세스 토큰 재발급에 사용)
     * @param userGrade 사용자 등급 (claim — 갱신 시 액세스 토큰 재발급에 사용)
     * @return 서명된 JWT 리프레시 토큰 문자열
     */
    private String createRefreshToken(String userId, String userName, String userGrade) {
        return Jwts.builder()
                .subject(userId)
                .claim("userName", userName)
                .claim("userGrade", userGrade)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenTtlMs))
                .signWith(refreshKey)
                .compact();
    }

    /**
     * 리프레시 토큰을 httpOnly 쿠키로 설정한다.
     *
     * @param response     HTTP 응답
     * @param refreshToken 리프레시 토큰 문자열
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)   // 개발 환경 HTTP — 운영에서는 true 로 변경
                .sameSite("Lax")
                .maxAge(refreshTokenTtlMs / 1000) // ms → sec 변환
                .path("/api/auth")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * HTTP 요청 쿠키 배열에서 {@code refreshToken} 쿠키 값을 추출한다.
     *
     * @param request HTTP 요청
     * @return 리프레시 토큰 문자열, 없으면 {@code null}
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 인증AP 가 반환하는 lastLoginDtime(YYYYMMDDHH24MISS) 형식을
     * 화면 표시용 {@code "YYYY.MM.DD HH:mm:ss"} 형식으로 변환한다.
     *
     * @param raw 원본 날짜 문자열 (14자리), null 허용
     * @return 포맷된 날짜 문자열, 입력이 null 이거나 14자리가 아니면 원본 반환
     */
    private String formatLoginDtime(String raw) {
        if (raw == null || raw.length() != 14) return raw != null ? raw : "";
        return raw.substring(0, 4) + "." + raw.substring(4, 6) + "." + raw.substring(6, 8) + " "
                + raw.substring(8, 10) + ":" + raw.substring(10, 12) + ":" + raw.substring(12, 14);
    }
}
