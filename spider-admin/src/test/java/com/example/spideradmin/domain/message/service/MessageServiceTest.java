package com.example.spideradmin.domain.message.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.spideradmin.domain.message.dto.FieldPoolVerifyResponse;
import com.example.spideradmin.domain.message.dto.MessageVersionResponse;
import com.example.spideradmin.domain.message.dto.StdMessageSearchResponse;
import com.example.spideradmin.domain.message.dto.TableColumnResponse;
import com.example.spideradmin.domain.message.mapper.MessageMapper;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 테스트")
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageService messageService;

    private static final String ORG_ID = "ORG001";
    private static final String MESSAGE_ID = "MSG001";

    // ─── backupMessage ───────────────────────────────────────────

    @Test
    @DisplayName("[백업] 존재하지 않는 전문 백업 시 NotFoundException이 발생한다")
    void backupMessage_notFound_throwsNotFoundException() {
        given(messageMapper.countByMessageId(ORG_ID, MESSAGE_ID)).willReturn(0);

        assertThrows(NotFoundException.class, () -> messageService.backupMessage(ORG_ID, MESSAGE_ID, "백업사유"));
    }

    // ─── restoreMessage ──────────────────────────────────────────

    @Test
    @DisplayName("[복원] 존재하지 않는 버전 복원 시 NotFoundException이 발생한다")
    void restoreMessage_versionNotFound_throwsNotFoundException() {
        given(messageMapper.findVersionsByMessageId(ORG_ID, MESSAGE_ID))
                .willReturn(List.of(MessageVersionResponse.builder().version(1).build()));

        assertThrows(NotFoundException.class, () -> messageService.restoreMessage(ORG_ID, MESSAGE_ID, 999));
    }

    // ─── listVersions ────────────────────────────────────────────

    @Test
    @DisplayName("[버전목록] 버전 목록을 정상적으로 반환한다")
    void listVersions_returnsVersionList() {
        List<MessageVersionResponse> versions = List.of(
                MessageVersionResponse.builder().version(2).build(),
                MessageVersionResponse.builder().version(1).build());

        given(messageMapper.findVersionsByMessageId(ORG_ID, MESSAGE_ID)).willReturn(versions);

        List<MessageVersionResponse> result = messageService.listVersions(ORG_ID, MESSAGE_ID);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersion());
    }

    // ─── listFieldsByVersion ─────────────────────────────────────

    @Test
    @DisplayName("[버전필드] 특정 버전의 필드 목록을 정상적으로 반환한다")
    void listFieldsByVersion_returnsFields() {
        List<MessageFieldResponse> fields =
                List.of(MessageFieldResponse.builder().messageFieldId("FLD01").build());

        given(messageMapper.findFieldsByVersion(ORG_ID, MESSAGE_ID, 1)).willReturn(fields);

        List<MessageFieldResponse> result = messageService.listFieldsByVersion(ORG_ID, MESSAGE_ID, 1);

        assertEquals(1, result.size());
        assertEquals("FLD01", result.get(0).getMessageFieldId());
    }

    // ─── verifyFieldPool ─────────────────────────────────────────

    @Test
    @DisplayName("[필드풀검증] null 입력 시 빈 목록을 반환한다")
    void verifyFieldPool_nullInput_returnsEmpty() {
        List<FieldPoolVerifyResponse> result = messageService.verifyFieldPool(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[필드풀검증] 빈 목록 입력 시 빈 목록을 반환한다")
    void verifyFieldPool_emptyInput_returnsEmpty() {
        List<FieldPoolVerifyResponse> result = messageService.verifyFieldPool(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[필드풀검증] 유효한 필드 ID 목록 입력 시 검증 결과를 반환한다")
    void verifyFieldPool_validInput_returnsResult() {
        FieldPoolVerifyResponse row = new FieldPoolVerifyResponse();
        row.setMessageFieldId("FLD01");
        row.setFieldRegistryYn("Y");

        given(messageMapper.verifyFieldPool(List.of("FLD01"))).willReturn(List.of(row));

        List<FieldPoolVerifyResponse> result = messageService.verifyFieldPool(List.of("FLD01"));

        assertEquals(1, result.size());
        assertEquals("Y", result.get(0).getFieldRegistryYn());
    }

    // ─── findTableColumns ────────────────────────────────────────

    @Test
    @DisplayName("[테이블컬럼] null 입력 시 빈 목록을 반환한다")
    void findTableColumns_nullInput_returnsEmpty() {
        List<TableColumnResponse> result = messageService.findTableColumns(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[테이블컬럼] 빈 문자열 입력 시 빈 목록을 반환한다")
    void findTableColumns_blankInput_returnsEmpty() {
        List<TableColumnResponse> result = messageService.findTableColumns("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[테이블컬럼] 유효한 테이블명 입력 시 컬럼 정보를 반환한다")
    void findTableColumns_validInput_returnsColumns() {
        TableColumnResponse col = new TableColumnResponse();
        col.setColumnName("USER_ID");
        col.setDataType("VARCHAR2");

        given(messageMapper.findTableColumns("FWK_USER")).willReturn(List.of(col));

        List<TableColumnResponse> result = messageService.findTableColumns("fwk_user");

        assertEquals(1, result.size());
        assertEquals("USER_ID", result.get(0).getColumnName());
        verify(messageMapper).findTableColumns("FWK_USER"); // 대문자 변환 확인
    }

    // ─── listHeaderIncludedFields ────────────────────────────────

    @Test
    @DisplayName("[헤더필드] 전문이 존재하지 않으면 빈 목록을 반환한다")
    void listHeaderIncludedFields_messageNotFound_returnsEmpty() {
        given(messageMapper.selectResponseById(ORG_ID, MESSAGE_ID)).willReturn(null);

        List<MessageFieldResponse> result = messageService.listHeaderIncludedFields(ORG_ID, MESSAGE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[헤더필드] 상위전문 없는 경우 현재 전문 필드만 반환한다")
    void listHeaderIncludedFields_noParent_returnsCurrentFields() {
        MessageResponse msg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .parentMessageId(null)
                .build();

        given(messageMapper.selectResponseById(ORG_ID, MESSAGE_ID)).willReturn(msg);
        given(messageMapper.findFieldsByMessageId(ORG_ID, MESSAGE_ID))
                .willReturn(List.of(
                        MessageFieldResponse.builder().messageFieldId("FLD01").build()));

        List<MessageFieldResponse> result = messageService.listHeaderIncludedFields(ORG_ID, MESSAGE_ID);

        assertEquals(1, result.size());
        assertEquals("FLD01", result.get(0).getMessageFieldId());
    }

    @Test
    @DisplayName("[헤더필드] 상위전문이 있는 경우 상위 필드 + 현재 필드를 반환한다")
    void listHeaderIncludedFields_withParent_returnsAllFields() {
        MessageResponse bodyMsg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .parentMessageId("HDR001")
                .build();

        MessageResponse headerMsg = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId("HDR001")
                .parentMessageId(null)
                .build();

        given(messageMapper.selectResponseById(ORG_ID, MESSAGE_ID)).willReturn(bodyMsg);
        given(messageMapper.selectResponseById(ORG_ID, "HDR001")).willReturn(headerMsg);
        given(messageMapper.findFieldsByMessageId(ORG_ID, "HDR001"))
                .willReturn(List.of(MessageFieldResponse.builder()
                        .messageFieldId("HDR_FLD01")
                        .build()));
        given(messageMapper.findFieldsByMessageId(ORG_ID, MESSAGE_ID))
                .willReturn(List.of(MessageFieldResponse.builder()
                        .messageFieldId("BODY_FLD01")
                        .build()));

        List<MessageFieldResponse> result = messageService.listHeaderIncludedFields(ORG_ID, MESSAGE_ID);

        assertEquals(2, result.size());
        assertEquals("HDR_FLD01", result.get(0).getMessageFieldId());
        assertEquals("BODY_FLD01", result.get(1).getMessageFieldId());
    }

    // ─── searchTrxMessagesForCopy ────────────────────────────────

    @Test
    @DisplayName("[표준전문검색] 검색 결과를 페이징하여 반환한다")
    void searchTrxMessagesForCopy_returnsPaginatedResult() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();

        given(messageMapper.countTrxMessagesForStdSearch("trxId", "TRX")).willReturn(1L);
        given(messageMapper.findTrxMessagesForStdSearch(
                        "trxId", "TRX", pageRequest.getOffset(), pageRequest.getEndRow()))
                .willReturn(List.of(StdMessageSearchResponse.builder()
                        .trxId("TRX001")
                        .messageId("MSG001")
                        .build()));

        PageResponse<StdMessageSearchResponse> result =
                messageService.searchTrxMessagesForCopy(pageRequest, "trxId", "TRX");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("TRX001", result.getContent().get(0).getTrxId());
    }

    @Test
    @DisplayName("[표준전문검색] 검색 결과가 없으면 빈 PageResponse를 반환한다")
    void searchTrxMessagesForCopy_noResult_returnsEmptyPage() {
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();

        given(messageMapper.countTrxMessagesForStdSearch(any(), any())).willReturn(0L);
        given(messageMapper.findTrxMessagesForStdSearch(any(), any(), anyInt(), anyInt()))
                .willReturn(List.of());

        PageResponse<StdMessageSearchResponse> result =
                messageService.searchTrxMessagesForCopy(pageRequest, null, null);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
}
