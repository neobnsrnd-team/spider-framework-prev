package com.example.spider_admin.domain.bizgroup.controller;

import com.example.spider_admin.domain.bizgroup.dto.BizGroupResponse;
import com.example.spider_admin.domain.bizgroup.service.BizGroupService;
import com.example.spider_admin.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/biz-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CODE_GROUP:R')")
public class BizGroupController {

    private final BizGroupService bizGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BizGroupResponse>>> getAllBizGroups() {
        List<BizGroupResponse> bizGroups = bizGroupService.getAllBizGroups();
        return ResponseEntity.ok(ApiResponse.success(bizGroups));
    }
}
