package com.example.spideradmin.domain.loglevel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>Additivity 변경 요청 DTO</h3>
 * <p>특정 로거의 Additivity를 런타임에 변경할 때 사용합니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditivityUpdateRequest {

    /** 변경 대상 로거 이름 (패키지명 또는 클래스명) */
    @NotBlank(message = "로거 이름은 필수입니다")
    private String logName;

    /**
     * Additivity 설정값
     * <ul>
     *     <li>Y: 로그 이벤트를 상위 로거에도 전파 (Logback 기본값)</li>
     *     <li>N: 이 로거에 연결된 Appender에서만 처리, 상위로 전파하지 않음</li>
     * </ul>
     */
    @NotBlank(message = "Additivity 값은 필수입니다")
    @Pattern(regexp = "^[YN]$", message = "Additivity는 Y 또는 N이어야 합니다")
    private String additivity;
}
