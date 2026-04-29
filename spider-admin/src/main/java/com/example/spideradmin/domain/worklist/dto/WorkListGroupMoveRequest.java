package com.example.spideradmin.domain.worklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업함 항목 그룹 이동 요청 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkListGroupMoveRequest {
    @NotEmpty
    private List<Integer> workSeqs;
    /** 이동할 그룹 ID. 기본 그룹으로 이동 시 null 불가 — userId001 형식으로 전달. */
    @NotBlank
    private String groupId;
}
