package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.repository.AccountRepository;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 사용자 인증 커맨드 핸들러 ({@code CORE_USER_AUTH}).
 *
 * <p>userId·password를 수신하여 DB 인증 후 userId, userName, userGrade, lastLoginDtime을 반환한다.
 * 인증 성공 시 LAST_LOGIN_DTIME도 갱신된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreUserAuthHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(String command) {
        return BizCommands.CORE_USER_AUTH.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();

        try {
            String userId = getRequiredString(payload, "userId");
            String password = getRequiredString(payload, "password");

            log.debug("[CORE_USER_AUTH] 인증 요청 — userId={}", userId);

            Map<String, Object> userInfo = accountRepository.authenticateUser(userId, password);

            log.debug("[CORE_USER_AUTH] 인증 성공 — userId={}", userId);

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("인증 성공")
                    .payload(userInfo)
                    .build();

        } catch (Exception e) {
            log.warn("[CORE_USER_AUTH] 인증 실패 — {}", e.getMessage());
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * payload에서 필수 문자열 값을 추출한다. null이거나 빈 값이면 예외를 던진다.
     *
     * @param payload 요청 payload
     * @param key     추출할 키
     * @return 문자열 값
     * @throws IllegalArgumentException 값이 없거나 빈 문자열인 경우
     */
    private String getRequiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("필수 파라미터 누락: " + key);
        }
        return value.toString();
    }
}
