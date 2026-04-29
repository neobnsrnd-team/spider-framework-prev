package com.example.spider_admin.domain.adminhistory.controller;

import com.example.spider_admin.domain.adminhistory.dto.AdminActionLogResponse;
import com.example.spider_admin.domain.adminhistory.dto.AdminActionLogSearchRequest;
import com.example.spider_admin.domain.adminhistory.service.AdminActionLogService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 작업이력 로그 조회 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *     <li>GET /api/admin-action-logs - 검색 조건에 따른 로그 페이징 조회</li>
 *     <li>GET /api/admin-action-logs/export - 엑셀 내보내기</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-action-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_ACCESS_HIS:R')")
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminActionLogResponse>>> searchLogs(
            @ModelAttribute AdminActionLogSearchRequest searchDTO,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        log.info(
                "Searching AdminActionLog: userId={}, accessIp={}, accessUrl={}",
                searchDTO.getUserId(),
                searchDTO.getAccessIp(),
                searchDTO.getAccessUrl());

        PageResponse<AdminActionLogResponse> response = adminActionLogService.searchLogs(searchDTO, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(@ModelAttribute AdminActionLogSearchRequest searchDTO) {
        byte[] excelBytes = adminActionLogService.exportLogs(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("AdminActionLog", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
