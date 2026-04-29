package com.example.spider_admin.domain.proxyresponse.controller;

import com.example.spider_admin.domain.message.dto.*;
import com.example.spider_admin.domain.messageparsing.dto.MessageParseRequest;
import com.example.spider_admin.domain.proxyresponse.dto.*;
import com.example.spider_admin.domain.proxyresponse.dto.ProxySettingSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataCreateRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataTrxSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataUpdateRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyValueUpdateRequest;
import com.example.spider_admin.domain.proxyresponse.service.ProxyTestdataService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 당발 대응답 관리 REST API Controller
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 *
 * @see ProxyTestdataService
 */
@RestController
@RequestMapping("/api/proxy-testdata")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROXY_TESTDATA:R')") // Class-level: READ permission
public class ProxyTestdataController {

    private final ProxyTestdataService proxyTestdataService;

    /**
     * 필터 조건에 따른 당발 대응답 테스트 페이징 검색을 수행합니다.
     *
     * @param page              페이지 번호 (1-based index)
     * @param size              페이지 당 항목 수
     * @param sortBy            정렬 기준 필드
     * @param sortDirection     정렬 방향 (ASC, DESC)
     * @param orgIdFilter       기관 ID 필터
     * @param trxIdFilter       거래 ID 필터
     * @param testNameFilter    테스트명 필터
     * @param userIdFilter      등록자 필터
     * @return 페이징 처리된 당발 대응답 테스트 검색 결과
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ProxyTestdataListResponse>>> getMessageTestsWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String trxIdFilter,
            @RequestParam(required = false) String testNameFilter,
            @RequestParam(required = false) String userIdFilter) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        ProxyTestdataSearchRequest searchDTO = ProxyTestdataSearchRequest.builder()
                .orgIdFilter(orgIdFilter)
                .trxIdFilter(trxIdFilter)
                .testNameFilter(testNameFilter)
                .userIdFilter(userIdFilter)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(proxyTestdataService.getMessageTestsWithSearch(pageRequest, searchDTO)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProxyTestdata(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String trxIdFilter,
            @RequestParam(required = false) String testNameFilter,
            @RequestParam(required = false) String userIdFilter) {

        byte[] excelBytes = proxyTestdataService.exportProxyTestdata(
                orgIdFilter, trxIdFilter, testNameFilter, userIdFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("ProxyTestdata", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 거래조회 모달용 거래-전문 매핑 검색
     *
     * @param orgId   기관 ID (exact match, optional)
     * @param trxId   거래 ID (LIKE, optional)
     * @param trxName 거래명 (LIKE, optional)
     * @return 거래-전문 매핑 검색 결과
     */
    @GetMapping("/trx-messages/search")
    public ResponseEntity<ApiResponse<List<ProxyTestdataTrxSearchResponse>>> searchTrxMessages(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String trxId,
            @RequestParam(required = false) String trxName) {

        ProxyTestdataTrxSearchRequest searchDTO = ProxyTestdataTrxSearchRequest.builder()
                .orgId(orgId)
                .trxId(trxId)
                .trxName(trxName)
                .build();

        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.searchTrxMessages(searchDTO)));
    }

    /**
     * 테스트용 전문 필드 조회 (기관+표준 전문 JOIN, makeRealValue 적용)
     * 거래 ID 기반으로 MESSAGE_ID, STD_MESSAGE_ID를 조회하여 필드 목록을 반환합니다.
     *
     * @param orgId 기관 ID
     * @param trxId 거래 ID
     * @return makeRealValue 처리된 전문 필드 목록
     */
    @GetMapping("/test-fields")
    public ResponseEntity<ApiResponse<List<ProxyTestdataFieldResponse>>> getFieldsForTest(
            @RequestParam String orgId, @RequestParam String trxId) {

        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.getFieldsForTest(orgId, trxId)));
    }

    /**
     * 테스트 그룹 ID 목록 조회 (DISTINCT)
     *
     * @param orgId 기관 ID
     * @param trxId 거래 ID
     * @return 그룹 ID 목록
     */
    @GetMapping("/group-ids")
    public ResponseEntity<ApiResponse<List<String>>> getGroupIds(
            @RequestParam String orgId, @RequestParam String trxId) {
        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.getGroupIds(orgId, trxId)));
    }

    /**
     * 통전문을 부모 체인 포함 전체 필드 기준으로 파싱합니다.
     *
     * @param request 파싱 요청 (orgId, messageId, rawString)
     * @return 파싱 결과 (전체 필드 대상)
     */
    @PostMapping("/parse-raw")
    public ResponseEntity<ApiResponse<MessageParseResponse>> parseRawMessage(
            @Valid @RequestBody MessageParseRequest request) {

        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.parseRawMessage(
                request.getOrgId(), request.getMessageId(), request.getRawString())));
    }

    /**
     * 당발 대응답 테스트 상세 조회
     *
     * @param testSno 테스트 일련번호
     * @return 상세 응답 (기관명, 거래명 포함)
     */
    @GetMapping("/{testSno}")
    public ResponseEntity<ApiResponse<ProxyTestdataDetailResponse>> getMessageTestDetail(@PathVariable Long testSno) {
        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.getMessageTestDetail(testSno)));
    }

    /**
     * 당발 대응답 테스트 데이터를 생성합니다.
     *
     * @param requestDTO 생성할 테스트 정보 (유효성 검증 필수)
     * @return 생성 성공 응답
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody ProxyTestdataCreateRequest requestDTO) {
        proxyTestdataService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 당발 대응답 테스트 데이터를 수정합니다.
     *
     * @param testSno    테스트 일련번호
     * @param requestDTO 수정할 테스트 정보
     * @return 수정 성공 응답
     */
    @PutMapping("/{testSno}")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long testSno, @Valid @RequestBody ProxyTestdataUpdateRequest requestDTO) {
        requestDTO.setTestSno(testSno);
        proxyTestdataService.update(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 당발 대응답 테스트 데이터를 삭제합니다.
     *
     * @param testSno     테스트 일련번호
     * @param testGroupId 테스트 그룹 ID
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/{testSno}")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long testSno, @RequestParam(defaultValue = "DEFAULT") String testGroupId) {
        proxyTestdataService.delete(testSno, testGroupId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 대응답 설정 목록 조회
     *
     * @param orgId       기관 ID (필수)
     * @param trxId       거래 ID (필수)
     * @param testGroupId 테스트 그룹 ID (optional)
     * @param testName    테스트명 (LIKE, optional)
     * @param userId      등록자 (LIKE, optional)
     * @return 대응답 설정 목록
     */
    @GetMapping("/proxy-settings")
    public ResponseEntity<ApiResponse<List<ProxySettingListResponse>>> getProxySettings(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(required = false) String testGroupId,
            @RequestParam(required = false) String testName,
            @RequestParam(required = false) String userId) {

        ProxySettingSearchRequest searchDTO = ProxySettingSearchRequest.builder()
                .orgId(orgId)
                .trxId(trxId)
                .testGroupId(testGroupId)
                .testName(testName)
                .userId(userId)
                .build();

        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.getProxySettings(searchDTO)));
    }

    /**
     * 기본 대응답 조회 (DEFAULT_PROXY_YN='Y')
     *
     * @param orgId       기관 ID (필수)
     * @param trxId       거래 ID (필수)
     * @param testGroupId 테스트 그룹 ID (optional)
     * @return 기본 대응답 정보 (없으면 null)
     */
    @GetMapping("/proxy-settings/default")
    public ResponseEntity<ApiResponse<ProxySettingListResponse>> getDefaultProxy(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(required = false) String testGroupId) {

        return ResponseEntity.ok(ApiResponse.success(proxyTestdataService.getDefaultProxy(orgId, trxId, testGroupId)));
    }

    /**
     * 대응답 필드 구분값(PROXY_FIELD) 일괄 업데이트
     *
     * @param orgId       기관 ID
     * @param trxId       거래 ID
     * @param testGroupId 테스트 그룹 ID (기본값: DEFAULT)
     * @param proxyField  필드 구분값
     * @return 성공 응답
     */
    @PutMapping("/proxy-settings/proxy-field")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> updateProxyField(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(defaultValue = "DEFAULT") String testGroupId,
            @RequestParam String proxyField) {

        proxyTestdataService.updateProxyField(orgId, trxId, testGroupId, proxyField);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 대응답 값(PROXY_VALUE) 중복 건수 조회
     *
     * @param orgId       기관 ID
     * @param trxId       거래 ID
     * @param testGroupId 테스트 그룹 ID (기본값: DEFAULT)
     * @param proxyValue  대응답 값
     * @return 중복 건수
     */
    @GetMapping("/proxy-settings/proxy-value/count")
    public ResponseEntity<ApiResponse<Integer>> countByProxyValue(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(defaultValue = "DEFAULT") String testGroupId,
            @RequestParam String proxyValue) {

        return ResponseEntity.ok(
                ApiResponse.success(proxyTestdataService.countByProxyValue(orgId, trxId, testGroupId, proxyValue)));
    }

    /**
     * 대응답 값(PROXY_VALUE) 업데이트 (초기화 후 설정)
     *
     * @param dto 대응답 값 업데이트 요청 DTO
     * @return 성공 응답
     */
    @PutMapping("/proxy-settings/proxy-value")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> updateProxyValue(@Valid @RequestBody ProxyValueUpdateRequest dto) {

        proxyTestdataService.updateProxyValue(dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 기본 대응답 설정 (초기화 후 특정 TEST_SNO에 DEFAULT_PROXY_YN='Y' 설정)
     *
     * @param orgId       기관 ID
     * @param trxId       거래 ID
     * @param testGroupId 테스트 그룹 ID (기본값: DEFAULT)
     * @param testSno     테스트 일련번호
     * @return 성공 응답
     */
    @PutMapping("/proxy-settings/default-proxy/set")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> setDefaultProxy(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(defaultValue = "DEFAULT") String testGroupId,
            @RequestParam Long testSno) {

        proxyTestdataService.setDefaultProxy(orgId, trxId, testGroupId, testSno);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 기본 대응답 해제 (초기화만 수행, DEFAULT_PROXY_YN → 'N')
     *
     * @param orgId       기관 ID
     * @param trxId       거래 ID
     * @param testGroupId 테스트 그룹 ID (기본값: DEFAULT)
     * @return 성공 응답
     */
    @PutMapping("/proxy-settings/default-proxy/clear")
    @PreAuthorize("hasAuthority('PROXY_TESTDATA:W')")
    public ResponseEntity<ApiResponse<Void>> clearDefaultProxy(
            @RequestParam String orgId,
            @RequestParam String trxId,
            @RequestParam(defaultValue = "DEFAULT") String testGroupId) {

        proxyTestdataService.clearDefaultProxy(orgId, trxId, testGroupId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
