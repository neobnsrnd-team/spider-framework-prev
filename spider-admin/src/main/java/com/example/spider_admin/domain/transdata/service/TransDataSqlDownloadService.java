package com.example.spider_admin.domain.transdata.service;

import com.example.spider_admin.domain.transdata.dto.TransDataGenerationRequest;
import com.example.spider_admin.domain.transdata.dto.TransDataItemRequest;
import com.example.spider_admin.domain.transdata.mapper.TransDataSqlDownloadMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이행 SQL 파일 생성 서비스
 *
 * <p>ZIP 구조:
 * <pre>
 * {yyyyMMddHHmm}.zip
 *   └── {yyyyMMddHHmm}/
 *         └── {TYPE}@{ORG_ID}@{ID}@{IO_TYPE}_{userId}_{yyyyMMddHHmm}  (TRX)
 *         └── {TYPE}@{ID}_{userId}_{yyyyMMddHHmm}                      (기타)
 * </pre>
 *
 * <p>SQL 파일 섹션 구조 (TRX 기준):
 * <pre>
 *   ##이행회차##   → FWK_TRANS_DATA_TIMES INSERT
 *   ##이행이력##   → FWK_TRANS_DATA_HIS INSERT
 *   ##거래##       → FWK_TRX + FWK_TRX_MESSAGE DELETE/INSERT
 *   ##거래전문##   → FWK_MESSAGE + FWK_MESSAGE_FIELD (메시지별 반복)
 *   ##응답전문맵핑## → FWK_MESSAGE_FIELD_MAPPING DELETE/INSERT
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransDataSqlDownloadService {

    private final TransDataSqlDownloadMapper downloadMapper;

    // ===== yyyyMMddHHmmss (14자리) TRAN_SEQ 채번용
    private static final DateTimeFormatter SEQ_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    // ===== yyyyMMddHHmm  (12자리) 폴더/파일명용
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public String generateTimestamp() {
        return LocalDateTime.now().format(FILE_FMT);
    }

    public byte[] generateSqlZip(TransDataGenerationRequest request, String userId) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FILE_FMT);
        String tranSeq = now.format(SEQ_FMT);
        String tranReason = request.getTranReason() != null ? request.getTranReason() : "";

        boolean trxOnly = request.isTrxOnly();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (TransDataItemRequest item : request.getItems()) {
                String[] result = buildSqlFile(item, tranSeq, tranReason, userId, timestamp, trxOnly);
                String fileName = result[0];
                String sqlContent = result[1];

                ZipEntry entry = new ZipEntry(timestamp + "/" + fileName + ".sql");
                zos.putNextEntry(entry);
                zos.write(sqlContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                log.info("SQL 파일 생성 - {}", fileName);
            }
        }
        return baos.toByteArray();
    }

    // =========================================================================
    // 파일명 + 내용 생성 (타입별 분기)
    // =========================================================================

    /** @return [fileName, sqlContent] */
    private String[] buildSqlFile(
            TransDataItemRequest item,
            String tranSeq,
            String reason,
            String userId,
            String timestamp,
            boolean trxOnly) {
        return switch (item.getTranType().toUpperCase()) {
            case "TRX" -> buildTrxFile(item, tranSeq, reason, userId, timestamp, trxOnly);
            case "MESSAGE" -> buildMessageFile(item, tranSeq, reason, userId, timestamp);
            case "CODE" -> buildCodeFile(item, tranSeq, reason, userId, timestamp);
            case "SERVICE" -> buildServiceFile(item, tranSeq, reason, userId, timestamp);
            case "ERROR" -> buildErrorFile(item, tranSeq, reason, userId, timestamp);
            case "PROPERTY" -> buildPropertyFile(item, tranSeq, reason, userId, timestamp);
            case "COMPONENT" -> buildComponentFile(item, tranSeq, reason, userId, timestamp);
            case "WEBAPP" -> buildWebappFile(item, tranSeq, reason, userId, timestamp);
            default -> buildDefaultFile(item, tranSeq, reason, userId, timestamp);
        };
    }

    // =========================================================================
    // TRX 타입
    // =========================================================================

    private String[] buildTrxFile(
            TransDataItemRequest item,
            String tranSeq,
            String reason,
            String userId,
            String timestamp,
            boolean trxOnly) {
        String trxId = item.getTranId();

        // TRX_MESSAGE 먼저 조회 → 파일명용 orgId/ioType 결정
        List<Map<String, Object>> trxMessages = downloadMapper.selectTrxMessageByTrxId(trxId);

        String orgId = "UNK";
        String ioType = "I";
        for (Map<String, Object> msg : trxMessages) {
            if ("I".equals(str(msg.get("IO_TYPE")))) {
                orgId = str(msg.get("ORG_ID"));
                break;
            }
        }

        String fileName = "TRX@" + orgId + "@" + trxId + "@" + ioType + "_" + userId + "_" + timestamp;
        String sqlContent = buildTrxSqlContent(item, trxMessages, tranSeq, reason, userId, trxOnly);

        return new String[] {fileName, sqlContent};
    }

    private String buildTrxSqlContent(
            TransDataItemRequest item,
            List<Map<String, Object>> trxMessages,
            String tranSeq,
            String reason,
            String userId,
            boolean trxOnly) {
        String trxId = item.getTranId();
        String trxName = item.getTranName();

        StringBuilder sb = new StringBuilder();

        // ── ##이행회차## + ##이행이력## ────────────────────────────────────────
        appendTimesAndHis(sb, tranSeq, reason, userId, trxId, trxName, "TRX");

        // ── ##거래## ──────────────────────────────────────────────────────────
        sb.append("\n##거래##\n");

        // FWK_TRX
        List<Map<String, Object>> trxList = downloadMapper.selectTrxById(trxId);
        if (!trxList.isEmpty()) {
            sb.append("DELETE FROM FWK_TRX WHERE TRX_ID = '")
                    .append(escape(trxId))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_TRX", trxList.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        // FWK_TRX_MESSAGE + 관련 메시지 ID 수집
        // orgId → 메시지 ID 리스트 (순서: MESSAGE_ID, STD_MESSAGE_ID, RES_MESSAGE_ID, STD_RES_MESSAGE_ID)
        Map<String, Set<String>> orgMessageIds = new LinkedHashMap<>();
        // orgId → [trgMsgId, srcMsgId] (응답전문맵핑용)
        Map<String, String[]> mappingInfo = new LinkedHashMap<>();

        for (Map<String, Object> trxMsg : trxMessages) {
            String orgId = str(trxMsg.get("ORG_ID"));
            String msgIoType = str(trxMsg.get("IO_TYPE"));

            sb.append("DELETE FROM FWK_TRX_MESSAGE WHERE TRX_ID = '")
                    .append(escape(trxId))
                    .append("' AND ORG_ID = '")
                    .append(escape(orgId))
                    .append("' AND IO_TYPE = '")
                    .append(escape(msgIoType))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_TRX_MESSAGE", trxMsg)).append("\n");
            sb.append("COMMIT;\n");

            // IO_TYPE='I' 행에서 관련 메시지 ID 수집
            if (!trxOnly && "I".equals(msgIoType)) {
                collectRelatedMessageIds(trxMsg, orgId, orgMessageIds, mappingInfo);
            }
        }

        if (!trxOnly) {
            // ── ##거래전문## (메시지별) ───────────────────────────────────────────
            for (Map.Entry<String, Set<String>> entry : orgMessageIds.entrySet()) {
                String orgId = entry.getKey();
                for (String msgId : entry.getValue()) {
                    sb.append(buildMessageSection(orgId, msgId, reason));
                }
            }

            // ── ##응답전문맵핑## ──────────────────────────────────────────────────
            for (Map.Entry<String, String[]> entry : mappingInfo.entrySet()) {
                String orgId = entry.getKey();
                String trgMsgId = entry.getValue()[0]; // RES_MESSAGE_ID
                String srcMsgId = entry.getValue()[1]; // STD_RES_MESSAGE_ID
                sb.append(buildMappingSection(orgId, trgMsgId, orgId, srcMsgId));
            }
        }

        return sb.toString();
    }

    private void collectRelatedMessageIds(
            Map<String, Object> trxMsg,
            String orgId,
            Map<String, Set<String>> orgMessageIds,
            Map<String, String[]> mappingInfo) {
        Set<String> msgIds = orgMessageIds.computeIfAbsent(orgId, k -> new LinkedHashSet<>());
        addUnique(msgIds, str(trxMsg.get("MESSAGE_ID")));
        addUnique(msgIds, str(trxMsg.get("STD_MESSAGE_ID")));
        String resMsgId = str(trxMsg.get("RES_MESSAGE_ID"));
        String stdResMsgId = str(trxMsg.get("STD_RES_MESSAGE_ID"));
        addUnique(msgIds, resMsgId);
        addUnique(msgIds, stdResMsgId);

        if (resMsgId != null && stdResMsgId != null) {
            mappingInfo.put(orgId, new String[] {resMsgId, stdResMsgId});
        }
    }

    // =========================================================================
    // ##거래전문## 섹션
    // =========================================================================

    private String buildMessageSection(String orgId, String messageId, String reason) {
        return buildMessageSection(orgId, messageId, reason, "##거래전문##");
    }

    private String buildMessageSection(String orgId, String messageId, String reason, String sectionHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(sectionHeader).append("\n");

        // FWK_MESSAGE_HISTORY (INSERT ... SELECT 템플릿)
        sb.append(
                        "INSERT INTO FWK_MESSAGE_HISTORY (ORG_ID, MESSAGE_ID, VERSION, MESSAGE_NAME, MESSAGE_DESC, MESSAGE_TYPE, PARENT_MESSAGE_ID, HEADER_YN, REQUEST_YN, TRX_TYPE, PRE_LOAD_YN, LOG_LEVEL, BIZ_DOMAIN, VALIDATION_USE_YN, LOCK_YN, HISTORY_REASON, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID) ")
                .append("SELECT ORG_ID,MESSAGE_ID,")
                .append(
                        "(SELECT NVL(MAX(his.VERSION), 0) + 1 AS VERSION FROM FWK_MESSAGE_HISTORY his WHERE his.MESSAGE_ID = message_.MESSAGE_ID AND his.ORG_ID = message_.ORG_ID) AS VERSION, ")
                .append(
                        "MESSAGE_NAME,MESSAGE_DESC,MESSAGE_TYPE,PARENT_MESSAGE_ID,HEADER_YN,REQUEST_YN,TRX_TYPE,PRE_LOAD_YN,LOG_LEVEL,BIZ_DOMAIN,VALIDATION_USE_YN,LOCK_YN, '")
                .append(escape(reason))
                .append("' AS HISTORY_REASON,LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID ")
                .append("FROM FWK_MESSAGE message_ WHERE ORG_ID = '")
                .append(escape(orgId))
                .append("' AND MESSAGE_ID = '")
                .append(escape(messageId))
                .append("';\n");

        // FWK_MESSAGE DELETE + INSERT
        List<Map<String, Object>> messages = downloadMapper.selectMessageByOrgAndId(orgId, messageId);
        if (messages.isEmpty()) {
            return sb.toString();
        }

        sb.append("DELETE FROM FWK_MESSAGE WHERE ORG_ID = '")
                .append(escape(orgId))
                .append("' AND MESSAGE_ID = '")
                .append(escape(messageId))
                .append("'; \n");
        sb.append(toInsertSql("FWK_MESSAGE", messages.get(0))).append("\n");
        sb.append("COMMIT;\n");

        // FWK_MESSAGE_FIELD_HISTORY + DELETE + INSERT
        List<Map<String, Object>> fields = downloadMapper.selectMessageFieldByOrgAndId(orgId, messageId);
        if (!fields.isEmpty()) {
            sb.append(
                            "INSERT INTO FWK_MESSAGE_FIELD_HISTORY (ORG_ID, MESSAGE_ID, MESSAGE_FIELD_ID, VERSION, SORT_ORDER, DATA_TYPE, DATA_LENGTH, SCALE, ALIGN, FILLER, FIELD_TYPE, USE_MODE, REQUIRED_YN, FIELD_TAG, CODE_GROUP, DEFAULT_VALUE, TEST_VALUE, REMARK, LOG_YN, CODE_MAPPING_YN, MESSAGE_FIELD_NAME, MESSAGE_FIELD_DESC, LAST_UPDATE_DTIME, VALIDATION_RULE_ID, LAST_UPDATE_USER_ID) ")
                    .append("SELECT message_field_.ORG_ID,message_field_.MESSAGE_ID,MESSAGE_FIELD_ID,")
                    .append(
                            "(SELECT NVL(MAX(VERSION), 1) AS VERSION FROM FWK_MESSAGE_HISTORY his WHERE his.MESSAGE_ID = message_field_.MESSAGE_ID AND his.ORG_ID = message_field_.ORG_ID) AS VERSION,")
                    .append(
                            "SORT_ORDER,DATA_TYPE,DATA_LENGTH,SCALE,ALIGN,FILLER,FIELD_TYPE,USE_MODE,REQUIRED_YN,FIELD_TAG,CODE_GROUP,DEFAULT_VALUE,TEST_VALUE,REMARK,LOG_YN,CODE_MAPPING_YN,MESSAGE_FIELD_NAME,MESSAGE_FIELD_DESC,message_field_.LAST_UPDATE_DTIME,VALIDATION_RULE_ID, message_field_.LAST_UPDATE_USER_ID ")
                    .append("FROM FWK_MESSAGE_FIELD message_field_, FWK_MESSAGE message_ ")
                    .append(
                            " WHERE message_field_.MESSAGE_ID = message_.MESSAGE_ID AND  message_field_.ORG_ID = message_.ORG_ID")
                    .append(" AND message_field_.ORG_ID = '")
                    .append(escape(orgId))
                    .append("' AND message_field_.MESSAGE_ID = '")
                    .append(escape(messageId))
                    .append("';\n");

            sb.append("DELETE FROM FWK_MESSAGE_FIELD WHERE ORG_ID = '")
                    .append(escape(orgId))
                    .append("' AND MESSAGE_ID = '")
                    .append(escape(messageId))
                    .append("'; \n");

            for (Map<String, Object> field : fields) {
                sb.append(toInsertSql("FWK_MESSAGE_FIELD", field)).append("\n");
                sb.append("COMMIT;\n");
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // ##응답전문맵핑## 섹션
    // =========================================================================

    private String buildMappingSection(String trgOrgId, String trgMessageId, String srcOrgId, String srcMessageId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n##응답전문맵핑##\n");

        List<Map<String, Object>> mappings =
                downloadMapper.selectMessageFieldMappingByTrgAndSrc(trgOrgId, trgMessageId, srcOrgId, srcMessageId);

        if (mappings.isEmpty()) return sb.toString();

        sb.append("DELETE FROM FWK_MESSAGE_FIELD_MAPPING WHERE TRG_ORG_ID = '")
                .append(escape(trgOrgId))
                .append("' AND TRG_MESSAGE_ID = '")
                .append(escape(trgMessageId))
                .append("' AND SRC_ORG_ID = '")
                .append(escape(srcOrgId))
                .append("' AND SRC_MESSAGE_ID = '")
                .append(escape(srcMessageId))
                .append("'; \n");

        for (Map<String, Object> mapping : mappings) {
            sb.append(toInsertSql("FWK_MESSAGE_FIELD_MAPPING", mapping)).append("\n");
            sb.append("COMMIT;\n");
        }

        return sb.toString();
    }

    // =========================================================================
    // MESSAGE 타입
    // =========================================================================

    private String[] buildMessageFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String messageId = item.getTranId();
        List<Map<String, Object>> messages = downloadMapper.selectMessagesByMessageId(messageId);

        // 파일명용 orgId (첫 번째 결과 사용)
        String orgId = messages.isEmpty() ? "UNK" : str(messages.get(0).get("ORG_ID"));
        String fileName = "MESSAGE@" + orgId + "@" + messageId + "_" + userId + "_" + timestamp;

        // 레거시 형식: TRAN_ID = [ORG_ID]MESSAGE_ID
        String tranId = "[" + orgId + "]" + messageId;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, tranId, item.getTranName(), "MESSAGE");

        // 각 ORG_ID에 대해 전문 섹션 생성
        for (Map<String, Object> msg : messages) {
            String mOrgId = str(msg.get("ORG_ID"));
            sb.append(buildMessageSection(mOrgId, messageId, reason, "##전문##"));
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // CODE 타입
    // =========================================================================

    private String[] buildCodeFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String codeGroupId = item.getTranId();
        String fileName = "CODE@" + codeGroupId + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, codeGroupId, item.getTranName(), "CODE");

        sb.append("\n##코드그룹##\n");

        // FWK_CODE_GROUP DELETE + INSERT
        List<Map<String, Object>> groups = downloadMapper.selectCodeGroupById(codeGroupId);
        if (!groups.isEmpty()) {
            sb.append("DELETE FROM FWK_CODE_GROUP WHERE CODE_GROUP_ID = '")
                    .append(escape(codeGroupId))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_CODE_GROUP", groups.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        // FWK_CODE DELETE + INSERT (코드그룹에 속한 전체 코드)
        List<Map<String, Object>> codes = downloadMapper.selectCodesByGroupId(codeGroupId);
        if (!codes.isEmpty()) {
            sb.append("DELETE FROM FWK_CODE WHERE CODE_GROUP_ID = '")
                    .append(escape(codeGroupId))
                    .append("'; \n");
            for (Map<String, Object> code : codes) {
                sb.append(toInsertSql("FWK_CODE", code)).append("\n");
                sb.append("COMMIT;\n");
            }
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // SERVICE 타입
    // =========================================================================

    private String[] buildServiceFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String serviceId = item.getTranId();
        String fileName = "SERVICE@" + serviceId + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, serviceId, item.getTranName(), "SERVICE");

        // ##SERVICE##
        sb.append("\n##SERVICE##\n");

        List<Map<String, Object>> services = downloadMapper.selectServiceById(serviceId);
        if (!services.isEmpty()) {
            sb.append("DELETE FROM FWK_SERVICE WHERE SERVICE_ID = '")
                    .append(escape(serviceId))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_SERVICE", services.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        // ##SERVICE_RELATION##
        List<Map<String, Object>> relations = downloadMapper.selectServiceRelationByServiceId(serviceId);
        if (!relations.isEmpty()) {
            sb.append("##SERVICE_RELATION##\n");
            sb.append("DELETE FROM FWK_SERVICE_RELATION WHERE SERVICE_ID = '")
                    .append(escape(serviceId))
                    .append("'; \n");
            for (Map<String, Object> rel : relations) {
                sb.append(toInsertSql("FWK_SERVICE_RELATION", rel)).append("\n");
                sb.append("COMMIT;\n");
            }
        }

        // ##RELATION_PARAM##
        List<Map<String, Object>> params = downloadMapper.selectRelationParamByServiceId(serviceId);
        if (!params.isEmpty()) {
            sb.append("##RELATION_PARAM##\n");
            sb.append("DELETE FROM FWK_RELATION_PARAM WHERE SERVICE_ID = '")
                    .append(escape(serviceId))
                    .append("'; \n");
            for (Map<String, Object> param : params) {
                sb.append(toInsertSql("FWK_RELATION_PARAM", param)).append("\n");
                sb.append("COMMIT;\n");
            }
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // ERROR 타입
    // =========================================================================

    private String[] buildErrorFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String errorCode = item.getTranId();
        String fileName = "ERROR@" + errorCode + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        // 레거시 TRAN_TYPE: 'ERROR_CODE'
        appendTimesAndHis(sb, tranSeq, reason, userId, errorCode, item.getTranName(), "ERROR_CODE");

        sb.append("\n##오류코드##\n");

        // FWK_ERROR DELETE + INSERT
        List<Map<String, Object>> errors = downloadMapper.selectErrorByCode(errorCode);
        if (!errors.isEmpty()) {
            sb.append("DELETE FROM FWK_ERROR WHERE ERROR_CODE = '")
                    .append(escape(errorCode))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_ERROR", errors.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        // FWK_ERROR_DESC DELETE + INSERT (로케일별)
        List<Map<String, Object>> descs = downloadMapper.selectErrorDescByCode(errorCode);
        if (!descs.isEmpty()) {
            sb.append("DELETE FROM FWK_ERROR_DESC WHERE ERROR_CODE = '")
                    .append(escape(errorCode))
                    .append("'; \n");
            for (Map<String, Object> desc : descs) {
                sb.append(toInsertSql("FWK_ERROR_DESC", desc)).append("\n");
                sb.append("COMMIT;\n");
            }
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // PROPERTY 타입
    // =========================================================================

    private String[] buildPropertyFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String propertyId = item.getTranId();
        List<Map<String, Object>> props = downloadMapper.selectPropertiesByPropertyId(propertyId);

        // 파일명용 groupId (첫 번째 결과 사용)
        String groupId = props.isEmpty() ? "UNK" : str(props.get(0).get("PROPERTY_GROUP_ID"));
        String fileName = "PROPERTY@" + groupId + "@" + propertyId + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, propertyId, item.getTranName(), "PROPERTY");

        sb.append("\n##PROPERTY##\n");

        // 복합 PK(PROPERTY_GROUP_ID, PROPERTY_ID) 기준으로 행별 DELETE + INSERT
        for (Map<String, Object> prop : props) {
            String propGroupId = str(prop.get("PROPERTY_GROUP_ID"));
            sb.append("DELETE FROM FWK_PROPERTY WHERE PROPERTY_ID = '")
                    .append(escape(propertyId))
                    .append("' AND PROPERTY_GROUP_ID = '")
                    .append(escape(propGroupId))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_PROPERTY", prop)).append("\n");
            sb.append("COMMIT;\n");
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // COMPONENT 타입
    // =========================================================================

    private String[] buildComponentFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String componentId = item.getTranId();
        String fileName = "COMPONENT@" + componentId + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, componentId, item.getTranName(), "COMPONENT");

        sb.append("\n##COMPONENT##\n");

        List<Map<String, Object>> components = downloadMapper.selectComponentById(componentId);
        if (!components.isEmpty()) {
            sb.append("DELETE FROM FWK_COMPONENT WHERE COMPONENT_ID = '")
                    .append(escape(componentId))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_COMPONENT", components.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // WEBAPP 타입
    // =========================================================================

    // History INSERT 대상 컬럼 (41개 중 STOP_REASON_END_DTIME 제외한 40개)
    private static final String WEBAPP_HISTORY_COLS =
            "START_TIME,ASYNC_YN,BIZ_DOMAIN,BANK_STATUS_CHECK_YN,BIZ_GROUP_ID,"
                    + "WARNING_END_DTIME,MENU_ID,STOP_REASON_START_DTIME,BIZ_DAY_CHECK_YN,ENCRIPTION_YN,"
                    + "WARNING_START_DTIME,CRM_LOG_TYPE1,MENU_URL,END_TIME,USE_YN,"
                    + "LAST_UPDATE_USER_ID,WARNING_EN,CRM_LOG_TYPE2,CRM_LOG_TYPE3,WEB_APP_ID,"
                    + "TIME_CHECK_YN,WARNING_KO,STOP_REASON_KO,STOP_REASON_EN,VALIDATION_MESSAGE_ID,"
                    + "E_CHANNEL_CODE,ECRM_DESC,LAST_UPDATE_DTIME,LOGIN_ONLY_YN,"
                    + "BANK_CODE_FIELD,IN_OUT_MESSAGE_ID,HDAY_END_TIME,IN_OUT_USE_YN,HDAY_START_TIME,"
                    + "URL_PATTERN,LOG_YN,INPUT_TYPE,MENU_NAME,APP_TYPE,SECURE_SIGN_YN";

    private String[] buildWebappFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String menuUrl = item.getTranId();
        String fileName = "WEBAPP@" + menuUrl + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, menuUrl, item.getTranName(), "WEBAPP");

        sb.append("\n##WEBAPP##\n");

        List<Map<String, Object>> webapps = downloadMapper.selectWebappByMenuUrl(menuUrl);
        if (!webapps.isEmpty()) {
            // FWK_CUST_MENU_APP_HISTORY: SELECT-INSERT (VERSION 서브쿼리 + HISTORY_REASON)
            sb.append("INSERT INTO FWK_CUST_MENU_APP_HISTORY ( \n")
                    .append(WEBAPP_HISTORY_COLS)
                    .append(", VERSION, HISTORY_REASON )\n")
                    .append("SELECT ")
                    .append(WEBAPP_HISTORY_COLS)
                    .append(", ")
                    .append("(SELECT NVL(MAX(his.VERSION), 0) + 1 AS VERSION FROM FWK_CUST_MENU_APP_HISTORY his")
                    .append("  WHERE main_.MENU_URL = his.MENU_URL) AS VERSION , '")
                    .append(escape(reason))
                    .append("' AS HISTORY_REASON\n")
                    .append("FROM FWK_CUST_MENU_APP main_\n")
                    .append("WHERE main_.MENU_URL = '")
                    .append(escape(menuUrl))
                    .append("' ;\n");

            // FWK_CUST_MENU_APP: DELETE + INSERT
            sb.append("DELETE FROM FWK_CUST_MENU_APP WHERE MENU_URL = '")
                    .append(escape(menuUrl))
                    .append("'; \n");
            sb.append(toInsertSql("FWK_CUST_MENU_APP", webapps.get(0))).append("\n");
            sb.append("COMMIT;\n");
        }

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // 공통: ##이행회차## + ##이행이력## 섹션
    // =========================================================================

    private void appendTimesAndHis(
            StringBuilder sb,
            String tranSeq,
            String reason,
            String userId,
            String tranId,
            String tranName,
            String tranType) {
        sb.append("##이행회차##\n");
        sb.append("INSERT INTO FWK_TRANS_DATA_TIMES(TRAN_SEQ, TRAN_REASON, TRAN_RESULT,TRAN_TIME,USER_ID)")
                .append(" VALUES ('")
                .append(tranSeq)
                .append("','")
                .append(escape(reason))
                .append("','S',TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'),'")
                .append(escape(userId))
                .append("');\n");
        sb.append("COMMIT;\n");

        sb.append("\n##이행이력##\n");
        sb.append("INSERT INTO FWK_TRANS_DATA_HIS(TRAN_SEQ,TRAN_ID,TRAN_TYPE,TRAN_NAME,TRAN_RESULT,TRAN_TIME)")
                .append(" VALUES ('")
                .append(tranSeq)
                .append("','")
                .append(escape(tranId))
                .append("','")
                .append(escape(tranType))
                .append("','")
                .append(escape(tranName))
                .append("','S',TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'));\n");
        sb.append("COMMIT;\n");
    }

    // =========================================================================
    // 미지원 타입 fallback
    // =========================================================================

    private String[] buildDefaultFile(
            TransDataItemRequest item, String tranSeq, String reason, String userId, String timestamp) {
        String fileName = item.getTranType() + "@" + item.getTranId() + "_" + userId + "_" + timestamp;

        StringBuilder sb = new StringBuilder();
        appendTimesAndHis(sb, tranSeq, reason, userId, item.getTranId(), item.getTranName(), item.getTranType());

        return new String[] {fileName, sb.toString()};
    }

    // =========================================================================
    // SQL 유틸
    // =========================================================================

    /** Map 행을 INSERT SQL 문자열로 변환 */
    private String toInsertSql(String tableName, Map<String, Object> row) {
        String cols = String.join(",", row.keySet());
        String vals = row.values().stream().map(this::formatValue).collect(Collectors.joining(" , "));
        return "INSERT INTO " + tableName + " (" + cols + ") VALUES ( " + vals + " );";
    }

    /** 값 포맷: null → null, 숫자 → 그대로, 문자 → 단따옴표 */
    private String formatValue(Object val) {
        if (val == null) return "null";
        if (val instanceof Number) return val.toString();
        return "'" + val.toString().replace("'", "''") + "'";
    }

    /** SQL 인젝션 방지용 단따옴표 이스케이프 */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }

    /** Object → 트림된 String (null/빈 문자열이면 null 반환) */
    private String str(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 중복 없이 추가 (null 제외) */
    private void addUnique(Set<String> set, String value) {
        if (value != null) {
            set.add(value);
        }
    }
}
