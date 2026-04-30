package com.example.spideradmin.domain.xmlproperty.controller;

import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileCreateRequest;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileDetailResponse;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertyFileResponse;
import com.example.spideradmin.domain.xmlproperty.dto.XmlPropertySaveRequest;
import com.example.spideradmin.domain.xmlproperty.service.XmlPropertyService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * XML Property 관리 Controller
 */
@RestController
@RequestMapping("/api/xml-property")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('XML_PROPERTY:R')")
@Slf4j
public class XmlPropertyController {

    private final XmlPropertyService xmlPropertyService;

    /**
     * 파일 목록 조회
     */
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<XmlPropertyFileResponse>>> listFiles() {
        return ResponseEntity.ok(ApiResponse.success(xmlPropertyService.listFiles()));
    }

    /**
     * 파일 항목 조회
     */
    @GetMapping("/files/{fileName}")
    public ResponseEntity<ApiResponse<XmlPropertyFileDetailResponse>> getFileDetail(@PathVariable String fileName) {
        return ResponseEntity.ok(ApiResponse.success(xmlPropertyService.getFileDetail(fileName)));
    }

    /**
     * 파일 등록
     */
    @PostMapping("/files")
    @PreAuthorize("hasAuthority('XML_PROPERTY:W')")
    public ResponseEntity<ApiResponse<XmlPropertyFileResponse>> createFile(
            @Valid @RequestBody XmlPropertyFileCreateRequest request) {
        log.info("XML Property 파일 등록 요청: fileName={}", request.getFileName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(xmlPropertyService.createFile(request)));
    }

    /**
     * 항목 일괄 저장
     */
    @PutMapping("/files/{fileName}/entries")
    @PreAuthorize("hasAuthority('XML_PROPERTY:W')")
    public ResponseEntity<ApiResponse<XmlPropertyFileDetailResponse>> saveEntries(
            @PathVariable String fileName, @Valid @RequestBody XmlPropertySaveRequest request) {
        log.info("XML Property 항목 저장 요청: fileName={}", fileName);
        return ResponseEntity.ok(ApiResponse.success(xmlPropertyService.saveEntries(fileName, request)));
    }

    /**
     * 파일 삭제
     */
    @DeleteMapping("/files/{fileName}")
    @PreAuthorize("hasAuthority('XML_PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileName) {
        log.info("XML Property 파일 삭제 요청: fileName={}", fileName);
        xmlPropertyService.deleteFile(fileName);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
