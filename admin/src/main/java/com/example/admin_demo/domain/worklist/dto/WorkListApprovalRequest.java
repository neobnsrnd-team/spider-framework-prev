package com.example.admin_demo.domain.worklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 결재요청 DTO. */
@Data
@NoArgsConstructor
public class WorkListApprovalRequest {

    /** 결재 대상 작업 일련번호 목록. */
    @NotEmpty
    private List<Integer> workSeqs;

    /** 결재명. */
    @NotBlank
    private String approvalName;

    /** 결재 설명 (선택). */
    private String approvalDesc;

    /** 최종 결재자 사용자 ID. */
    @NotBlank
    private String finalManager;
}
