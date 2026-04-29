package com.example.spider_admin.domain.messagetest.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 전문 테스트 설정
 *
 * <p>application.yml의 message-test 섹션에서 설정값을 읽어옵니다.</p>
 */
@Component
@ConfigurationProperties(prefix = "message-test")
@Getter
@Setter
public class MessageTestProperties {

    /**
     * 전문 테스트 모달에 표시할 허용 기관 목록
     * <p>application.yml에서 설정: message-test.allowed-orgs</p>
     */
    private List<String> allowedOrgs = new ArrayList<>();

    /**
     * 시뮬레이션 요청 설정
     */
    private Simulation simulation = new Simulation();

    @Getter
    @Setter
    public static class Simulation {
        /**
         * 시뮬레이션 요청 경로
         * <p>application.yml에서 설정: message-test.simulation.path</p>
         * <p>예: /ibsmgr/spider.admin.ap.message.test.ConnectorEmulatorA.web</p>
         */
        private String path = "/ibsmgr/spider.admin.ap.message.test.ConnectorEmulatorA.web";
    }
}
