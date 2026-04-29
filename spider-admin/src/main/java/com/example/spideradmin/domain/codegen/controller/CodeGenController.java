package com.example.spideradmin.domain.codegen.controller;

import com.example.spideradmin.domain.codegen.dto.CodeGenResponse;
import com.example.spideradmin.domain.codegen.service.CodeGenService;
import com.example.spideradmin.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전문 Layout 기반 소스 코드 자동 생성 API 컨트롤러.
 *
 * <p>전문관리 상세 모달의 '소스 생성' 버튼에서 호출되며,
 * FWK_MESSAGE_FIELD 정보를 바탕으로 Java DTO와 MyBatis Mapper XML 코드를 반환한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/code-gen")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE:R')")
public class CodeGenController {

    private final CodeGenService codeGenService;

    /**
     * 전문 ID 기준으로 DTO / Mapper XML 소스 코드를 생성하여 반환한다.
     *
     * @param messageId 전문 ID (Path Variable)
     * @param orgId     기관 ID (Query Parameter)
     * @return 생성된 DTO 코드와 Mapper XML 코드
     */
    @GetMapping("/message/{messageId}")
    public ResponseEntity<ApiResponse<CodeGenResponse>> generateCode(
            @PathVariable String messageId, @RequestParam String orgId) {
        return ResponseEntity.ok(ApiResponse.success(codeGenService.generate(orgId, messageId)));
    }
}
