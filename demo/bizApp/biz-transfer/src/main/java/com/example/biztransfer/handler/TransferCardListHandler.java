package com.example.biztransfer.handler;

import com.example.bizcommon.BizCommands;
import com.example.biztransfer.service.TransferService;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TRANSFER_CARD_LIST 커맨드 핸들러.
 *
 * <p>사용자의 카드 목록 조회 요청을 수신하여 mock-core의 CORE_CARD_LIST로 중계한다.</p>
 *
 * <ul>
 *   <li>인바운드 페이로드: {@code {userId}}</li>
 *   <li>응답 페이로드:    {@code {cards:[...]}}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferCardListHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final TransferService transferService;

    @Override
    public boolean supports(String command) {
        return BizCommands.TRANSFER_CARD_LIST.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        log.debug("[TransferCardListHandler] 요청 수신 — requestId={}", request.getRequestId());
        return transferService.forward(BizCommands.TRANSFER_CARD_LIST, BizCommands.CORE_CARD_LIST, request);
    }
}
