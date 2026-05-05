package com.example.spideradmin.domain.messageinstance.controller;

import com.example.spideradmin.domain.messageinstance.dto.MessageInstanceResponse;
import com.example.spideradmin.domain.messageinstance.dto.MessageInstanceSearchRequest;
import com.example.spideradmin.domain.messageinstance.service.MessageInstanceService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 전문 내역(거래추적로그조회) API 컨트롤러
 * <p>
 * 거래 추적 로그 조회 및 관리 기능을 제공합니다.
 * </p>
 */
@RestController
@RequestMapping("/api/message-instances")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE_INSTANCE:R')")
public class MessageInstanceController {

    private final MessageInstanceService messageInstanceService;

    /**
     * 검색 조건에 따른 전문 내역(거래추적로그) 페이징 검색을 수행합니다.
     *
     * @param page          페이지 번호 (1-based)
     * @param size          페이지 당 항목 수
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향 (ASC, DESC)
     * @param userId        사용자 ID
     * @param trxTrackingNo 거래 추적 번호
     * @param globalId      글로벌 ID
     * @param orgId         기관 ID
     * @param orgMessageId  기관 전문 ID
     * @param trxDateFrom   거래 시작 일자 (YYYYMMDD)
     * @param trxDateTo     거래 종료 일자 (YYYYMMDD)
     * @param trxTimeFrom   거래 시작 시간 (HHMM)
     * @param trxTimeTo     거래 종료 시간 (HHMM)
     * @return 페이징 처리된 전문 내역 검색 결과
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<MessageInstanceResponse>>> search(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String trxTrackingNo,
            @RequestParam(required = false) String globalId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String orgMessageId,
            @RequestParam(required = false) String trxDateFrom,
            @RequestParam(required = false) String trxDateTo,
            @RequestParam(required = false) String trxTimeFrom,
            @RequestParam(required = false) String trxTimeTo) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        MessageInstanceSearchRequest searchDTO = MessageInstanceSearchRequest.builder()
                .userId(userId)
                .trxTrackingNo(trxTrackingNo)
                .globalId(globalId)
                .orgId(orgId)
                .orgMessageId(orgMessageId)
                .trxDateFrom(trxDateFrom)
                .trxDateTo(trxDateTo)
                .trxTimeFrom(trxTimeFrom)
                .trxTimeTo(trxTimeTo)
                .build();

        return ResponseEntity.ok(ApiResponse.success(messageInstanceService.search(pageRequest, searchDTO)));
    }

    /**
     * 거래 추적 번호로 전문 내역 목록을 조회합니다.
     *
     * @param trxTrackingNo 거래 추적 번호
     * @return 해당 추적 번호의 전문 내역 목록
     */
    @GetMapping("/tracking/{trxTrackingNo}")
    public ResponseEntity<ApiResponse<List<MessageInstanceResponse>>> getByTrxTrackingNo(
            @PathVariable String trxTrackingNo) {
        return ResponseEntity.ok(ApiResponse.success(messageInstanceService.getByTrxTrackingNo(trxTrackingNo)));
    }
}
