package com.example.spiderbatch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @file BatchConfigurationProperties.java
 * @description spider-batch 공통 설정값을 {@code @ConfigurationProperties}로 통합 관리.
 *
 * <p>기존 {@code @Value}로 흩어진 {@code batch.*} 설정을 단일 빈으로 집약한다.
 * application.yml의 {@code batch} 네임스페이스에 매핑된다.</p>
 *
 * @example
 * <pre>{@code
 * # application.yml
 * batch:
 *   was:
 *     instance-id: BT01
 *   tcp:
 *     port: 9998
 *     handler-pool-size: 20
 *     queue-capacity: 100
 *   file:
 *     input-dir: ./batch-files/input
 *     archive-dir: ./batch-files/archive
 *     error-dir: ./batch-files/error
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "batch")
public class BatchConfigurationProperties {

    private Was was = new Was();
    private Tcp tcp = new Tcp();
    private File file = new File();

    @Getter
    @Setter
    public static class Was {
        /** FWK_WAS_INSTANCE.INSTANCE_ID와 일치해야 하는 WAS 인스턴스 식별자 */
        private String instanceId = "BT01";
    }

    @Getter
    @Setter
    public static class Tcp {
        /** Admin과 통신하는 TCP 포트 */
        private int port = 9998;
        /** 동시 처리 가능한 최대 스레드 수 */
        private int handlerPoolSize = 20;
        /** 핸들러 풀 포화 시 대기 허용 최대 요청 수 */
        private int queueCapacity = 100;
    }

    @Getter
    @Setter
    public static class File {
        /** 고정길이 파일 입력 디렉토리 */
        private String inputDir = "./batch-files/input";
        /** 처리 완료 파일 아카이브 디렉토리 */
        private String archiveDir = "./batch-files/archive";
        /** 처리 실패 파일 보관 디렉토리 */
        private String errorDir = "./batch-files/error";
    }
}
