package com.example.bizauth.handler;

import com.example.bizauth.service.AuthService;
import com.example.bizcommon.BizCommands;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AUTH_LOGIN 커맨드 핸들러.
 *
 * <p>채널AP 로부터 수신한 로그인 요청({@code AUTH_LOGIN})을
 * {@link AuthService#login(JsonCommandRequest)} 에 위임하고,
 * mock-core 가 반환한 응답의 커맨드 이름을 {@code AUTH_LOGIN} 으로 재설정하여 반환한다.</p>
 *
 * <p>요청 페이로드: {@code userId}, {@code password}</p>
 * <p>응답 페이로드: {@code userId}, {@code userName}, {@code userGrade}, {@code lastLoginDtime}</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLoginHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AuthService authService;

    /**
     * 이 핸들러가 {@code AUTH_LOGIN} 커맨드를 처리함을 선언한다.
     *
     * @param command 수신된 커맨드 이름
     * @return {@code AUTH_LOGIN} 커맨드일 때 {@code true}
     */
    @Override
    public boolean supports(String command) {
        return BizCommands.AUTH_LOGIN.equals(command);
    }

    /**
     * 로그인 요청을 처리하고 인증 결과를 반환한다.
     *
     * <p>mock-core 가 반환하는 응답의 커맨드 이름은 {@code CORE_USER_AUTH} 이므로,
     * 채널AP 가 기대하는 {@code AUTH_LOGIN} 으로 덮어써서 반환한다.</p>
     *
     * @param command 수신된 커맨드 이름 ({@code AUTH_LOGIN})
     * @param request 요청 객체 (requestId, payload 포함)
     * @return 로그인 결과 응답 (커맨드={@code AUTH_LOGIN})
     */
    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        log.info("[AuthLoginHandler] 로그인 요청: userId={}", request.getPayload().get("userId"));

        JsonCommandResponse coreResponse = authService.login(request);

        // mock-core 응답의 커맨드(CORE_USER_AUTH)를 채널AP 가 기대하는 AUTH_LOGIN 으로 재설정
        return JsonCommandResponse.builder()
                .command(command)
                .success(coreResponse.isSuccess())
                .message(coreResponse.getMessage())
                .error(coreResponse.getError())
                .payload(coreResponse.getPayload())
                .build();
    }
}
