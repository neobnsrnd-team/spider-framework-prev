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
 * TRANSFER_PAYABLE_AMT 커맨드 핸들러.
 *
 * <p>카드 결제 가능 금액 조회 요청을 수신하여 mock-core의 CORE_PAYABLE_AMT로 중계한다.</p>
 *
 * <ul>
 *   <li>인바운드 페이로드: {@code {userId, cardId}}</li>
 *   <li>응답 페이로드:    {@code {payableAmount, creditLimit}}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferPayableAmtHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final TransferService transferService;

    @Override
    public boolean supports(String command) {
        return BizCommands.TRANSFER_PAYABLE_AMT.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        log.debug("[TransferPayableAmtHandler] 요청 수신 — requestId={}", request.getRequestId());
        return transferService.forward(BizCommands.TRANSFER_PAYABLE_AMT, BizCommands.CORE_PAYABLE_AMT, request);
    }
}
