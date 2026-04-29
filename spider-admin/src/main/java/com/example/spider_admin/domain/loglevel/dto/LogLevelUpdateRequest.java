package com.example.spider_admin.domain.loglevel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * <h3>로그 레벨 변경 요청 DTO</h3>
 * <p>특정 로거의 레벨을 런타임에 변경할 때 사용합니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogLevelUpdateRequest {

    /** 변경 대상 로거 이름 (패키지명 또는 클래스명) */
    @NotBlank(message = "로거 이름은 필수입니다")
    private String logName;

    /**
     * 변경할 로그 레벨. null 이면 명시적 레벨을 제거하고 부모 로거 레벨을 상속합니다.
     */
    @Nullable
    @Pattern(
            regexp = "^(ERROR|WARN|INFO|DEBUG|TRACE|OFF)$",
            message = "유효한 로그 레벨이 아닙니다 (ERROR/WARN/INFO/DEBUG/TRACE/OFF)")
    private String level;
}
