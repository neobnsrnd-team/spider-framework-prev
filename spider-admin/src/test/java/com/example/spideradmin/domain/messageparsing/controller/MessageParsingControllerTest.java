package com.example.spideradmin.domain.messageparsing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spideradmin.domain.message.dto.MessageParseResponse;
import com.example.spideradmin.domain.message.dto.MessageSearchResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageParseRequest;
import com.example.spideradmin.domain.messageparsing.service.MessageParsingService;
import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageParsingController.class)
@DisplayName("MessageParsingController 테스트")
class MessageParsingControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageParsingService messageParsingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/telegram";

    // ─── POST /api/telegram/parse ────────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[파싱] 유효한 파싱 요청 시 HTTP 200과 파싱 결과를 반환한다")
    void parseMessage_validRequest_returns200() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .rawString("ABCDEFGHIJ1234567890")
                .build();

        MessageParseResponse response = MessageParseResponse.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .messageName("테스트전문")
                .rawString("ABCDEFGHIJ1234567890")
                .totalLength(20)
                .fields(List.of())
                .build();

        given(messageParsingService.parseMessage(any())).willReturn(response);

        mockMvc.perform(post(BASE_URL + "/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgId").value("ORG001"))
                .andExpect(jsonPath("$.data.totalLength").value(20));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[파싱] orgId 누락 시 HTTP 400을 반환한다")
    void parseMessage_missingOrgId_returns400() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .messageId("MSG001")
                .rawString("ABCD")
                .build();

        mockMvc.perform(post(BASE_URL + "/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[파싱] 존재하지 않는 전문 파싱 시 HTTP 404를 반환한다")
    void parseMessage_notFound_returns404() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("NONEXIST")
                .rawString("ABCD")
                .build();

        given(messageParsingService.parseMessage(any())).willThrow(new NotFoundException("messageId: ORG001/NONEXIST"));

        mockMvc.perform(post(BASE_URL + "/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[파싱] 파싱 오류 발생 시 HTTP 500을 반환한다")
    void parseMessage_internalError_returns500() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .rawString("ABCD")
                .build();

        given(messageParsingService.parseMessage(any())).willThrow(new InternalException("파싱 중 오류가 발생했습니다"));

        mockMvc.perform(post(BASE_URL + "/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /api/telegram/to-json ──────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[JSON변환] 유효한 요청으로 JSON 변환 시 HTTP 200을 반환한다")
    void convertToJson_validRequest_returns200() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .rawString("ABCDEFGHIJ1234567890")
                .build();

        Map<String, Object> jsonResult = new LinkedHashMap<>();
        jsonResult.put("orgId", "ORG001");
        jsonResult.put("messageId", "MSG001");
        jsonResult.put("messageName", "테스트전문");
        jsonResult.put("fields", Map.of("FLD01", "ABCD"));
        jsonResult.put("totalLength", 20);

        given(messageParsingService.convertToJson(any())).willReturn(jsonResult);

        mockMvc.perform(post(BASE_URL + "/to-json")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgId").value("ORG001"))
                .andExpect(jsonPath("$.data.fields.FLD01").value("ABCD"));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[JSON변환] 대상 전문 지정 시에도 HTTP 200을 반환한다")
    void convertToJson_withTarget_returns200() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .rawString("ABCDEFGHIJ")
                .targetOrgId("ORG002")
                .targetMessageId("MSG002")
                .build();

        Map<String, Object> jsonResult = new LinkedHashMap<>();
        jsonResult.put("orgId", "ORG002");
        jsonResult.put("messageId", "MSG002");

        given(messageParsingService.convertToJson(any())).willReturn(jsonResult);

        mockMvc.perform(post(BASE_URL + "/to-json")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgId").value("ORG002"));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[JSON변환] rawString 누락 시 HTTP 400을 반환한다")
    void convertToJson_missingRawString_returns400() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("MSG001")
                .build();

        mockMvc.perform(post(BASE_URL + "/to-json")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[JSON변환] 전문 미발견 시 HTTP 404를 반환한다")
    void convertToJson_notFound_returns404() throws Exception {
        MessageParseRequest request = MessageParseRequest.builder()
                .orgId("ORG001")
                .messageId("NONEXIST")
                .rawString("ABCD")
                .build();

        given(messageParsingService.convertToJson(any()))
                .willThrow(new NotFoundException("messageId: ORG001/NONEXIST"));

        mockMvc.perform(post(BASE_URL + "/to-json")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /api/telegram/orgs ──────────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[기관목록] 기관 목록 조회 시 HTTP 200을 반환한다")
    void getOrgList_returns200() throws Exception {
        OrgResponse org = new OrgResponse();
        org.setOrgId("ORG001");
        org.setOrgName("테스트기관");

        given(messageParsingService.getOrgList()).willReturn(List.of(org));

        mockMvc.perform(get(BASE_URL + "/orgs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orgId").value("ORG001"));
    }

    // ─── GET /api/telegram/messages ──────────────────────────────

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[전문검색] 전문 검색 시 HTTP 200을 반환한다")
    void searchMessages_returns200() throws Exception {
        given(messageParsingService.searchMessages("ORG001", "messageId", "MSG"))
                .willReturn(List.of(MessageSearchResponse.builder()
                        .orgId("ORG001")
                        .messageId("MSG001")
                        .messageName("테스트전문")
                        .build()));

        mockMvc.perform(get(BASE_URL + "/messages")
                        .param("orgId", "ORG001")
                        .param("searchField", "messageId")
                        .param("keyword", "MSG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].messageId").value("MSG001"));
    }

    @Test
    @WithMockUser(authorities = "MESSAGE_PARSING:R")
    @DisplayName("[전문검색] 검색 조건 없이 전체 조회해도 HTTP 200을 반환한다")
    void searchMessages_noParams_returns200() throws Exception {
        given(messageParsingService.searchMessages(any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── 인증 ───────────────────────────────────────────────────

    @Test
    @DisplayName("[인증] 비인증 요청 시 HTTP 401을 반환한다")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/orgs")).andExpect(status().isUnauthorized());
    }
}
