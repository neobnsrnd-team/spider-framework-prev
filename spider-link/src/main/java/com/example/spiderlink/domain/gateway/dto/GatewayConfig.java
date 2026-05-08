package com.example.spiderlink.domain.gateway.dto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FWK_GATEWAY 1건 — TCP 게이트웨이 설정 정보.
 *
 * <p>GW_PROPERTIES 컬럼은 {@code key=value;key=value} 형식의 문자열로
 * 포트·코덱·스레드 수 등 런타임 설정을 담는다.</p>
 *
 * <pre>
 * GW_PROPERTIES 예시:
 *   port=19100;codec=JSON;pool-size=5;queue=20;header-msg-id=DEMO_GW_HEADER;org-id=DEMO
 * </pre>
 */
@Data
@NoArgsConstructor
public class GatewayConfig {

    private String gwId;
    private String gwName;
    /** {@code key=value;key=value} 형식의 게이트웨이 런타임 속성 */
    private String gwProperties;
    /** I=Listener(수신), O=Adapter(발신) */
    private String ioType;

    /**
     * GW_PROPERTIES 문자열을 Map으로 파싱하여 반환한다.
     *
     * @return key→value Map, gwProperties가 null이면 빈 Map
     */
    public Map<String, String> parseProperties() {
        Map<String, String> props = new HashMap<>();
        if (gwProperties == null || gwProperties.isBlank()) {
            return props;
        }
        Arrays.stream(gwProperties.split(";"))
                .map(String::trim)
                .filter(s -> s.contains("="))
                .forEach(entry -> {
                    int idx = entry.indexOf('=');
                    props.put(entry.substring(0, idx).trim(), entry.substring(idx + 1).trim());
                });
        return props;
    }

    /** GW_PROPERTIES에서 포트 값을 반환한다. 미설정이면 defaultPort를 반환한다. */
    public int getPort(int defaultPort) {
        String val = parseProperties().get("port");
        if (val == null) return defaultPort;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    /** GW_PROPERTIES에서 codec 값을 반환한다. 기본값: JSON */
    public String getCodec() {
        return parseProperties().getOrDefault("codec", "JSON");
    }

    /** GW_PROPERTIES에서 handler pool-size 값을 반환한다. 기본값: 5 */
    public int getPoolSize(int defaultSize) {
        String val = parseProperties().get("pool-size");
        if (val == null) return defaultSize;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultSize;
        }
    }

    /** GW_PROPERTIES에서 요청 대기 큐 크기를 반환한다. 기본값: 20 */
    public int getQueueCapacity(int defaultCapacity) {
        String val = parseProperties().get("queue");
        if (val == null) return defaultCapacity;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultCapacity;
        }
    }

    /**
     * GW_PROPERTIES에서 헤더 전문 ID를 반환한다.
     *
     * <p>null이면 헤더 오프셋 파싱 비활성화 — JSON command 필드 방식으로 동작한다.</p>
     */
    public String getHeaderMessageId() {
        return parseProperties().get("header-msg-id");
    }

    /** GW_PROPERTIES에서 기관 ID를 반환한다. 기본값: DEMO */
    public String getOrgId() {
        return parseProperties().getOrDefault("org-id", "DEMO");
    }

    /**
     * GW_PROPERTIES에서 헤더 고정 길이(byte)를 반환한다.
     *
     * <p>값이 존재하면 {@link com.example.spiderlink.infra.tcp.codec.BankingHeaderMessageCodec}을
     * 사용하여 실제 뱅킹 프로토콜(헤더 내 ASCII 길이 필드)로 처리한다.
     * null이면 기존 4byte binary prefix 방식으로 동작한다.</p>
     */
    public Integer getHeaderLength() {
        String val = parseProperties().get("header-length");
        if (val == null) return null;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * GW_PROPERTIES에서 길이 필드의 헤더 내 시작 위치(byte offset)를 반환한다.
     * 기본값: 0
     */
    public int getLengthFieldOffset() {
        String val = parseProperties().get("length-field-offset");
        if (val == null) return 0;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * GW_PROPERTIES에서 길이 필드의 크기(ASCII 문자 수)를 반환한다.
     * 기본값: 8 (예: "00001024")
     */
    public int getLengthFieldLength() {
        String val = parseProperties().get("length-field-length");
        if (val == null) return 8;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return 8; }
    }

    /**
     * GW_PROPERTIES에서 is-total-length 값을 반환한다.
     *
     * <p>true면 길이 필드 값이 전체(헤더+바디) 길이 — 헤더 길이를 차감하여 바디 길이 계산.
     * false(기본)면 바디만의 길이.</p>
     */
    public boolean isTotalLength() {
        return "true".equalsIgnoreCase(parseProperties().get("is-total-length"));
    }
}
