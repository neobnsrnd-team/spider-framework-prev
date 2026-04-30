package com.example.spideradmin.domain.reload.service;

import com.example.spideradmin.domain.property.dto.PropertyResponse;
import com.example.spideradmin.domain.property.mapper.PropertyMapper;
import com.example.spideradmin.domain.reload.dto.ReloadExecuteRequest;
import com.example.spideradmin.domain.reload.dto.ReloadResultResponse;
import com.example.spideradmin.domain.reload.dto.ReloadResultResponse.WasReloadResult;
import com.example.spideradmin.domain.reload.dto.ReloadTypeResponse;
import com.example.spideradmin.domain.reload.enums.ReloadType;
import com.example.spideradmin.domain.wasgroup.mapper.WasGroupMapper;
import com.example.spideradmin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spideradmin.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.infra.tcp.client.TcpClient;
import com.example.spideradmin.infra.tcp.model.JsonCommandRequest;
import com.example.spideradmin.infra.tcp.model.JsonCommandResponse;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 운영정보 Reload Service 구현체.
 *
 * <p>WAS 인스턴스별 통신 방식(COMM_TYPE)을 FWK_PROPERTY에서 조회하여
 * HTTP 또는 TCP 중 적합한 전송 방식을 자동 선택한다.</p>
 *
 * <ul>
 *   <li>HTTP: {@link RestTemplate}으로 {@code /api/management/reload}에 POST 요청</li>
 *   <li>TCP:  {@link TcpClient#sendJson}으로 {@code MANAGEMENT_RELOAD} 커맨드 전송</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReloadService {

    private final WasInstanceMapper wasInstanceMapper;
    private final WasGroupMapper wasGroupMapper;
    private final PropertyMapper propertyMapper;
    private final RestTemplate restTemplate;
    private final TcpClient tcpClient;

    @Value("${reload.management.default-port:50005}")
    private int defaultManagementPort;

    @Value("${reload.management.default-ip:localhost}")
    private String defaultManagementIp;

    @Value("${reload.management.endpoint:/api/management/reload}")
    private String managementEndpoint;

    @Value("${reload.management.property-group:was_config}")
    private String propertyGroup;

    public List<ReloadTypeResponse> getReloadTypes() {
        return Arrays.stream(ReloadType.values())
                .filter(ReloadType::isVisible)
                .map(type -> ReloadTypeResponse.builder()
                        .code(type.getCode())
                        .label(type.getDescription())
                        .description(type.getDetail())
                        .build())
                .toList();
    }

    /**
     * 지정된 WAS 그룹에 속한 모든 인스턴스에 Reload 명령을 전송한다.
     *
     * <p>ASIS의 {@code ReloadUtil.reload(ALL_WAS_CONFIG, map)} 역할.
     * {@code reload.default-was-group} 프로퍼티로 대상 그룹을 결정하며,
     * 그룹에 인스턴스가 없으면 빈 결과를 반환한다.</p>
     *
     * @param groupId          WAS 그룹 ID (FWK_WAS_GROUP_INSTANCE)
     * @param reloadType       Reload 종류 코드
     * @param additionalParams 추가 파라미터 (logName, level 등)
     * @return 그룹 내 각 WAS별 Reload 결과
     */
    public ReloadResultResponse executeReloadForGroup(
            String groupId, String reloadType, Map<String, String> additionalParams) {
        List<String> instanceIds = wasGroupMapper.selectInstanceIdsByGroupId(groupId);
        if (instanceIds.isEmpty()) {
            log.warn("그룹 '{}' 에 등록된 WAS 인스턴스가 없습니다.", groupId);
            return ReloadResultResponse.builder()
                    .reloadType(reloadType)
                    .results(Collections.emptyList())
                    .build();
        }
        return executeReload(ReloadExecuteRequest.builder()
                .reloadType(reloadType)
                .instanceIds(instanceIds)
                .additionalParams(additionalParams)
                .build());
    }

    public ReloadResultResponse executeReload(ReloadExecuteRequest request) {
        ReloadType reloadType = ReloadType.fromCode(request.getReloadType());
        if (reloadType == null) {
            throw new InternalException("reloadType: " + request.getReloadType());
        }

        Map<String, String> additionalParams =
                request.getAdditionalParams() != null ? request.getAdditionalParams() : Collections.emptyMap();

        List<WasReloadResult> results = request.getInstanceIds().stream()
                .map(instanceId -> executeReloadForInstance(instanceId, reloadType, additionalParams))
                .toList();

        return ReloadResultResponse.builder()
                .reloadType(request.getReloadType())
                .results(results)
                .build();
    }

    private WasReloadResult executeReloadForInstance(
            String instanceId, ReloadType reloadType, Map<String, String> additionalParams) {
        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(instanceId);
        if (instance == null) {
            return WasReloadResult.builder()
                    .instanceId(instanceId)
                    .success(false)
                    .errorMessage(instanceId + " 인스턴스를 찾을 수 없습니다.")
                    .build();
        }

        String managementIp = resolveManagementProperty(instanceId, "MANAGEMENT_SERVER_IP", defaultManagementIp);
        int managementPort = resolveManagementPort(instanceId);
        // 기본값 HTTP — biz-auth 처럼 TCP 전용 WAS는 FWK_PROPERTY에 COMM_TYPE=TCP 로 등록한다
        String commType = resolveManagementProperty(instanceId, "COMM_TYPE", "HTTP");

        Map<String, String> body = new HashMap<>();
        body.put("gubun", reloadType.getCode());
        if (!additionalParams.isEmpty()) {
            body.putAll(additionalParams);
        }

        log.info(
                "Reload 요청: instanceId={}, commType={}, host={}:{}, gubun={}, params={}",
                instanceId,
                commType,
                managementIp,
                managementPort,
                reloadType.getCode(),
                additionalParams);

        if ("TCP".equalsIgnoreCase(commType)) {
            return executeReloadViaTcp(instanceId, instance, managementIp, managementPort, body);
        }
        String url = String.format("http://%s:%d%s", managementIp, managementPort, managementEndpoint);
        return executeReloadViaHttp(instanceId, instance, managementIp, managementPort, url, body);
    }

    /**
     * TCP 채널로 {@code MANAGEMENT_RELOAD} 커맨드를 전송한다.
     *
     * <p>biz-auth처럼 HTTP 서버가 없는 WAS(web-application-type=none)에서 사용한다.
     * payload에 gubun 및 추가 파라미터를 담아 전송하며,
     * spider-link의 {@code ManagementReloadCommandHandler}가 수신하여 처리한다.</p>
     */
    private WasReloadResult executeReloadViaTcp(
            String instanceId,
            WasInstanceResponse instance,
            String managementIp,
            int managementPort,
            Map<String, String> body) {
        try {
            Map<String, Object> tcpPayload = new HashMap<>(body);
            JsonCommandRequest tcpReq = JsonCommandRequest.builder()
                    .command("MANAGEMENT_RELOAD")
                    .payload(tcpPayload)
                    .build();
            JsonCommandResponse tcpResp = tcpClient.sendJson(managementIp, managementPort, tcpReq);

            if (tcpResp.isSuccess()) {
                log.info("Reload(TCP) 성공: instanceId={}", instanceId);
                return WasReloadResult.builder()
                        .instanceId(instanceId)
                        .instanceName(instance.getInstanceName())
                        .success(true)
                        .build();
            }
            String errorMsg = tcpResp.getError() != null ? tcpResp.getError() : tcpResp.getMessage();
            log.warn("Reload(TCP) 실패 응답: instanceId={}, error={}", instanceId, errorMsg);
            return WasReloadResult.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();

        } catch (IOException e) {
            String errorMsg = String.format(
                    "%s TCP 서버에 연결 중 오류가 발생하였습니다.[host=%s,port=%d]", instanceId, managementIp, managementPort);
            log.error("Reload(TCP) 통신 오류: {}", errorMsg, e);
            return WasReloadResult.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        }
    }

    /**
     * HTTP로 관리 엔드포인트({@code /api/management/reload})에 POST 요청을 전송한다.
     */
    private WasReloadResult executeReloadViaHttp(
            String instanceId,
            WasInstanceResponse instance,
            String managementIp,
            int managementPort,
            String url,
            Map<String, String> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, httpEntity, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object successObj = responseBody.get("success");
                boolean success = successObj instanceof Boolean b && b;
                String message =
                        responseBody.get("message") != null ? String.valueOf(responseBody.get("message")) : null;

                if (success) {
                    log.info("Reload(HTTP) 성공: instanceId={}", instanceId);
                    return WasReloadResult.builder()
                            .instanceId(instanceId)
                            .instanceName(instance.getInstanceName())
                            .success(true)
                            .build();
                }
                log.warn("Reload(HTTP) 실패 응답: instanceId={}, message={}", instanceId, message);
                return WasReloadResult.builder()
                        .instanceId(instanceId)
                        .instanceName(instance.getInstanceName())
                        .success(false)
                        .errorMessage(message)
                        .build();
            }

            return WasReloadResult.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage("응답 상태: " + response.getStatusCode())
                    .build();

        } catch (RestClientException e) {
            String errorMsg = String.format(
                    "%s 서버에 연결 중 오류가 발생하였습니다.[host=%s,port=%d]", instanceId, managementIp, managementPort);
            log.error("Reload(HTTP) 통신 오류: {}", errorMsg, e);
            return WasReloadResult.builder()
                    .instanceId(instanceId)
                    .instanceName(instance.getInstanceName())
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        }
    }

    private int resolveManagementPort(String instanceId) {
        String value = resolveManagementProperty(instanceId, "MANAGEMENT_SERVER_PORT", null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.debug("Management port 파싱 실패, 기본값 사용: instanceId={}", instanceId, e);
            }
        }
        return defaultManagementPort;
    }

    /**
     * FWK_PROPERTY 테이블에서 management 통신 설정 조회
     * key: (was_config, {instanceId}.{suffix})
     */
    private String resolveManagementProperty(String instanceId, String suffix, String defaultValue) {
        try {
            String propertyId = instanceId + "." + suffix;
            PropertyResponse property = propertyMapper.selectResponseById(propertyGroup, propertyId);
            if (property != null
                    && property.getDefaultValue() != null
                    && !property.getDefaultValue().isBlank()) {
                return property.getDefaultValue();
            }
        } catch (Exception e) {
            log.warn("Management property 조회 실패, 기본값 사용: instanceId={}, suffix={}", instanceId, suffix, e);
        }
        return defaultValue;
    }
}
