package com.example.spiderlink.infra.http;

import com.example.spiderlink.infra.tcp.client.pool.SocketPoolManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SocketPool 상태 조회 내부 API.
 *
 * <p>Admin에서 AP 서버 접속 현황을 실시간으로 조회할 때 사용한다.
 * {@code GET /api/internal/pool/status} 를 호출하면 이 WAS에 등록된
 * 모든 (host:port) 풀의 active / idle / total 수를 반환한다.</p>
 *
 * <pre>{@code
 * GET /api/internal/pool/status
 * →
 * {
 *   "success": true,
 *   "pools": {
 *     "localhost:19300": { "host": "localhost", "port": 19300, "active": 1, "idle": 2, "total": 3, "maxActive": 5 }
 *   }
 * }
 * }</pre>
 *
 * <p>보안: {@link com.example.spidercommon.config.WebMvcConfig}의 인터셉터가
 * {@code /api/internal/**} 경로를 허용 IP(기본: localhost)로만 제한한다.</p>
 *
 * <p>주의: {@code @ConditionalOnBean}을 사용하지 않는다.
 * 해당 애노테이션은 컴포넌트 스캔 중 평가되어 auto-configuration에서 등록되는
 * {@link SocketPoolManager} 빈을 아직 인식하지 못하는 타이밍 문제가 있다.
 * spider-link를 사용하는 모든 앱에서 {@code SpiderLinkAutoConfiguration}이
 * {@link SocketPoolManager}를 항상 등록하므로 조건이 불필요하다.</p>
 */
@RestController
@RequestMapping("/api/internal/pool")
@RequiredArgsConstructor
public class SocketPoolStatusController {

    private final SocketPoolManager socketPoolManager;

    /**
     * 전체 소켓 풀 상태 조회.
     *
     * @return 엔드포인트별 active / idle / total / maxActive 수
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPoolStatus() {
        Map<String, Map<String, Object>> pools = socketPoolManager.getAllPoolInfo();
        return ResponseEntity.ok(Map.of("success", true, "pools", pools));
    }
}
