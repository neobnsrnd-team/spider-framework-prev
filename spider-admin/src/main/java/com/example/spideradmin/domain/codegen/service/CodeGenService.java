package com.example.spideradmin.domain.codegen.service;

import com.example.spideradmin.domain.codegen.dto.CodeGenResponse;
import com.example.spideradmin.domain.codegen.generator.DtoGenerator;
import com.example.spideradmin.domain.message.mapper.MessageMapper;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageResponse;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 전문 Layout 기반 소스 코드 자동 생성 서비스.
 *
 * <p>FWK_MESSAGE + FWK_MESSAGE_FIELD 데이터를 조회하여 DtoGenerator에 위임하고 결과를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenService {

    private final MessageMapper messageMapper;

    /**
     * 전문 ID에 해당하는 전문 정보와 필드 목록을 조회하여 DTO 소스 코드를 생성한다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 생성된 DTO 코드를 담은 응답 DTO
     * @throws NotFoundException 전문이 존재하지 않는 경우
     */
    public CodeGenResponse generate(String orgId, String messageId) {
        log.info("소스 코드 생성 요청 — orgId: {}, messageId: {}", orgId, messageId);

        MessageResponse message = messageMapper.selectResponseById(orgId, messageId);
        if (message == null) {
            throw new NotFoundException("전문을 찾을 수 없습니다. messageId=" + messageId);
        }

        List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, messageId);
        if (fields == null || fields.isEmpty()) {
            log.warn("전문 필드가 없습니다 — messageId: {}", messageId);
        }

        String dtoCode = DtoGenerator.generate(messageId, message.getMessageName(), fields);

        log.info("소스 코드 생성 완료 — messageId: {}, fieldCount: {}", messageId, fields != null ? fields.size() : 0);

        return CodeGenResponse.builder()
                .messageId(messageId)
                .messageName(message.getMessageName())
                .dtoCode(dtoCode)
                .build();
    }
}
