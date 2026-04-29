package com.example.spider_admin.domain.messagehandler.controller;

import com.example.spider_admin.domain.messagehandler.dto.HandlerBatchRequest;
import com.example.spider_admin.domain.messagehandler.dto.HandlerResponse;
import com.example.spider_admin.domain.messagehandler.service.MessageHandlerService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/message-handlers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE_HANDLER:R')")
public class MessageHandlerController {

    private final MessageHandlerService handlerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HandlerResponse>>> getHandlers(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String trxType,
            @RequestParam(required = false) String ioType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<HandlerResponse> response = handlerService.searchHandlers(pageRequest, orgId, trxType, ioType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMessageHandlers(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String trxType,
            @RequestParam(required = false) String ioType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] excelBytes = handlerService.exportMessageHandlers(orgId, trxType, ioType, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("MessageHandler", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('MESSAGE_HANDLER:W')")
    public ResponseEntity<ApiResponse<Void>> saveHandlers(
            @RequestParam String orgId, @Valid @RequestBody HandlerBatchRequest request) {
        if (request.getUpserts() != null) {
            request.getUpserts().forEach(item -> item.setOrgId(orgId));
        }
        if (request.getDeletes() != null) {
            request.getDeletes().forEach(item -> item.setOrgId(orgId));
        }
        handlerService.saveBatch(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
