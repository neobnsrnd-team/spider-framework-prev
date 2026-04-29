package com.example.spider_admin.domain.accessuser.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>중지거래 접근허용자 수정 요청 DTO</h3>
 * <p>중지거래 접근허용자 정보 수정 시 사용</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessUserUpdateRequest {

    /**
     * 구분유형 (T=거래/S=서비스) - PK
     */
    @NotBlank(message = "구분유형은 필수입니다")
    private String gubunType;

    /**
     * 거래/서비스 ID - PK
     */
    @NotBlank(message = "거래/서비스ID는 필수입니다")
    private String trxId;

    /**
     * 접근허용 고객 사용자ID - PK
     */
    @NotBlank(message = "접근허용 사용자ID는 필수입니다")
    private String custUserId;

    /**
     * 사용여부 (Y/N)
     */
    private String useYn;
}
