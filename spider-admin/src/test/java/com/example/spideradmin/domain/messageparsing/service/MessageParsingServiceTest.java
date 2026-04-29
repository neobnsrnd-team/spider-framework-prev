package com.example.spideradmin.domain.messageparsing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.example.spideradmin.domain.message.dto.MessageSearchResponse;
import com.example.spideradmin.domain.message.mapper.MessageMapper;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageParseRequest;
import com.example.spideradmin.domain.messageparsing.dto.MessageResponse;
import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.domain.org.mapper.OrgMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageParsingService 테스트")
class MessageParsingServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private MessageParsingService messageParsingService;

    private static final String ORG_ID = "ORG001";
    private static final String MESSAGE_ID = "MSG001";

    // ─── getOrgList ──────────────────────────────────────────────

    @Test
    @DisplayName("[기관목록] 기관 목록을 정상적으로 반환한다")
    void getOrgList_returnsList() {
        OrgResponse org = OrgResponse.builder().orgId(ORG_ID).orgName("테스트기관").build();
        given(orgMapper.findAll()).willReturn(List.of(org));

        List<OrgResponse> result = messageParsingService.getOrgList();

        assertEquals(1, result.size());
        assertEquals(ORG_ID, result.get(0).getOrgId());
    }

    // ─── convertToJson — 동일 포멧 ──────────────────────────────

    @Test
    @DisplayName("[JSON변환] 전문이 존재하지 않으면 NotFoundException이 발생한다")
    void convertToJson_messageNotFound_throwsNotFoundException() {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId(ORG_ID)
                .messageId("NONEXIST")
                .rawString("ABCD")
                .build();

        given(messageMapper.selectResponseById(ORG_ID, "NONEXIST")).willReturn(null);

        assertThrows(NotFoundException.class, () -> messageParsingService.convertToJson(request));
    }

    @Test
    @DisplayName("[JSON변환] 동일 포멧 JSON 생성 시 필드 매핑을 포함한 결과를 반환한다")
    void convertToJson_sameFormat_returnsFieldMapping() {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .rawString("ABCD1234")
                .build();

        MessageResponse msg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .parentMessageId(null)
                .build();

        MessageFieldResponse field1 = MessageFieldResponse.builder()
                .messageFieldId("FLD01")
                .messageFieldName("필드1")
                .dataLength(4L)
                .dataType("S")
                .sortOrder(1)
                .build();

        MessageFieldResponse field2 = MessageFieldResponse.builder()
                .messageFieldId("FLD02")
                .messageFieldName("필드2")
                .dataLength(4L)
                .dataType("S")
                .sortOrder(2)
                .build();

        given(messageMapper.selectResponseById(ORG_ID, MESSAGE_ID)).willReturn(msg);
        given(messageMapper.findFieldsByMessageId(ORG_ID, MESSAGE_ID)).willReturn(List.of(field1, field2));

        Map<String, Object> result = messageParsingService.convertToJson(request);

        assertNotNull(result);
        assertEquals(ORG_ID, result.get("orgId"));
        assertEquals(MESSAGE_ID, result.get("messageId"));
        assertEquals("테스트전문", result.get("messageName"));
        assertNotNull(result.get("fields"));
    }

    // ─── convertToJson — 지정 전문 포멧 ─────────────────────────

    @Test
    @DisplayName("[JSON변환] 대상 전문이 존재하지 않으면 NotFoundException이 발생한다")
    void convertToJson_targetNotFound_throwsNotFoundException() {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .rawString("ABCD1234")
                .targetOrgId("ORG002")
                .targetMessageId("MSG002")
                .build();

        MessageResponse sourceMsg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("소스전문")
                .parentMessageId(null)
                .build();

        MessageFieldResponse field = MessageFieldResponse.builder()
                .messageFieldId("FLD01")
                .dataLength(8L)
                .dataType("S")
                .sortOrder(1)
                .build();

        given(messageMapper.selectResponseById(ORG_ID, MESSAGE_ID)).willReturn(sourceMsg);
        given(messageMapper.findFieldsByMessageId(ORG_ID, MESSAGE_ID)).willReturn(List.of(field));
        given(messageMapper.selectResponseById("ORG002", "MSG002")).willReturn(null);

        assertThrows(NotFoundException.class, () -> messageParsingService.convertToJson(request));
    }

    // ─── searchMessages ──────────────────────────────────────────

    @Test
    @DisplayName("[전문검색] 키워드와 검색필드가 모두 있으면 필터링하여 반환한다")
    void searchMessages_withKeywordAndField_returnsFiltered() {
        MessageResponse msg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .messageDesc("설명")
                .build();

        given(messageMapper.findAllBySearch("messageId", "MSG", ORG_ID, null, null, null, null, null))
                .willReturn(List.of(msg));

        List<MessageSearchResponse> result = messageParsingService.searchMessages(ORG_ID, "messageId", "MSG");

        assertEquals(1, result.size());
        assertEquals(MESSAGE_ID, result.get(0).getMessageId());
        assertEquals("테스트전문", result.get(0).getMessageName());
    }

    @Test
    @DisplayName("[전문검색] 키워드가 null이면 필터 없이 전체 검색한다")
    void searchMessages_nullKeyword_searchesAll() {
        given(messageMapper.findAllBySearch(null, null, ORG_ID, null, null, null, null, null))
                .willReturn(List.of());

        List<MessageSearchResponse> result = messageParsingService.searchMessages(ORG_ID, "messageId", null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[전문검색] 빈 키워드도 필터 없이 검색한다")
    void searchMessages_emptyKeyword_searchesAll() {
        given(messageMapper.findAllBySearch(null, null, ORG_ID, null, null, null, null, null))
                .willReturn(List.of());

        List<MessageSearchResponse> result = messageParsingService.searchMessages(ORG_ID, "messageId", "");

        assertTrue(result.isEmpty());
    }

    // ─── getOrgIdList ────────────────────────────────────────────

    @Test
    @DisplayName("[기관ID목록] 기관 ID 목록을 정상적으로 반환한다")
    void getOrgIdList_returnsList() {
        given(messageMapper.findDistinctOrgIds()).willReturn(List.of(ORG_ID, "ORG002"));

        List<String> result = messageParsingService.getOrgIdList();

        assertEquals(2, result.size());
        assertTrue(result.contains(ORG_ID));
    }
}
