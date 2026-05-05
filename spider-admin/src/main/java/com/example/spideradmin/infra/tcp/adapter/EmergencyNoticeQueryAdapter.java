package com.example.spideradmin.infra.tcp.adapter;

import com.example.spideradmin.domain.emergencynotice.service.EmergencyNoticeDeployService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 긴급공지 상태 조회 TCP 어댑터.
 *
 * <p>{@link com.example.spideradmin.infra.tcp.handler.NoticeStateQueryCommandHandler}가
 * domain service를 직접 참조하지 않도록 infra 계층에서 감싸는 어댑터.
 * 다른 CommandHandler 구현체와 동일하게 handler → adapter → domain 계층 의존 방향을 유지한다.</p>
 */
@Component
@RequiredArgsConstructor
public class EmergencyNoticeQueryAdapter {

    private final EmergencyNoticeDeployService emergencyNoticeDeployService;

    /**
     * 현재 긴급공지 배포 상태 전체를 조회하여 반환한다.
     *
     * @return deployStatus·notices·displayType·closeableYn·hideTodayYn 포함 Map
     */
    public Map<String, Object> buildNoticeStatePayload() {
        return emergencyNoticeDeployService.buildNoticeStatePayload();
    }
}
