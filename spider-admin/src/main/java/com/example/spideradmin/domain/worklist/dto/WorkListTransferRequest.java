package com.example.spideradmin.domain.worklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업함 권한이양 요청 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkListTransferRequest {
    @NotEmpty
    private List<Integer> workSeqs;

    @NotBlank
    private String fromUserId;

    @NotBlank
    private String toUserId;
}
