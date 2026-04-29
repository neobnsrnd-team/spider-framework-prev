package com.example.spideradmin.domain.wasgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasGroupRequest {

    @NotBlank(message = "WAS 그룹 ID는 필수입니다.")
    @Size(max = 20, message = "WAS 그룹 ID는 20자 이내여야 합니다.")
    private String wasGroupId;

    @Size(max = 50, message = "WAS 그룹명은 50자 이내여야 합니다.")
    private String wasGroupName;

    @Size(max = 200, message = "WAS 그룹 설명은 200자 이내여야 합니다.")
    private String wasGroupDesc;

    /**
     * 그룹에 속한 인스턴스 ID 목록
     */
    private List<String> instanceIds;
}
