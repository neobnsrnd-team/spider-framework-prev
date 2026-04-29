package com.example.spider_admin.domain.message.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.message.dto.FieldPoolVerifyResponse;
import com.example.spider_admin.domain.message.dto.HeaderMessageResponse;
import com.example.spider_admin.domain.message.dto.MessageBackupRequest;
import com.example.spider_admin.domain.message.dto.MessageCreateRequest;
import com.example.spider_admin.domain.message.dto.MessageDetailResponse;
import com.example.spider_admin.domain.message.dto.MessageExcelImportResponse;
import com.example.spider_admin.domain.message.dto.MessageListItemResponse;
import com.example.spider_admin.domain.message.dto.MessageRestoreRequest;
import com.example.spider_admin.domain.message.dto.MessageVersionResponse;
import com.example.spider_admin.domain.message.dto.StdMessageSearchResponse;
import com.example.spider_admin.domain.message.dto.TableColumnResponse;
import com.example.spider_admin.domain.message.service.MessageService;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageResponse;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
@DisplayName("MessageController 테스트")
class MessageControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/messages";
    private static final String ORG_ID = "ORG001";
    private static final String MESSAGE_ID = "MSG001";

    // ─── GET /api/messages/types ──────────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[전문타입] 전문 타입 목록 조회 시 HTTP 200을 반환한다")
    void getMessageTypes_returns200() throws Exception {
        given(messageService.getMessageTypes()).willReturn(List.of(Map.of("code", "F", "name", "고정길이")));

        mockMvc.perform(get(BASE_URL + "/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("F"));
    }

    // ─── GET /api/messages/validation-rules ──────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[검증규칙] 검증 규칙 ID 목록 조회 시 HTTP 200을 반환한다")
    void getValidationRuleIds_returns200() throws Exception {
        given(messageService.getValidationRuleIds()).willReturn(List.of("RULE01", "RULE02"));

        mockMvc.perform(get(BASE_URL + "/validation-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── GET /api/messages/headers ───────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[헤더전문] 헤더 전문 목록 조회 시 HTTP 200을 반환한다")
    void getHeaderMessages_returns200() throws Exception {
        given(messageService.getHeaderMessages(ORG_ID))
                .willReturn(List.of(HeaderMessageResponse.builder()
                        .messageId("HDR01")
                        .messageName("공통헤더")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/headers").param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageId").value("HDR01"));
    }

    // ─── GET /api/messages/export ────────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[엑셀] 엑셀 내보내기 시 HTTP 200과 xlsx Content-Type을 반환한다")
    void exportMessages_returns200WithXlsx() throws Exception {
        given(messageService.exportMessages(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(BASE_URL + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("spreadsheetml")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")));
    }

    // ─── POST /api/messages/import ───────────────────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[엑셀업로드] 유효한 xlsx 파일 업로드 시 HTTP 200과 결과를 반환한다")
    void importMessages_validFile_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "messages.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});

        MessageExcelImportResponse result = MessageExcelImportResponse.builder()
                .totalRows(10)
                .created(5)
                .updated(3)
                .skipped(2)
                .errors(List.of())
                .build();

        given(messageService.importFromExcel(any())).willReturn(result);

        mockMvc.perform(multipart(BASE_URL + "/import").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(10))
                .andExpect(jsonPath("$.data.created").value(5))
                .andExpect(jsonPath("$.data.updated").value(3));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[엑셀업로드] READ 권한으로 업로드 시 HTTP 403을 반환한다")
    void importMessages_readOnly_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "messages.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart(BASE_URL + "/import").file(file).with(csrf())).andExpect(status().isForbidden());
    }

    // ─── GET /api/messages/{messageId} ───────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[단건조회] 전문 단건 조회 시 HTTP 200을 반환한다")
    void getMessageById_returns200() throws Exception {
        MessageResponse response = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .build();

        given(messageService.getMessageById(ORG_ID, MESSAGE_ID, false)).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/{messageId}", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(MESSAGE_ID));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[단건조회] 존재하지 않는 전문 조회 시 HTTP 404를 반환한다")
    void getMessageById_notFound_returns404() throws Exception {
        given(messageService.getMessageById(ORG_ID, MESSAGE_ID, false))
                .willThrow(new NotFoundException("messageId: " + MESSAGE_ID));

        mockMvc.perform(get(BASE_URL + "/{messageId}", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /api/messages/{messageId}/detail ────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[상세조회] 전문 상세 조회 시 HTTP 200을 반환한다")
    void getMessageDetail_returns200() throws Exception {
        MessageDetailResponse detail = MessageDetailResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .build();

        given(messageService.getMessageDetail(ORG_ID, MESSAGE_ID)).willReturn(detail);

        mockMvc.perform(get(BASE_URL + "/{messageId}/detail", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(MESSAGE_ID));
    }

    // ─── GET /api/messages/page ──────────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[페이징] 페이징 조회 시 HTTP 200과 PageResponse를 반환한다")
    void getMessagesWithPagination_returns200() throws Exception {
        PageResponse<MessageListItemResponse> page = PageResponse.of(
                List.of(MessageListItemResponse.builder()
                        .orgId(ORG_ID)
                        .messageId(MESSAGE_ID)
                        .messageName("테스트전문")
                        .build()),
                1L,
                0,
                10);

        given(messageService.searchMessages(any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/page").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ─── POST /api/messages ──────────────────────────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[생성] 유효한 요청으로 생성 시 HTTP 201을 반환한다")
    void createMessage_validRequest_returns201() throws Exception {
        MessageCreateRequest request = MessageCreateRequest.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .build();

        MessageResponse response = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .build();

        given(messageService.createMessage(any())).willReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(MESSAGE_ID));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[생성] READ 권한으로 생성 요청 시 HTTP 403을 반환한다")
    void createMessage_readOnly_returns403() throws Exception {
        // messageName(@NotBlank)을 포함한 유효한 요청 — validation을 통과시키고 권한 검사에서만 403이 발생해야 함
        MessageCreateRequest request = MessageCreateRequest.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("테스트전문")
                .build();

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── PUT /api/messages/{messageId} ───────────────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[수정] 유효한 요청으로 수정 시 HTTP 200을 반환한다")
    void updateMessage_validRequest_returns200() throws Exception {
        MessageResponse response = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("수정전문")
                .build();

        given(messageService.updateMessage(eq(ORG_ID), eq(MESSAGE_ID), any())).willReturn(response);

        mockMvc.perform(put(BASE_URL + "/{messageId}", MESSAGE_ID)
                        .param("orgId", ORG_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[수정] READ 권한으로 수정 요청 시 HTTP 403을 반환한다")
    void updateMessage_readOnly_returns403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{messageId}", MESSAGE_ID)
                        .param("orgId", ORG_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/messages/{messageId} ────────────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[삭제] 삭제 요청 시 HTTP 200을 반환한다")
    void deleteMessage_returns200() throws Exception {
        willDoNothing().given(messageService).deleteMessage(ORG_ID, MESSAGE_ID);

        mockMvc.perform(delete(BASE_URL + "/{messageId}", MESSAGE_ID)
                        .param("orgId", ORG_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[삭제] READ 권한으로 삭제 요청 시 HTTP 403을 반환한다")
    void deleteMessage_readOnly_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{messageId}", MESSAGE_ID)
                        .param("orgId", ORG_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/messages/{messageId}/backup ───────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[백업] 유효한 백업 요청 시 HTTP 200을 반환한다")
    void backupMessage_validRequest_returns200() throws Exception {
        MessageBackupRequest request = MessageBackupRequest.builder()
                .orgId(ORG_ID)
                .historyReason("변경 전 백업")
                .build();

        willDoNothing().given(messageService).backupMessage(ORG_ID, MESSAGE_ID, "변경 전 백업");

        mockMvc.perform(post(BASE_URL + "/{messageId}/backup", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[백업] orgId 누락 시 HTTP 400을 반환한다")
    void backupMessage_missingOrgId_returns400() throws Exception {
        MessageBackupRequest request = MessageBackupRequest.builder().build();

        mockMvc.perform(post(BASE_URL + "/{messageId}/backup", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[백업] READ 권한으로 백업 요청 시 HTTP 403을 반환한다")
    void backupMessage_readOnly_returns403() throws Exception {
        MessageBackupRequest request = MessageBackupRequest.builder()
                .orgId(ORG_ID)
                .historyReason("변경 전 백업")
                .build();

        mockMvc.perform(post(BASE_URL + "/{messageId}/backup", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[백업] 존재하지 않는 전문 백업 시 HTTP 404를 반환한다")
    void backupMessage_notFound_returns404() throws Exception {
        MessageBackupRequest request =
                MessageBackupRequest.builder().orgId(ORG_ID).historyReason("백업").build();

        willThrow(new NotFoundException("messageId: " + MESSAGE_ID))
                .given(messageService)
                .backupMessage(ORG_ID, MESSAGE_ID, "백업");

        mockMvc.perform(post(BASE_URL + "/{messageId}/backup", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /api/messages/{messageId}/restore ──────────────────

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[복원] 유효한 복원 요청 시 HTTP 200과 복원된 전문을 반환한다")
    void restoreMessage_validRequest_returns200() throws Exception {
        MessageRestoreRequest request =
                MessageRestoreRequest.builder().orgId(ORG_ID).version(1).build();

        MessageResponse response = MessageResponse.builder()
                .orgId(ORG_ID)
                .messageId(MESSAGE_ID)
                .messageName("복원전문")
                .build();

        given(messageService.restoreMessage(ORG_ID, MESSAGE_ID, 1)).willReturn(response);

        mockMvc.perform(post(BASE_URL + "/{messageId}/restore", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(MESSAGE_ID));
    }

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[복원] orgId 누락 시 HTTP 400을 반환한다")
    void restoreMessage_missingOrgId_returns400() throws Exception {
        MessageRestoreRequest request =
                MessageRestoreRequest.builder().version(1).build();

        mockMvc.perform(post(BASE_URL + "/{messageId}/restore", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[복원] READ 권한으로 복원 요청 시 HTTP 403을 반환한다")
    void restoreMessage_readOnly_returns403() throws Exception {
        MessageRestoreRequest request =
                MessageRestoreRequest.builder().orgId(ORG_ID).version(1).build();

        mockMvc.perform(post(BASE_URL + "/{messageId}/restore", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"MESSAGE:R", "MESSAGE:W"})
    @DisplayName("[복원] 존재하지 않는 버전 복원 시 HTTP 404를 반환한다")
    void restoreMessage_versionNotFound_returns404() throws Exception {
        MessageRestoreRequest request =
                MessageRestoreRequest.builder().orgId(ORG_ID).version(999).build();

        given(messageService.restoreMessage(ORG_ID, MESSAGE_ID, 999)).willThrow(new NotFoundException("version: 999"));

        mockMvc.perform(post(BASE_URL + "/{messageId}/restore", MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /api/messages/{messageId}/versions ──────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[버전목록] 버전 목록 조회 시 HTTP 200을 반환한다")
    void listVersions_returns200() throws Exception {
        given(messageService.listVersions(ORG_ID, MESSAGE_ID))
                .willReturn(List.of(
                        MessageVersionResponse.builder()
                                .version(2)
                                .historyReason("수정 전 백업")
                                .build(),
                        MessageVersionResponse.builder()
                                .version(1)
                                .historyReason("초기 백업")
                                .build()));

        mockMvc.perform(get(BASE_URL + "/{messageId}/versions", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].version").value(2));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[버전목록] 빈 버전 목록 조회 시에도 HTTP 200을 반환한다")
    void listVersions_empty_returns200() throws Exception {
        given(messageService.listVersions(ORG_ID, MESSAGE_ID)).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/{messageId}/versions", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ─── GET /api/messages/{messageId}/versions/{version}/fields ─

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[버전필드] 특정 버전의 필드 목록 조회 시 HTTP 200을 반환한다")
    void listFieldsByVersion_returns200() throws Exception {
        given(messageService.listFieldsByVersion(ORG_ID, MESSAGE_ID, 1))
                .willReturn(List.of(MessageFieldResponse.builder()
                        .messageFieldId("FLD01")
                        .messageFieldName("필드1")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/{messageId}/versions/{version}/fields", MESSAGE_ID, 1)
                        .param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageFieldId").value("FLD01"));
    }

    // ─── POST /api/messages/field-pool/verify ────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[필드풀검증] 필드풀 검증 요청 시 HTTP 200을 반환한다")
    void verifyFieldPool_returns200() throws Exception {
        FieldPoolVerifyResponse row = new FieldPoolVerifyResponse();
        row.setMessageFieldId("FLD01");
        row.setFieldRegistryYn("Y");

        given(messageService.verifyFieldPool(anyList())).willReturn(List.of(row));

        String body = "{\"messageFieldIds\":[\"FLD01\",\"FLD02\"]}";

        mockMvc.perform(post(BASE_URL + "/field-pool/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageFieldId").value("FLD01"));
    }

    // ─── GET /api/messages/table-columns ─────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[테이블컬럼] 테이블 컬럼 조회 시 HTTP 200을 반환한다")
    void findTableColumns_returns200() throws Exception {
        TableColumnResponse col = new TableColumnResponse();
        col.setColumnName("USER_ID");
        col.setDataType("VARCHAR2");
        col.setDataLength(20);

        given(messageService.findTableColumns("FWK_USER")).willReturn(List.of(col));

        mockMvc.perform(get(BASE_URL + "/table-columns").param("tableName", "FWK_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].columnName").value("USER_ID"));
    }

    // ─── GET /api/messages/{messageId}/header-fields ─────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[헤더필드] 헤더전문포함 필드 조회 시 HTTP 200을 반환한다")
    void listHeaderIncludedFields_returns200() throws Exception {
        given(messageService.listHeaderIncludedFields(ORG_ID, MESSAGE_ID))
                .willReturn(List.of(MessageFieldResponse.builder()
                        .messageFieldId("HDR_FLD01")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/{messageId}/header-fields", MESSAGE_ID).param("orgId", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageFieldId").value("HDR_FLD01"));
    }

    // ─── GET /api/messages/std-search ────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[표준전문검색] 표준전문 검색 시 HTTP 200과 페이징 결과를 반환한다")
    void searchStdMessages_returns200() throws Exception {
        PageResponse<StdMessageSearchResponse> page = PageResponse.of(
                List.of(StdMessageSearchResponse.builder()
                        .trxId("TRX001")
                        .messageId(MESSAGE_ID)
                        .build()),
                1L,
                0,
                20);

        given(messageService.searchTrxMessagesForCopy(any(), anyString(), anyString()))
                .willReturn(page);

        mockMvc.perform(get(BASE_URL + "/std-search")
                        .param("page", "1")
                        .param("size", "20")
                        .param("searchField", "trxId")
                        .param("searchValue", "TRX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].trxId").value("TRX001"));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE:R")
    @DisplayName("[표준전문검색] 검색 조건 없이 조회해도 HTTP 200을 반환한다")
    void searchStdMessages_noParams_returns200() throws Exception {
        PageResponse<StdMessageSearchResponse> page = PageResponse.of(List.of(), 0L, 0, 20);

        given(messageService.searchTrxMessagesForCopy(any(), any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/std-search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── 인증/인가 ──────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/types")).andExpect(status().isUnauthorized());
    }
}
