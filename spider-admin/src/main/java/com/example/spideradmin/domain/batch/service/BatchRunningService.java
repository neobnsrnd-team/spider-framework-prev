package com.example.spideradmin.domain.batch.service;

import com.example.spideradmin.domain.batch.dto.BatchRunningResponse;
import com.example.spideradmin.domain.batch.dto.BatchStopRequest;
import com.example.spideradmin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spideradmin.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 실행 중 배치 모니터링 서비스.
 *
 * <p>등록된 모든 WAS 인스턴스에 병렬 HTTP 요청을 보내 실행 중인 배치 목록을 집계하고,
 * 특정 배치의 강제 종료를 해당 WAS 인스턴스로 프록시한다.
 *
 * <p>WAS 인스턴스 통신 실패 시 해당 인스턴스를 connected=false로 표시하고,
 * 나머지 인스턴스 결과는 정상적으로 집계한다 (부분 실패 허용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRunningService {

    private final WasInstanceMapper wasInstanceMapper;
    private final RestTemplate restTemplate;

    /** batch-was HTTP 포트. TCP 포트(WasInstanceResponse.port)와 별도 설정값을 사용한다. */
    @Value("${batch.was.http-port:8081}")
    private int httpPort;

    /** batch-was의 실행 중 배치 조회 엔드포인트 경로 */
    private static final String RUNNING_ENDPOINT = "/api/batch/running";

    /** batch-was의 배치 강제 종료 엔드포인트 경로 (뒤에 /{jobExecutionId} 추가) */
    private static final String STOP_ENDPOINT = "/api/batch/stop/";

    /**
     * 모든 WAS 인스턴스에서 실행 중인 배치 목록을 병렬로 집계한다.
     *
     * <p>인스턴스별 HTTP 요청은 CompletableFuture로 병렬 실행되며,
     * 통신에 실패한 인스턴스는 connected=false 항목으로 결과에 포함된다.
     *
     * @return 전체 WAS 인스턴스의 실행 중 배치 목록 (instanceId 오름차순 정렬)
     */
    public List<BatchRunningResponse> getRunningBatches() {
        List<WasInstanceResponse> instances = wasInstanceMapper.selectAll();

        // 각 인스턴스에 대해 비동기 HTTP 요청 생성
        List<CompletableFuture<List<BatchRunningResponse>>> futures = instances.stream()
                .map(instance -> CompletableFuture.supplyAsync(() -> fetchRunningFromInstance(instance)))
                .toList();

        // 모든 요청이 완료될 때까지 대기 (RestTemplate 자체에 타임아웃 설정됨: connectTimeout=5s, readTimeout=10s)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 집계 후 instanceId 오름차순 정렬
        return futures.stream()
                .flatMap(f -> f.join().stream())
                .sorted(Comparator.comparing(
                        BatchRunningResponse::getInstanceId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * 지정한 WAS 인스턴스에서 실행 중인 배치 목록을 HTTP GET으로 조회한다.
     *
     * <p>통신 실패 시 예외를 전파하지 않고 connected=false 응답 하나를 반환하여,
     * 다른 인스턴스의 집계 결과에 영향을 주지 않는다.
     *
     * @param instance 조회 대상 WAS 인스턴스 정보
     * @return 실행 중 배치 목록. 통신 실패 시 connected=false 단일 항목 리스트
     */
    private List<BatchRunningResponse> fetchRunningFromInstance(WasInstanceResponse instance) {
        String url = buildWasBaseUrl(instance.getIp()) + RUNNING_ENDPOINT;
        try {
            ResponseEntity<List<BatchRunningResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<BatchRunningResponse>>() {});

            List<BatchRunningResponse> body = response.getBody();
            if (body == null || body.isEmpty()) {
                // 연결은 됐지만 실행 중인 배치가 없음 → 인스턴스를 "대기 중" 항목으로 표시
                return List.of(BatchRunningResponse.builder()
                        .instanceId(instance.getInstanceId())
                        .connected(true)
                        .build());
            }

            // spider-batch가 응답에 자신의 instanceId를 포함하므로,
            // 응답의 instanceId가 쿼리한 instanceId와 다른 항목은 IP를 공유하는
            // 다른 WAS 서버의 데이터 → 해당 인스턴스는 대기 중으로 처리한다.
            String expected = instance.getInstanceId();
            List<BatchRunningResponse> own = body.stream()
                    .filter(item -> expected.equals(item.getInstanceId()))
                    .peek(item -> item.setConnected(true))
                    .toList();

            if (own.isEmpty()) {
                return List.of(BatchRunningResponse.builder()
                        .instanceId(expected)
                        .connected(true)
                        .build());
            }

            return own;

        } catch (RestClientException e) {
            // 타임아웃, 연결 거부 등 통신 오류: WARN 로그 후 connected=false 항목 반환
            log.warn("WAS 인스턴스 통신 실패: instanceId={}, url={}, error={}", instance.getInstanceId(), url, e.getMessage());
            return List.of(BatchRunningResponse.builder()
                    .instanceId(instance.getInstanceId())
                    .connected(false)
                    .build());
        }
    }

    /**
     * 지정한 WAS 인스턴스에 배치 강제 종료 요청을 프록시한다.
     *
     * <p>batch-was의 POST /api/batch/stop/{jobExecutionId} 를 호출하고 응답을 그대로 반환한다.
     *
     * @param request 강제 종료 대상 인스턴스 ID와 JobExecution ID
     * @return batch-was의 강제 종료 응답 문자열
     * @throws NotFoundException instanceId에 해당하는 WAS 인스턴스를 찾을 수 없을 때
     */
    public String stopBatch(BatchStopRequest request) {
        // 대상 인스턴스 조회 — 존재하지 않으면 404
        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(request.getInstanceId());
        if (instance == null) {
            throw new NotFoundException("WAS 인스턴스를 찾을 수 없습니다. instanceId=" + request.getInstanceId());
        }

        String url = buildWasBaseUrl(instance.getIp()) + STOP_ENDPOINT + request.getJobExecutionId();
        log.info(
                "배치 강제 종료 프록시: instanceId={}, jobExecutionId={}, url={}",
                request.getInstanceId(),
                request.getJobExecutionId(),
                url);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, null, String.class);

        return response.getBody();
    }

    /**
     * WAS 인스턴스 IP로 HTTP 기본 URL을 조립한다.
     *
     * <p>WasInstanceResponse.port 는 TCP 포트이므로 HTTP 호출에는 별도 설정인 httpPort를 사용한다.
     *
     * @param ip WAS 인스턴스 IP 주소
     * @return "http://{ip}:{httpPort}" 형식의 기본 URL
     */
    private String buildWasBaseUrl(String ip) {
        return "http://" + ip + ":" + httpPort;
    }
}
