package com.example.spider_admin.domain.messagetest.service;

import com.example.spider_admin.domain.message.mapper.MessageMapper;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageSimulationRequest;
import com.example.spider_admin.domain.messageparsing.dto.MessageSimulationResponse;
import com.example.spider_admin.domain.messagetest.config.MessageTestProperties;
import com.example.spider_admin.domain.messagetest.dto.MessageFieldForTestResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageTestCreateRequest;
import com.example.spider_admin.domain.messagetest.dto.MessageTestResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageTestUpdateRequest;
import com.example.spider_admin.domain.messagetest.mapper.MessageTestMapper;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageWithTrxResponse;
import com.example.spider_admin.domain.trxmessage.mapper.TrxMessageMapper;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.service.WasInstanceService;
import com.example.spider_admin.global.exception.ErrorType;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.exception.base.BaseException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.SecurityUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * 전문 테스트 Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageTestService {

    private final MessageTestMapper messageTestMapper;
    private final MessageMapper messageMapper;
    private final TrxMessageMapper trxMessageMapper;
    private final WasInstanceService wasInstanceService;
    private final RestTemplate restTemplate;
    private final MessageTestProperties messageTestProperties;

    @Transactional
    public MessageTestResponse createTestCase(MessageTestCreateRequest requestDTO) {
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        messageTestMapper.insertMessageTest(requestDTO, now, currentUserId);

        return messageTestMapper.selectResponseByTestSno(requestDTO.getTestSno());
    }

    @Transactional
    public MessageTestResponse updateTestCase(Long testSno, MessageTestUpdateRequest requestDTO) {
        // 기존 데이터 조회
        MessageTestResponse existing = messageTestMapper.selectResponseByTestSno(testSno);
        if (existing == null) {
            throw new NotFoundException("testSno: " + testSno);
        }

        // 권한 확인 (본인 것만 수정 가능)
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (!existing.getUserId().equals(currentUserId)) {
            throw new BaseException(
                    ErrorType.FORBIDDEN, String.format("testSno: %d, userId: %s", testSno, currentUserId));
        }

        // 업데이트
        String now = AuditUtil.now();
        requestDTO.setTestSno(testSno);
        messageTestMapper.updateMessageTest(requestDTO, now, currentUserId);

        return messageTestMapper.selectResponseByTestSno(testSno);
    }

    @Transactional
    public void deleteTestCase(Long testSno) {
        // 기존 데이터 조회
        MessageTestResponse existing = messageTestMapper.selectResponseByTestSno(testSno);
        if (existing == null) {
            throw new NotFoundException("testSno: " + testSno);
        }

        // 권한 확인 (본인 것만 삭제 가능)
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (!existing.getUserId().equals(currentUserId)) {
            throw new BaseException(
                    ErrorType.FORBIDDEN, String.format("testSno: %d, userId: %s", testSno, currentUserId));
        }

        messageTestMapper.deleteByTestSno(testSno);
    }

    public List<MessageTestResponse> getMyTestCases() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return messageTestMapper.findByUserId(currentUserId);
    }

    public List<MessageTestResponse> getTestCasesByTrxId(
            String trxId, String headerYn, String testName, String testData, String userId) {
        if ((testName != null && !testName.isEmpty())
                || (testData != null && !testData.isEmpty())
                || (userId != null && !userId.isEmpty())) {
            return messageTestMapper.findByMessageIdWithFilters(trxId, headerYn, testName, testData, userId);
        } else {
            return messageTestMapper.findByMessageId(trxId, headerYn);
        }
    }

    public List<MessageFieldForTestResponse> getFieldsForTest(String trxId, String ioType) {
        // 1. 거래-메시지 매핑 목록 조회 (레거시: 거래에 매핑된 모든 메시지)
        List<TrxMessageWithTrxResponse> trxMessages = trxMessageMapper.findAllByTrxIdAndIoTypeWithTrx(trxId, ioType);
        if (trxMessages == null || trxMessages.isEmpty()) {
            throw new NotFoundException("거래에 대한 메시지 매핑을 찾을 수 없습니다: trxId=" + trxId + ", ioType=" + ioType);
        }

        List<MessageFieldResponse> allFields = new ArrayList<>();
        boolean headerAdded = false;

        // 2. 각 메시지의 필드 조회
        for (TrxMessageWithTrxResponse trxMessage : trxMessages) {
            String orgId = trxMessage.getOrgId();
            String messageId = trxMessage.getMessageId();

            // 메시지 조회 (헤더 정보 확인용)
            MessageResponse message = messageMapper.selectResponseById(orgId, messageId);
            if (message == null) {
                log.warn("메시지를 찾을 수 없습니다: orgId={}, messageId={}", orgId, messageId);
                continue;
            }

            // 헤더 메시지 필드 조회 (최초 1회만 추가)
            if (!headerAdded
                    && message.getParentMessageId() != null
                    && !message.getParentMessageId().isEmpty()) {
                allFields.addAll(collectHeaderFields(orgId, message.getParentMessageId()));
                headerAdded = true;
            }

            // 바디 메시지 필드 조회
            List<MessageFieldResponse> bodyFields = messageMapper.findFieldsByMessageId(orgId, messageId);
            if (bodyFields != null) {
                allFields.addAll(bodyFields);
            }
        }

        // 3. DTO 변환 (sortOrder 순서는 이미 정렬되어 있음)
        return allFields.stream().map(this::fieldToTestDTO).toList();
    }

    private List<MessageFieldResponse> collectHeaderFields(String orgId, String parentMessageId) {
        List<MessageFieldResponse> headerFields = new ArrayList<>();
        List<String> headerChain = new ArrayList<>();
        String currentParentId = parentMessageId;

        while (currentParentId != null && !currentParentId.isEmpty()) {
            headerChain.add(currentParentId);
            MessageResponse parentMessage = messageMapper.selectResponseById(orgId, currentParentId);
            if (parentMessage == null) {
                break;
            }
            currentParentId = parentMessage.getParentMessageId();
        }

        java.util.Collections.reverse(headerChain);
        for (String headerId : headerChain) {
            List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, headerId);
            if (fields != null) {
                headerFields.addAll(fields);
            }
        }
        return headerFields;
    }

    /**
     * MessageFieldResponse를 테스트용 DTO로 변환
     */
    private MessageFieldForTestResponse fieldToTestDTO(MessageFieldResponse field) {
        String testValue = field.getTestValue() != null ? field.getTestValue() : "";
        String defaultValue = field.getDefaultValue() != null ? field.getDefaultValue() : "";

        // 시스템 키워드 판단: _$로 시작하는 값
        boolean isSystemKeyword = testValue.startsWith("_$") || defaultValue.startsWith("_$");

        return MessageFieldForTestResponse.builder()
                .messageId(field.getMessageId())
                .fieldId(field.getMessageFieldId())
                .fieldName(field.getMessageFieldName())
                .dataLength(field.getDataLength())
                .requiredYn(field.getRequiredYn() != null ? field.getRequiredYn() : "N")
                .testValue(testValue)
                .sortOrder(field.getSortOrder())
                .defaultValue(defaultValue)
                .isSystemKeyword(isSystemKeyword)
                .build();
    }

    @Transactional(readOnly = true)
    public MessageSimulationResponse runSimulation(MessageSimulationRequest requestDTO) {

        log.info(
                "Running simulation: orgId={}, trxId={}, instanceId={}",
                requestDTO.getOrgId(),
                requestDTO.getTrxId(),
                requestDTO.getInstanceId());

        try {
            // DataMap 형식으로 요청 데이터 구성
            Map<String, Object> dataMap = buildDataMap(requestDTO);

            // 1. 인스턴스 존재 여부 확인
            String instanceId = requestDTO.getInstanceId();
            WasInstanceResponse wasInstance = resolveInstance(instanceId);

            // 2. 인스턴스가 없으면 레거시와 동일한 오류 반환
            if (wasInstance == null) {
                return buildErrorResponse(
                        "속성값 Error : was_config.properties 파일에서 " + instanceId + ".SERVICE_SERVER_IP에 해당하는 값을 찾지 못했습니다",
                        dataMap);
            }

            // 3. 인스턴스 IP가 설정되지 않은 경우
            if (wasInstance.getIp() == null || wasInstance.getIp().trim().isEmpty()) {
                return buildErrorResponse("속성값 Error : " + instanceId + " 인스턴스의 IP 주소가 설정되지 않았습니다", dataMap);
            }

            // 4. 인스턴스 연결 테스트
            String connectionError = testInstanceConnection(wasInstance);
            if (connectionError != null) {
                return buildErrorResponse(connectionError, dataMap);
            }

            // 5. HTTP 요청 전송 및 응답 처리
            return sendSimulationRequest(wasInstance, dataMap);

        } catch (Exception e) {
            // 전체 스택 트레이스 로깅 (디버깅 정보 향상)
            log.error("Simulation failed: {}", e.getMessage(), e);

            // DataMap 형식으로 오류 정보 구성 (민감 정보 제외)
            Map<String, Object> dataMap = buildDataMap(requestDTO);
            return buildErrorResponse("시뮬레이션 실행 중 오류가 발생했습니다.", dataMap);
        }
    }

    /**
     * 인스턴스 ID로 WasInstance 조회 (없으면 null 반환)
     */
    private WasInstanceResponse resolveInstance(String instanceId) {
        try {
            return wasInstanceService.getInstanceById(instanceId);
        } catch (Exception e) {
            log.warn("Instance not found: {}", instanceId);
            return null;
        }
    }

    /**
     * 실패 응답을 생성하는 공통 헬퍼
     */
    private MessageSimulationResponse buildErrorResponse(String errorMessage, Map<String, Object> dataMap) {
        return MessageSimulationResponse.builder()
                .success(false)
                .request(errorMessage)
                .response(dataMap)
                .errorMessage(errorMessage)
                .errorDetails(dataMap)
                .build();
    }

    /**
     * 실제 시뮬레이션 HTTP 요청 전송 및 응답 처리
     */
    private MessageSimulationResponse sendSimulationRequest(
            WasInstanceResponse wasInstance, Map<String, Object> dataMap) {
        String instanceId = wasInstance.getInstanceId();

        // URL 구성: http://{ip}:{port}{path}
        String url = "http://" + wasInstance.getIp() + ":" + wasInstance.getPort()
                + messageTestProperties.getSimulation().getPath();

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // DataMap을 Form Data로 변환
        String formData = buildFormData(dataMap);
        HttpEntity<String> entity = new HttpEntity<>(formData, headers);

        try {
            // HTTP POST 요청 전송
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 응답 파싱 (레거시 응답 형식에 따라)
            Map<String, Object> responseMap = parseResponse(response.getBody());

            return MessageSimulationResponse.builder()
                    .success(true)
                    .request(dataMap)
                    .response(responseMap)
                    .build();

        } catch (org.springframework.web.client.HttpClientErrorException
                | org.springframework.web.client.HttpServerErrorException e) {
            // HTTP 오류 (4xx, 5xx)
            return buildErrorResponse(instanceId + " 서버 응답 오류가 발생했습니다.", dataMap);

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 연결 오류 (타임아웃, 연결 거부 등)
            return buildErrorResponse(instanceId + " 서버와 통신 중 오류가 발생했습니다.", dataMap);
        }
    }

    /**
     * WAS 인스턴스 연결 테스트
     * @param wasInstance WAS 인스턴스 정보
     * @return 오류 메시지 (연결 성공 시 null)
     */
    private String testInstanceConnection(WasInstanceResponse wasInstance) {
        String ip = wasInstance.getIp();
        String portStr = wasInstance.getPort();
        String instanceId = wasInstance.getInstanceId();

        // SSRF 방지: IP 유효성 검증
        if (!isValidIpAddress(ip)) {
            return "속성값 Error : " + instanceId + " 인스턴스의 IP 주소 형식이 올바르지 않습니다";
        }

        // SSRF 방지: 로컬호스트 및 내부망 IP만 허용
        if (!isAllowedIpForConnection(ip)) {
            return "속성값 Error : " + instanceId + " 인스턴스의 IP 주소로 연결할 수 없습니다";
        }

        // 포트가 설정되지 않은 경우
        if (portStr == null || portStr.trim().isEmpty()) {
            return "속성값 Error : " + instanceId + " 인스턴스의 PORT가 설정되지 않았습니다";
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            // 포트 범위 검증 (1-65535)
            if (port < 1 || port > 65535) {
                return "속성값 Error : " + instanceId + " 인스턴스의 PORT 범위가 올바르지 않습니다";
            }
        } catch (NumberFormatException e) {
            return "속성값 Error : " + instanceId + " 인스턴스의 PORT 형식이 올바르지 않습니다";
        }

        // Socket 연결 시도 (타임아웃 3초)
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), 3000);
            return null; // 연결 성공
        } catch (java.net.SocketTimeoutException e) {
            return instanceId + " 서버 연결 시간이 초과되었습니다.";
        } catch (Exception e) {
            return instanceId + " 서버와 연결시도중 오류가 발생하였습니다.";
        }
    }

    /**
     * IP 주소 형식 검증
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // IPv4 정규식
        String ipv4Pattern = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        return ip.matches(ipv4Pattern);
    }

    /**
     * SSRF 방지: 허용된 IP 범위 검증
     * 로컬호스트(127.0.0.1) 및 내부망(10.x.x.x, 172.16-31.x.x, 192.168.x.x)만 허용
     */
    private boolean isAllowedIpForConnection(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 로컬호스트 (127.0.0.0/8)
            if (first == 127) {
                return true;
            }

            // 사설 IP 대역
            // 10.0.0.0/8
            if (first == 10) {
                return true;
            }

            // 172.16.0.0/12 (172.16.0.0 ~ 172.31.255.255)
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }

            // 192.168.0.0/16
            if (first == 192 && second == 168) {
                return true;
            }

            // 그 외 IP는 보안상 차단
            return false;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 레거시 DataMap 형식으로 요청 데이터 구성
     */
    private Map<String, Object> buildDataMap(MessageSimulationRequest requestDTO) {

        Map<String, Object> dataMap = new LinkedHashMap<>();

        // 시스템 정보
        dataMap.put("orgId", requestDTO.getOrgId());
        dataMap.put("orgName", requestDTO.getOrgId());
        dataMap.put("_$TRX_ID", requestDTO.getTrxId());
        dataMap.put("trxName", requestDTO.getTrxId());
        dataMap.put("wasInstanceList", requestDTO.getInstanceId());
        dataMap.put("URI", "/ibsmgr/spider.admin.ap.message.test.ConnectorEmulatorA.web");
        dataMap.put("targetMethod", "executeTrx");
        dataMap.put("xmlYn", "N");
        dataMap.put("strLoop", "false");

        // 필드 데이터 처리
        populateFieldData(dataMap, requestDTO.getFieldData());

        // 기타 필드
        dataMap.put("LOG_FILE_CATEGORY", "");
        dataMap.put("testName", "");
        dataMap.put("testDesc", "");
        dataMap.put("testSno", "");

        return dataMap;
    }

    private void populateFieldData(Map<String, Object> dataMap, Map<String, Map<String, Object>> fieldData) {
        if (fieldData == null) {
            return;
        }

        List<String> fieldNameList = new ArrayList<>();
        fieldData.forEach((messageId, fields) -> fields.forEach((fieldId, value) -> {
            if (value instanceof List) {
                dataMap.put(fieldId, value);
                dataMap.put("strLoop", "true");
            } else {
                String strValue = value != null ? String.valueOf(value) : "";
                dataMap.put(fieldId, strValue.isEmpty() ? "" : strValue);
            }
            fieldNameList.add(fieldId);
        }));

        if (!fieldNameList.isEmpty()) {
            String fieldNameArray = "[" + String.join(",", fieldNameList) + ",]";
            dataMap.put("fieldNameArray", fieldNameArray);
        }
    }

    /**
     * DataMap을 application/x-www-form-urlencoded 형식으로 변환
     */
    @SuppressWarnings("unchecked")
    private String buildFormData(Map<String, Object> dataMap) {
        StringBuilder formData = new StringBuilder();

        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            Object value = entry.getValue();

            if (value instanceof List) {
                appendListValues(formData, key, (List<String>) value);
            } else {
                appendSingleValue(formData, key, String.valueOf(value));
            }
        }

        return formData.toString();
    }

    private void appendListValues(StringBuilder formData, String encodedKey, List<String> values) {
        for (String v : values) {
            if (!formData.isEmpty()) {
                formData.append("&");
            }
            formData.append(encodedKey)
                    .append("=")
                    .append(java.net.URLEncoder.encode(v != null ? v : "", StandardCharsets.UTF_8));
        }
    }

    private void appendSingleValue(StringBuilder formData, String encodedKey, String value) {
        if (!formData.isEmpty()) {
            formData.append("&");
        }
        formData.append(encodedKey).append("=").append(java.net.URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /**
     * 응답 문자열을 Map으로 파싱
     * 레거시 응답 형식: key=value 형식의 텍스트
     */
    private Map<String, Object> parseResponse(String responseBody) {
        Map<String, Object> responseMap = new LinkedHashMap<>();

        if (responseBody == null || responseBody.trim().isEmpty()) {
            responseMap.put("error", "응답 데이터가 없습니다");
            return responseMap;
        }

        // 레거시 응답 형식 파싱 (예: "key1=value1\nkey2=value2")
        String[] lines = responseBody.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();
                responseMap.put(key, value);
            } else {
                // key=value 형식이 아닌 경우 전체를 저장
                responseMap.put("rawResponse", line);
            }
        }

        // 파싱된 데이터가 없으면 원본 응답 저장
        if (responseMap.isEmpty()) {
            responseMap.put("rawResponse", responseBody);
        }

        return responseMap;
    }
}
