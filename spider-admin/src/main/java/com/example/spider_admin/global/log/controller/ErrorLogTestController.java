package com.example.spider_admin.global.log.controller;

import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!ci")
@RestController
@RequestMapping("/api/test/error-log")
public class ErrorLogTestController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> triggerError(@RequestParam(defaultValue = "NOT_FOUND") String type) {
        switch (type.toUpperCase()) {
            case "NOT_FOUND" -> throw new NotFoundException("테스트 리소스 test-id-123");
            case "DUPLICATE" -> throw new DuplicateException("테스트 리소스 test-id-123");
            case "INVALID_INPUT" -> throw new InvalidInputException("테스트 필드가 유효하지 않습니다");
            case "INTERNAL" -> throw new InternalException("테스트 내부 오류 발생");
            default -> throw new InvalidInputException("알 수 없는 type: " + type);
        }
    }
}
