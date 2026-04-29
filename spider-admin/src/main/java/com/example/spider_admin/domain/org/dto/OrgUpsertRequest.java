package com.example.spider_admin.domain.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgUpsertRequest {

    @NotBlank(message = "기관ID는 필수입니다")
    @Size(max = 10, message = "기관ID는 10자 이내여야 합니다")
    private String orgId;

    @NotBlank(message = "기관명은 필수입니다")
    @Size(max = 50, message = "기관명은 50자 이내여야 합니다")
    private String orgName;

    @Size(max = 200, message = "기관설명은 200자 이내여야 합니다")
    private String orgDesc;

    @NotBlank(message = "시작시간은 필수입니다")
    @Size(min = 4, max = 4, message = "시작시간은 4자리(HHmm)여야 합니다")
    private String startTime;

    @NotBlank(message = "종료시간은 필수입니다")
    @Size(min = 4, max = 4, message = "시작시간은 4자리(HHmm)여야 합니다")
    private String endTime;

    @Size(max = 20, message = "XML ROOT TAG는 20자 이내여야 합니다")
    private String xmlRootTag;
}
