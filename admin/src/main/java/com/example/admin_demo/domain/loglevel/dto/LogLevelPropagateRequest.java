package com.example.admin_demo.domain.loglevel.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그 레벨·Additivity WAS Reload 요청 DTO.
 *
 * <p>{@code gubun}에 따라 {@code level} 또는 {@code additivity} 중 하나를 채워서 전달한다.</p>
 *
 * <pre>{@code
 * // 레벨 Reload
 * { "instanceIds": ["biz-auth"], "gubun": "log_config_level",
 *   "logName": "com.example", "level": "DEBUG" }
 *
 * // Additivity Reload
 * { "instanceIds": ["biz-auth"], "gubun": "log_config_additivity",
 *   "logName": "com.example", "additivity": "N" }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogLevelPropagateRequest {

    /** Reload 대상 WAS 인스턴스 ID 목록 */
    @NotEmpty(message = "Reload할 WAS 인스턴스를 선택해주세요.")
    private List<String> instanceIds;

    /** 관리 명령 구분 ({@code log_config_level} 또는 {@code log_config_additivity}) */
    @NotBlank(message = "gubun은 필수입니다.")
    private String gubun;

    /** 대상 로거 이름 */
    @NotBlank(message = "로거 이름은 필수입니다.")
    private String logName;

    /** 변경할 로그 레벨 — {@code gubun=log_config_level} 시 사용. null이면 상속 */
    private String level;

    /** 변경할 Additivity — {@code gubun=log_config_additivity} 시 사용 (Y 또는 N) */
    private String additivity;

    /** {@code gubun=log_config_additivity}일 때 additivity는 반드시 Y 또는 N이어야 한다. */
    @AssertTrue(message = "gubun=log_config_additivity일 때 additivity는 Y 또는 N이어야 합니다.")
    public boolean isAdditivityValid() {
        if (!"log_config_additivity".equals(gubun)) {
            return true;
        }
        return "Y".equals(additivity) || "N".equals(additivity);
    }
}
