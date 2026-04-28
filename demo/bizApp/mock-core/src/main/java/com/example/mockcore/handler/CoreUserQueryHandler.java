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
 * 사용자 정보 조회 커맨드 핸들러 ({@code CORE_USER_QUERY}).
 *
 * <p>userId를 수신하여 userName, userGrade, lastLoginDtime을 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreUserQueryHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(String command) {
        return BizCommands.CORE_USER_QUERY.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();

        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj == null || userIdObj.toString().isBlank()) {
                throw new IllegalArgumentException("필수 파라미터 누락: userId");
            }
            String userId = userIdObj.toString();

            log.debug("[CORE_USER_QUERY] 사용자 조회 요청 — userId={}", userId);

            Map<String, Object> userInfo = accountRepository.findUserById(userId);

            log.debug("[CORE_USER_QUERY] 조회 성공 — userId={}", userId);

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("사용자 조회 성공")
                    .payload(userInfo)
                    .build();

        } catch (Exception e) {
            log.warn("[CORE_USER_QUERY] 조회 실패 — {}", e.getMessage());
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
}
