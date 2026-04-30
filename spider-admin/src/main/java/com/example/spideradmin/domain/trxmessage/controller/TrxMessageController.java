package com.example.spideradmin.domain.trxmessage.controller;

import com.example.spideradmin.domain.messagetest.config.MessageTestProperties;
import com.example.spideradmin.domain.trxmessage.dto.MessageBrowseResponse;
import com.example.spideradmin.domain.trxmessage.dto.TrxMessageMappingUpdateRequest;
import com.example.spideradmin.domain.trxmessage.dto.TrxMessageSearchRequest;
import com.example.spideradmin.domain.trxmessage.dto.TrxMessageWithTrxResponse;
import com.example.spideradmin.domain.trxmessage.service.TrxMessageService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * TRX 메시지 리소스에 대한 조회 인터페이스를 제공합니다.
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 *
 * <p>TRX 관리 메뉴와 전문 테스트 메뉴 모두에서 접근 가능합니다.</p>
 *
 * @see TrxMessageService
 */
@RestController
@RequestMapping("/api/trx-messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TRX:R', 'TRX_TEST:R')")
public class TrxMessageController {

    private final TrxMessageService trxMessageService;
    private final MessageTestProperties messageTestProperties;

    /**
     * 필터 조건에 따른 TRX 메시지 페이징 검색을 수행합니다.
     *
     * @param page            페이지 번호 (1-based index)
     * @param size            페이지 당 항목 수
     * @param sortBy          정렬 기준 필드
     * @param sortDirection   정렬 방향 (ASC, DESC)
     * @param searchField     검색 대상 필드
     * @param searchValue     검색어
     * @param orgIdFilter     기관 ID 필터
     * @param ioTypeFilter    I/O 타입 필터
     * @param trxIdFilter     거래 ID 필터
     * @param forTest         전문 테스트용 요청 여부 (true일 경우 허용된 기관만 필터링)
     * @return 페이징 처리된 TRX 메시지 검색 결과
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<TrxMessageWithTrxResponse>>> getTrxMessagesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String ioTypeFilter,
            @RequestParam(required = false) String trxIdFilter,
            @RequestParam(defaultValue = "false") boolean forTest) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        TrxMessageSearchRequest searchDTO = TrxMessageSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .orgIdFilter(orgIdFilter)
                .ioTypeFilter(ioTypeFilter)
                .trxIdFilter(trxIdFilter)
                // 전문 테스트 요청인 경우 허용된 기관 목록 설정 및 중복 제거
                .allowedOrgIds(forTest ? messageTestProperties.getAllowedOrgs() : null)
                .deduplicate(forTest)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(trxMessageService.getTrxMessagesWithSearch(pageRequest, searchDTO)));
    }

    /**
     * 전문 목록 조회 (전문 조회 모달용) - 페이징 지원
     * GET /api/trx-messages/browse
     */
    @GetMapping("/browse")
    public ResponseEntity<ApiResponse<PageResponse<MessageBrowseResponse>>> browseMessagesWithPaging(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String ioTypeFilter,
            @RequestParam(required = false) String orgIdFilter) {

        PageRequest pageRequest = PageRequest.builder().page(page).size(size).build();

        PageResponse<MessageBrowseResponse> result = trxMessageService.browseMessagesWithPaging(
                pageRequest, searchField, searchValue, ioTypeFilter, orgIdFilter);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 특정 거래의 전문 매핑 정보 조회 (모든 orgId 포함)
     * GET /api/trx-messages/by-trx/{trxId}/{ioType}
     */
    @GetMapping("/by-trx/{trxId}/{ioType}")
    public ResponseEntity<ApiResponse<List<TrxMessageWithTrxResponse>>> getTrxMessagesByTrxAndIoType(
            @PathVariable String trxId, @PathVariable String ioType) {
        List<TrxMessageWithTrxResponse> result = trxMessageService.getAllTrxMessagesByTrxIdAndIoType(trxId, ioType);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 거래-전문 매핑 변경 (messageId 또는 stdMessageId 업데이트)
     * PUT /api/trx-messages/{trxId}/{orgId}/{ioType}/message
     */
    @PutMapping("/{trxId}/{orgId}/{ioType}/message")
    @PreAuthorize("hasAnyAuthority('TRX:W', 'TRX_TEST:W')")
    public ResponseEntity<ApiResponse<Void>> updateTrxMessageId(
            @PathVariable String trxId,
            @PathVariable String orgId,
            @PathVariable String ioType,
            @RequestBody TrxMessageMappingUpdateRequest requestDTO) {
        trxMessageService.updateTrxMessageId(trxId, orgId, ioType, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
