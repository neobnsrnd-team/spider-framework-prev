package com.example.spider_admin.domain.messageparsing.controller;

import com.example.spider_admin.domain.message.dto.MessageParseResponse;
import com.example.spider_admin.domain.message.dto.MessageSearchResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageParseRequest;
import com.example.spider_admin.domain.messageparsing.service.MessageParsingService;
import com.example.spider_admin.domain.org.dto.OrgResponse;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 전문 로그 파싱 Controller
 *
 * 금융권 고정 길이 전문을 파싱하는 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE_PARSING:R')")
public class MessageParsingController {

    private final MessageParsingService messageParsingService;

    /**
     * 전문을 파싱합니다.
     *
     * @param request 파싱 요청 (orgId, messageId, rawString)
     * @return 파싱 결과
     * @throws MessageParsingFailedException 파싱 실패 시 (필드 없음, 파싱 오류 등)
     * @throws MessageNotFoundException 전문 메타데이터를 찾을 수 없는 경우
     */
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<MessageParseResponse>> parseMessage(
            @Valid @RequestBody MessageParseRequest request) {

        log.info(
                "POST /api/telegram/parse - orgId={}, messageId={}, rawLength={}",
                request.getOrgId(),
                request.getMessageId(),
                request.getRawString() != null ? request.getRawString().length() : 0);

        MessageParseResponse response = messageParsingService.parseMessage(request);

        return ResponseEntity.ok(ApiResponse.success("전문 파싱이 완료되었습니다", response));
    }

    /**
     * 전문을 파싱하고 JSON 구조로 변환합니다.
     *
     * @param request 파싱 요청 (orgId, messageId, rawString)
     * @return 필드명-값 매핑이 포함된 JSON 구조
     */
    @PostMapping("/to-json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convertToJson(
            @Valid @RequestBody MessageParseRequest request) {

        log.info(
                "POST /api/telegram/to-json - orgId={}, messageId={}, rawLength={}",
                request.getOrgId(),
                request.getMessageId(),
                request.getRawString() != null ? request.getRawString().length() : 0);

        Map<String, Object> result = messageParsingService.convertToJson(request);

        return ResponseEntity.ok(ApiResponse.success("JSON 변환이 완료되었습니다", result));
    }

    /**
     * 기관 목록을 조회합니다 (드롭다운용).
     *
     * @return 기관 목록 (orgName)
     */
    @GetMapping("/orgs")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getOrgList() {
        log.info("GET /api/telegram/orgs");

        List<OrgResponse> orgs = messageParsingService.getOrgList();

        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    /**
     * 전문을 검색합니다.
     *
     * @param orgId      기관 ID (필터, 선택사항)
     * @param searchField 검색 타입 ("messageId" 또는 "messageName")
     * @param keyword    검색 키워드
     * @return 검색된 전문 목록
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<MessageSearchResponse>>> searchMessages(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String keyword) {

        log.info("GET /api/telegram/messages - orgId={}, searchField={}, keyword={}", orgId, searchField, keyword);

        List<MessageSearchResponse> messages = messageParsingService.searchMessages(orgId, searchField, keyword);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
