package com.example.spider_admin.domain.messagetest.controller;

import com.example.spider_admin.domain.messageparsing.dto.MessageSimulationRequest;
import com.example.spider_admin.domain.messageparsing.dto.MessageSimulationResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageFieldForTestResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageTestCreateRequest;
import com.example.spider_admin.domain.messagetest.dto.MessageTestResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageTestUpdateRequest;
import com.example.spider_admin.domain.messagetest.service.MessageTestService;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.service.WasInstanceService;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 전문 테스트 Controller
 * 메뉴 ID: TRX_TEST
 */
@RestController
@RequestMapping("/api/message-test")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRX_TEST:R')") // Class-level: READ default
public class MessageTestController {

    private final MessageTestService messageTestService;
    private final WasInstanceService wasInstanceService;

    /**
     * 현재 사용자의 테스트 케이스 목록 조회
     * GET /api/message-test
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageTestResponse>>> getMyTestCases() {
        List<MessageTestResponse> testCases = messageTestService.getMyTestCases();
        return ResponseEntity.ok(ApiResponse.success(testCases));
    }

    /**
     * 거래의 메시지 필드 목록 조회 (테스트 입력용)
     * GET /api/message-test/fields?trxId={trxId}&ioType={ioType}
     * 권한: TRX_TEST READ (클래스 레벨 상속)
     */
    @GetMapping("/fields")
    public ResponseEntity<ApiResponse<List<MessageFieldForTestResponse>>> getFieldsForTest(
            @RequestParam String trxId, @RequestParam(defaultValue = "I") String ioType) {

        List<MessageFieldForTestResponse> fields = messageTestService.getFieldsForTest(trxId, ioType);
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    /**
     * 인스턴스 ID 목록 조회 (FWK_WAS_INSTANCE에서 조회)
     * GET /api/message-test/instance-ids
     */
    @GetMapping("/instance-ids")
    public ResponseEntity<ApiResponse<List<String>>> getInstanceIds() {
        List<String> instanceIds = wasInstanceService.getAllInstances().stream()
                .map(WasInstanceResponse::getInstanceId)
                .sorted()
                .toList();

        return ResponseEntity.ok(ApiResponse.success(instanceIds));
    }

    /**
     * 거래ID로 테스트 케이스 조회 (검색 기능 포함)
     * GET /api/message-test/by-trx/{trxId}?headerYn=N&testName=xxx&testData=xxx&userId=xxx
     *
     * @param trxId 거래 ID
     * @param headerYn 헤더 여부 필터 (Y/N, 미지정 시 전체 조회)
     * @param testName 테스트명 검색어 (부분 일치)
     * @param testData 테스트 데이터 검색어 (부분 일치)
     * @param userId 등록자 ID 검색어 (부분 일치)
     * @return 검색 조건에 맞는 테스트 케이스 목록
     */
    @GetMapping("/by-trx/{trxId}")
    public ResponseEntity<ApiResponse<List<MessageTestResponse>>> getTestCasesByTrxId(
            @PathVariable String trxId,
            @RequestParam(required = false) String headerYn,
            @RequestParam(required = false) String testName,
            @RequestParam(required = false) String testData,
            @RequestParam(required = false) String userId) {
        List<MessageTestResponse> testCases =
                messageTestService.getTestCasesByTrxId(trxId, headerYn, testName, testData, userId);
        return ResponseEntity.ok(ApiResponse.success(testCases));
    }

    /**
     * 테스트 케이스 생성
     * POST /api/message-test
     */
    @PostMapping
    @PreAuthorize("hasAuthority('TRX_TEST:W')")
    public ResponseEntity<ApiResponse<MessageTestResponse>> createTestCase(
            @Valid @RequestBody MessageTestCreateRequest requestDTO) {
        MessageTestResponse created = messageTestService.createTestCase(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    /**
     * 테스트 케이스 수정
     * PUT /api/message-test/{testSno}
     */
    @PutMapping("/{testSno}")
    @PreAuthorize("hasAuthority('TRX_TEST:W')")
    public ResponseEntity<ApiResponse<MessageTestResponse>> updateTestCase(
            @PathVariable Long testSno, @Valid @RequestBody MessageTestUpdateRequest requestDTO) {
        requestDTO.setTestSno(testSno);
        MessageTestResponse updated = messageTestService.updateTestCase(testSno, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 테스트 케이스 삭제
     * DELETE /api/message-test/{testSno}
     */
    @DeleteMapping("/{testSno}")
    @PreAuthorize("hasAuthority('TRX_TEST:W')")
    public ResponseEntity<ApiResponse<Void>> deleteTestCase(@PathVariable Long testSno) {
        messageTestService.deleteTestCase(testSno);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 전문 시뮬레이션 실행
     * POST /api/message-test/simulate
     * 권한: TRX_TEST READ (클래스 레벨 상속)
     */
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<MessageSimulationResponse>> runSimulation(
            @Valid @RequestBody MessageSimulationRequest requestDTO) {
        MessageSimulationResponse result = messageTestService.runSimulation(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
