package com.example.spideradmin.domain.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 등록 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceCreateRequest {

    @NotBlank(message = "서비스 ID는 필수입니다")
    @Size(max = 50, message = "서비스 ID는 50자 이하여야 합니다")
    private String serviceId;

    @NotBlank(message = "서비스명은 필수입니다")
    @Size(max = 200, message = "서비스명은 200자 이하여야 합니다")
    private String serviceName;

    @Size(max = 4000, message = "서비스 설명은 4000자 이하여야 합니다")
    private String serviceDesc;

    @Size(max = 200, message = "클래스명은 200자 이하여야 합니다")
    private String className;

    @Size(max = 100, message = "메서드명은 100자 이하여야 합니다")
    private String methodName;

    @Size(max = 1, message = "서비스 유형은 1자 이하여야 합니다")
    private String serviceType;

    @Size(max = 20, message = "Biz Group ID는 20자 이하여야 합니다")
    private String bizGroupId;

    @Size(max = 20, message = "조직 ID는 20자 이하여야 합니다")
    private String orgId;

    @Size(max = 10, message = "IO 유형은 10자 이하여야 합니다")
    private String ioType;

    @Size(max = 50, message = "Work Space ID는 50자 이하여야 합니다")
    private String workSpaceId;

    @Size(max = 50, message = "TRX ID는 50자 이하여야 합니다")
    private String trxId;

    @NotBlank(message = "사용여부는 필수입니다")
    @Size(max = 1)
    private String useYn;

    @Size(max = 50, message = "전처리 App ID는 50자 이하여야 합니다")
    private String preProcessAppId;

    @Size(max = 50, message = "후처리 App ID는 50자 이하여야 합니다")
    private String postProcessAppId;

    @Size(max = 1)
    private String timeCheckYn;

    @Size(max = 6, message = "시작 시간은 6자 이하여야 합니다")
    private String startTime;

    @Size(max = 6, message = "종료 시간은 6자 이하여야 합니다")
    private String endTime;

    @Size(max = 1)
    private String bizdayServiceYn;

    @Size(max = 6)
    private String bizdayStartTime;

    @Size(max = 6)
    private String bizdayEndTime;

    @Size(max = 1)
    private String saturdayServiceYn;

    @Size(max = 6)
    private String saturdayStartTime;

    @Size(max = 6)
    private String saturdayEndTime;

    @Size(max = 1)
    private String holidayServiceYn;

    @Size(max = 6)
    private String holidayStartTime;

    @Size(max = 6)
    private String holidayEndTime;

    @Size(max = 1)
    private String loginOnlyYn;

    @Size(max = 1)
    private String secureSignYn;

    @Size(max = 20, message = "요청 채널 코드는 20자 이하여야 합니다")
    private String reqChannelCode;

    @Size(max = 1)
    private String bankStatusCheckYn;

    @Size(max = 50, message = "은행 코드 필드는 50자 이하여야 합니다")
    private String bankCodeField;
}
