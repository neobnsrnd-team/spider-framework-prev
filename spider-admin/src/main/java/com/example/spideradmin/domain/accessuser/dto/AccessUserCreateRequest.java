package com.example.spideradmin.domain.accessuser.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>중지거래 접근허용자 생성 요청 DTO</h3>
 * <p>중지거래 접근허용자 신규 등록 시 사용</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessUserCreateRequest {

    /**
     * 구분유형 (T=거래/S=서비스)
     */
    @NotBlank(message = "구분유형은 필수입니다")
    private String gubunType;

    /**
     * 거래/서비스 ID
     */
    @NotBlank(message = "거래/서비스ID는 필수입니다")
    private String trxId;

    /**
     * 접근허용 고객 사용자ID
     */
    @NotBlank(message = "접근허용 사용자ID는 필수입니다")
    private String custUserId;

    /**
     * 사용여부 (Y/N)
     */
    @NotBlank(message = "사용여부는 필수입니다")
    private String useYn;
}
