package com.example.spider_admin.domain.proxyresponse.service;

import com.example.spider_admin.domain.message.dto.*;
import com.example.spider_admin.domain.message.mapper.MessageMapper;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messagefield.util.MessageFieldValidator;
import com.example.spider_admin.domain.messageparsing.dto.MessageResponse;
import com.example.spider_admin.domain.messageparsing.dto.ParsedFieldResponse;
import com.example.spider_admin.domain.messageparsing.util.MessageParser;
import com.example.spider_admin.domain.proxyresponse.dto.*;
import com.example.spider_admin.domain.proxyresponse.dto.ProxySettingSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataCreateRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataTrxSearchRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataUpdateRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyValueUpdateRequest;
import com.example.spider_admin.domain.proxyresponse.mapper.ProxyTestdataMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import com.example.spider_admin.global.util.SecurityUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MessageTest Service Implementation
 * 당발 대응답 관리 비즈니스 로직 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProxyTestdataService {

    private final ProxyTestdataMapper proxyTestdataMapper;
    private final MessageMapper messageMapper;

    public PageResponse<ProxyTestdataListResponse> getMessageTestsWithSearch(
            PageRequest pageRequest, ProxyTestdataSearchRequest searchDTO) {

        long total = proxyTestdataMapper.countAllWithSearch(
                searchDTO.getOrgIdFilter(),
                searchDTO.getTrxIdFilter(),
                searchDTO.getTestNameFilter(),
                searchDTO.getUserIdFilter(),
                "I");

        List<ProxyTestdataListResponse> messageTests = proxyTestdataMapper.findAllWithSearch(
                searchDTO.getOrgIdFilter(),
                searchDTO.getTrxIdFilter(),
                searchDTO.getTestNameFilter(),
                searchDTO.getUserIdFilter(),
                "I",
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(messageTests, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportProxyTestdata(
            String orgIdFilter,
            String trxIdFilter,
            String testNameFilter,
            String userIdFilter,
            String sortBy,
            String sortDirection) {
        List<ProxyTestdataListResponse> data = proxyTestdataMapper.findAllForExport(
                orgIdFilter, trxIdFilter, testNameFilter, userIdFilter, "I", sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("일련번호", 8, "testSno"),
                new ExcelColumnDefinition("기관ID", 10, "orgId"),
                new ExcelColumnDefinition("기관명", 20, "orgName"),
                new ExcelColumnDefinition("거래ID", 15, "trxId"),
                new ExcelColumnDefinition("테스트명", 25, "testName"),
                new ExcelColumnDefinition("테스트설명", 30, "testDesc"),
                new ExcelColumnDefinition("등록자", 15, "userId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (ProxyTestdataListResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("testSno", item.getTestSno());
            row.put("orgId", item.getOrgId());
            row.put("orgName", item.getOrgName());
            row.put("trxId", item.getTrxId());
            row.put("testName", item.getTestName());
            row.put("testDesc", item.getTestDesc());
            row.put("userId", item.getUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("당발대응답", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public List<ProxyTestdataTrxSearchResponse> searchTrxMessages(ProxyTestdataTrxSearchRequest searchDTO) {
        return proxyTestdataMapper.findTrxMessagesWithSearch(
                searchDTO.getOrgId(), searchDTO.getTrxId(), searchDTO.getTrxName(), "I");
    }

    public List<ProxyTestdataFieldResponse> getFieldsForTest(String orgId, String trxId) {
        log.debug("테스트용 전문 필드 조회 (기관+표준 JOIN): orgId={}, trxId={}", orgId, trxId);

        // 1. FWK_TRX_MESSAGE에서 MESSAGE_ID, STD_MESSAGE_ID 조회
        ProxyTestdataMsgIdsResponse msgIdsDTO = proxyTestdataMapper.findTrxMessageIds(orgId, trxId, "I");
        if (msgIdsDTO == null || msgIdsDTO.getMessageId() == null) {
            throw new NotFoundException("messageId: " + orgId + "/" + trxId);
        }

        String messageId = msgIdsDTO.getMessageId();
        String stdMessageId = msgIdsDTO.getStdMessageId() == null ? "" : msgIdsDTO.getStdMessageId();
        log.debug("거래-전문 매핑: messageId={}, stdMessageId={}", messageId, stdMessageId);

        // 2. 부모 체인 탐색 (부모 → 자식 순서)
        List<String> messageIdChain = buildMessageIdChain(orgId, messageId);

        // 3. 각 전문별로 기관+표준 필드 조회 후 makeRealValue 적용 (부모부터 순차 처리)
        List<ProxyTestdataFieldResponse> allFields = new ArrayList<>();
        for (String currentMessageId : messageIdChain) {
            List<ProxyTestdataFieldResponse> fields =
                    proxyTestdataMapper.findFieldsWithStd(orgId, currentMessageId, stdMessageId);

            if (fields != null) {
                for (ProxyTestdataFieldResponse field : fields) {
                    makeRealValue(field);
                    allFields.add(field);
                }
            }
        }

        log.info("테스트용 전문 필드 조회 완료: 총 {}건 (전문 {}개 체인)", allFields.size(), messageIdChain.size());
        return allFields;
    }

    /**
     * 부모 전문 체인을 재귀적으로 조회합니다 (부모 → 자식 순서).
     *
     * <p>Recursive CTE를 사용하여 한 번의 쿼리로 전체 체인을 조회합니다.
     * 최상위 부모부터 시작 전문까지 순서대로 반환되며,
     * 순환 참조가 있을 경우 중복 제거하여 반환합니다.</p>
     *
     * @param orgId     기관 ID
     * @param messageId 시작 전문 ID (자식)
     * @return 부모 → 자식 순서의 messageId 목록 (최상위 부모부터)
     */
    private List<String> buildMessageIdChain(String orgId, String messageId) {
        // Recursive CTE로 한 번에 체인 조회
        List<String> chain = proxyTestdataMapper.findMessageIdChain(orgId, messageId);

        if (chain == null || chain.isEmpty()) {
            log.warn("전문 체인 조회 실패: orgId={}, messageId={}", orgId, messageId);
            return List.of(messageId); // 최소한 시작 messageId는 반환
        }

        // 순환 참조 체크 (중복 검증)
        Set<String> uniqueCheck = new LinkedHashSet<>(chain);
        if (uniqueCheck.size() != chain.size()) {
            log.warn(
                    "순환 참조 감지 및 제거: orgId={}, messageId={}, 원본크기={}, 중복제거후={}",
                    orgId,
                    messageId,
                    chain.size(),
                    uniqueCheck.size());
            return new ArrayList<>(uniqueCheck);
        }

        log.debug("전문 체인 조회 완료: orgId={}, messageId={}, 체인깊이={}", orgId, messageId, chain.size());
        return chain;
    }

    /**
     * 기관/표준 전문의 USE_MODE, DEFAULT_VALUE에 따라 화면에 표시할 실제 값을 결정합니다.
     *
     * <p>레거시 SimpleTestcaseA.makeRealValue() 로직을 포팅한 것입니다.</p>
     *
     * <h4>결정 규칙:</h4>
     * <ol>
     *   <li>기관/표준 모두 USE_MODE='S' 이거나 DEFAULT_VALUE가 있는 경우:
     *     <ul>
     *       <li>표준 USE_MODE='S' → 표준 DEFAULT_VALUE 사용 (시스템 키워드 우선)</li>
     *       <li>기관 USE_MODE='S' → 기관 DEFAULT_VALUE 사용</li>
     *     </ul>
     *   </li>
     *   <li>그 외: 시스템 키워드(_$) 확인 후, DEFAULT_VALUE 또는 빈 값</li>
     * </ol>
     *
     * <h4>시스템 키워드:</h4>
     * <ul>
     *   <li>{@code _$MAP}: 매핑 키워드 - 수정 가능, 표시됨</li>
     *   <li>{@code _$}: 일반 시스템 키워드 - 읽기 전용, 숨김</li>
     * </ul>
     */
    private void makeRealValue(ProxyTestdataFieldResponse field) {
        String orgUseMode = nvl(field.getOrgUseMode());
        String stdUseMode = nvl(field.getStdUseMode());
        String orgDefault = nvl(field.getOrgDefaultValue());
        String stdDefault = nvl(field.getStdDefaultValue());

        FieldValueRequest fieldValue;

        boolean orgHasModeOrDefault = "S".equals(orgUseMode) || !orgDefault.isEmpty();
        boolean stdHasModeOrDefault = "S".equals(stdUseMode) || !stdDefault.isEmpty();

        if (orgHasModeOrDefault && stdHasModeOrDefault) {
            // 기관/표준 모두 USE_MODE='S' 이거나 DEFAULT_VALUE 존재
            fieldValue = "S".equals(stdUseMode)
                    ? resolveStandardValue(stdDefault, orgDefault)
                    : resolveOrganizationValue(orgDefault);
        } else {
            // 기관/표준 중 하나만 또는 둘 다 없는 경우
            fieldValue = resolveDefaultValue(orgDefault, stdDefault);
        }

        // TEST_VALUE가 있으면 realValue를 덮어씀 (기존 테스트 값 우선)
        String testValue = nvl(field.getTestValue());
        if (!testValue.isEmpty() && !fieldValue.readOnly) {
            fieldValue.realValue = testValue;
            fieldValue.comment = "";
        }

        field.setRealValue(fieldValue.realValue);
        field.setReadOnly(fieldValue.readOnly);
        field.setHidden(fieldValue.hidden);
        field.setValueComment(fieldValue.comment);
    }

    /**
     * 표준 전문 USE_MODE='S'일 때 값을 결정합니다.
     */
    private FieldValueRequest resolveStandardValue(String stdDefault, String orgDefault) {
        if (stdDefault.contains("_$MAP")) {
            return new FieldValueRequest(stdDefault, false, false, "전문 시스템 키워드");
        } else if (stdDefault.contains("_$")) {
            return new FieldValueRequest(stdDefault, true, true, "전문 시스템 키워드");
        } else if (!stdDefault.isEmpty()) {
            return new FieldValueRequest(stdDefault, false, true, "표준 전문 초기값");
        } else {
            return new FieldValueRequest(orgDefault, false, true, "기관 전문 초기값");
        }
    }

    /**
     * 기관 전문 USE_MODE='S'일 때 값을 결정합니다.
     */
    private FieldValueRequest resolveOrganizationValue(String orgDefault) {
        if (orgDefault.contains("_$MAP")) {
            return new FieldValueRequest(orgDefault, false, false, "시스템 키워드");
        } else if (orgDefault.contains("_$")) {
            return new FieldValueRequest(orgDefault, true, true, "시스템 키워드");
        } else {
            return new FieldValueRequest(orgDefault, false, true, "기관 전문 초기값");
        }
    }

    /**
     * 기본값을 결정합니다 (기관/표준 중 하나만 있거나 둘 다 없는 경우).
     */
    private FieldValueRequest resolveDefaultValue(String orgDefault, String stdDefault) {
        // 시스템 키워드 우선 확인
        if (orgDefault.contains("_$MAP")) {
            return new FieldValueRequest(orgDefault, false, false, "시스템 키워드");
        } else if (orgDefault.contains("_$")) {
            return new FieldValueRequest(orgDefault, true, true, "시스템 키워드");
        } else if (stdDefault.contains("_$")) {
            return new FieldValueRequest(stdDefault, true, true, "전문 시스템 키워드");
        } else if (stdDefault.contains("_$MAP")) {
            return new FieldValueRequest(stdDefault, false, false, "시스템 키워드");
        }

        // 일반 기본값 처리
        if (!orgDefault.isEmpty() && !stdDefault.isEmpty()) {
            return new FieldValueRequest(stdDefault, false, false, "표준 전문 초기값");
        } else if (!orgDefault.isEmpty()) {
            return orgDefault.contains("_$")
                    ? new FieldValueRequest(orgDefault, true, true, "시스템 키워드")
                    : new FieldValueRequest(orgDefault, false, false, "기관 전문 초기값");
        } else if (!stdDefault.isEmpty()) {
            return new FieldValueRequest(stdDefault, false, false, "표준 전문 초기값");
        }

        // 둘 다 없으면 빈 값
        return new FieldValueRequest("", false, false, "");
    }

    private static String nvl(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 필드 값 및 메타데이터를 담는 내부 클래스
     */
    private static class FieldValueRequest {
        String realValue;
        boolean readOnly;
        boolean hidden;
        String comment;

        FieldValueRequest(String realValue, boolean readOnly, boolean hidden, String comment) {
            this.realValue = realValue;
            this.readOnly = readOnly;
            this.hidden = hidden;
            this.comment = comment;
        }
    }

    public ProxyTestdataDetailResponse getMessageTestDetail(Long testSno) {
        ProxyTestdataDetailResponse detail = proxyTestdataMapper.findDetailByTestSno(testSno);
        if (detail == null) {
            throw new NotFoundException("testSno: " + testSno);
        }
        return detail;
    }

    @Transactional
    public void create(ProxyTestdataCreateRequest dto) {
        // 필드값 리스트 기반 검증 (우선 실행 - 구체적인 에러 메시지 제공)
        if (dto.getFieldValues() != null && !dto.getFieldValues().isEmpty()) {
            List<ProxyTestdataFieldResponse> fields = getFieldsForTest(dto.getOrgId(), dto.getTrxId());
            MessageFieldValidator.validateFieldValues(fields, dto.getFieldValues());
        }

        // 통전문 기반 필드 검증 (기존 검증 유지)
        validateRawMessage(dto.getOrgId(), dto.getTrxId(), dto.getTestData());

        String currentUserId = SecurityUtil.getCurrentUserIdOrSystem();
        proxyTestdataMapper.insertMessageTest(dto, currentUserId);
    }

    @Transactional
    public void update(ProxyTestdataUpdateRequest dto) {
        // 존재 여부 확인
        ProxyTestdataDetailResponse detail = proxyTestdataMapper.findDetailByTestSno(dto.getTestSno());
        if (detail == null) {
            throw new NotFoundException("testSno: " + dto.getTestSno());
        }

        // 필드값 리스트 기반 검증 (우선 실행 - 구체적인 에러 메시지 제공)
        if (dto.getFieldValues() != null && !dto.getFieldValues().isEmpty()) {
            List<ProxyTestdataFieldResponse> fields = getFieldsForTest(detail.getOrgId(), detail.getTrxId());
            MessageFieldValidator.validateFieldValues(fields, dto.getFieldValues());
        }

        // 통전문 기반 필드 검증 (기존 검증 유지)
        if (dto.getTestData() != null && !dto.getTestData().isBlank()) {
            validateRawMessage(detail.getOrgId(), detail.getTrxId(), dto.getTestData());
        }

        String currentUserId = SecurityUtil.getCurrentUserIdOrSystem();
        proxyTestdataMapper.updateMessageTest(dto, currentUserId);
    }

    /**
     * 통전문(raw message)을 파싱하여 필드 메타데이터 기반으로 검증합니다.
     *
     * <p>통전문을 MessageParser로 파싱한 후, 필드 메타데이터와 매칭하여
     * 필수 필드 누락 및 데이터 길이 초과를 검증합니다.
     * 검증 통과 시 통전문 원본이 그대로 DB에 저장됩니다.</p>
     *
     * @param orgId      기관 ID
     * @param trxId      거래 ID
     * @param rawMessage 통전문 문자열
     */
    private void validateRawMessage(String orgId, String trxId, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        // 1. 필드 메타데이터 조회 (makeRealValue 적용 완료 상태)
        List<ProxyTestdataFieldResponse> fields = getFieldsForTest(orgId, trxId);

        // 2. MESSAGE_ID 조회
        ProxyTestdataMsgIdsResponse msgIdsDTO = proxyTestdataMapper.findTrxMessageIds(orgId, trxId, "I");
        if (msgIdsDTO == null || msgIdsDTO.getMessageId() == null) {
            throw new NotFoundException("messageId: " + orgId + "/" + trxId);
        }
        String messageId = msgIdsDTO.getMessageId();

        // 3. 부모 체인 포함 전체 필드 목록 조회 (파싱용, 부모 → 자식 순서)
        List<String> messageIdChain = buildMessageIdChain(orgId, messageId);
        List<MessageFieldResponse> allMessageFields = new ArrayList<>();
        for (String currentMessageId : messageIdChain) {
            List<MessageFieldResponse> messageFields = messageMapper.findFieldsByMessageId(orgId, currentMessageId);
            if (messageFields != null) {
                allMessageFields.addAll(messageFields);
            }
        }

        // 4. 통전문 파싱
        List<ParsedFieldResponse> parsedFields = MessageParser.parseMessage(rawMessage, allMessageFields);

        // 5. 파싱 결과를 필드 메타데이터 기반으로 검증
        MessageFieldValidator.validateParsedFields(fields, parsedFields);

        log.info("통전문 검증 완료: orgId={}, trxId={}, rawLength={}", orgId, trxId, rawMessage.length());
    }

    @Transactional
    public void delete(Long testSno, String testGroupId) {
        ProxyTestdataDetailResponse detail = proxyTestdataMapper.findDetailByTestSno(testSno);
        if (detail == null) {
            throw new NotFoundException("testSno: " + testSno);
        }

        proxyTestdataMapper.deleteMessageTest(testSno, testGroupId);
    }

    public List<ProxySettingListResponse> getProxySettings(ProxySettingSearchRequest searchDTO) {
        return proxyTestdataMapper.findProxySettings(
                searchDTO.getOrgId(),
                searchDTO.getTrxId(),
                searchDTO.getTestGroupId(),
                searchDTO.getTestName(),
                searchDTO.getUserId());
    }

    public ProxySettingListResponse getDefaultProxy(String orgId, String trxId, String testGroupId) {
        return proxyTestdataMapper.findDefaultProxy(orgId, trxId, testGroupId);
    }

    @Transactional
    public void updateProxyField(String orgId, String trxId, String testGroupId, String proxyField) {
        String currentUserId = SecurityUtil.getCurrentUserIdOrSystem();
        proxyTestdataMapper.updateProxyField(orgId, trxId, testGroupId, proxyField, currentUserId);
    }

    public int countByProxyValue(String orgId, String trxId, String testGroupId, String proxyValue) {
        return proxyTestdataMapper.countByProxyValue(orgId, trxId, testGroupId, proxyValue);
    }

    @Transactional
    public void updateProxyValue(ProxyValueUpdateRequest dto) {
        String currentUserId = SecurityUtil.getCurrentUserIdOrSystem();
        log.info("updateProxyValue *******************");

        // 1단계: 기존 동일 PROXY_VALUE 초기화
        proxyTestdataMapper.clearProxyValue(dto.getOrgId(), dto.getTrxId(), dto.getTestGroupId(), dto.getProxyValue());
        log.info("초기화 성공 *******************");
        // 2단계: 새 PROXY_VALUE 설정
        proxyTestdataMapper.updateProxyValue(
                dto.getOrgId(),
                dto.getTrxId(),
                dto.getTestSno(),
                dto.getTestGroupId(),
                dto.getProxyValue(),
                currentUserId,
                dto.getUserId(),
                dto.getTestName());
        log.info("값 설정 성공 *******************");
    }

    @Transactional
    public void setDefaultProxy(String orgId, String trxId, String testGroupId, Long testSno) {
        String currentUserId = SecurityUtil.getCurrentUserIdOrSystem();

        // 1단계: 기존 기본 대응답 초기화
        proxyTestdataMapper.clearDefaultProxy(orgId, trxId, testGroupId);

        // 2단계: 새 기본 대응답 설정
        proxyTestdataMapper.setDefaultProxy(orgId, trxId, testSno, testGroupId, currentUserId);
    }

    @Transactional
    public void clearDefaultProxy(String orgId, String trxId, String testGroupId) {
        proxyTestdataMapper.clearDefaultProxy(orgId, trxId, testGroupId);
    }

    public MessageParseResponse parseRawMessage(String orgId, String messageId, String rawString) {
        log.debug(
                "통전문 파싱 (부모 체인 포함): orgId={}, messageId={}, rawLength={}",
                orgId,
                messageId,
                rawString != null ? rawString.length() : 0);

        MessageResponse message = messageMapper.selectResponseById(orgId, messageId);
        if (message == null) {
            throw new NotFoundException("messageId: " + orgId + "/" + messageId);
        }

        // 부모 체인을 따라 전체 필드 목록 조회 (최상위 부모 → 자식 순서)
        List<String> messageIdChain = buildMessageIdChain(orgId, messageId);
        List<MessageFieldResponse> allFields = new ArrayList<>();
        for (String currentMessageId : messageIdChain) {
            List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, currentMessageId);
            if (fields != null) {
                allFields.addAll(fields);
            }
        }

        if (allFields.isEmpty()) {
            throw new InvalidInputException(
                    String.format("orgId: %s, messageId: %s - %s", orgId, messageId, "전문에 필드가 정의되어 있지 않습니다"));
        }

        // MessageParser를 이용한 파싱
        List<ParsedFieldResponse> parsedFields = MessageParser.parseMessage(rawString, allFields);

        log.debug("통전문 파싱 완료: 총 {}건 (전문 {}개 체인)", parsedFields.size(), messageIdChain.size());

        return MessageParseResponse.builder()
                .orgId(orgId)
                .messageId(messageId)
                .messageName(message.getMessageName())
                .messageDesc(message.getMessageDesc())
                .rawString(rawString)
                .totalLength(MessageParser.calculateByteLength(rawString))
                .fields(parsedFields)
                .build();
    }

    public List<String> getGroupIds(String orgId, String trxId) {
        return proxyTestdataMapper.findGroupIds(orgId, trxId);
    }
}
