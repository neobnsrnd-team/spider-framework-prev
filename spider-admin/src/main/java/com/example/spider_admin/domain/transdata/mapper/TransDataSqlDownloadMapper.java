package com.example.spider_admin.domain.transdata.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 이행 SQL 생성용 Query Mapper
 * - LinkedHashMap으로 컬럼 순서 보장
 */
@Mapper
public interface TransDataSqlDownloadMapper {

    List<Map<String, Object>> selectTrxById(@Param("trxId") String trxId);

    List<Map<String, Object>> selectTrxMessageByTrxId(@Param("trxId") String trxId);

    List<Map<String, Object>> selectMessageByOrgAndId(
            @Param("orgId") String orgId, @Param("messageId") String messageId);

    List<Map<String, Object>> selectMessageFieldByOrgAndId(
            @Param("orgId") String orgId, @Param("messageId") String messageId);

    List<Map<String, Object>> selectMessageFieldMappingByTrgAndSrc(
            @Param("trgOrgId") String trgOrgId,
            @Param("trgMessageId") String trgMessageId,
            @Param("srcOrgId") String srcOrgId,
            @Param("srcMessageId") String srcMessageId);

    // ── MESSAGE 타입 (ORG_ID 미지정, MESSAGE_ID만으로 전체 조회) ──────────
    List<Map<String, Object>> selectMessagesByMessageId(@Param("messageId") String messageId);

    // ── CODE 타입 ─────────────────────────────────────────────────────────
    List<Map<String, Object>> selectCodeGroupById(@Param("codeGroupId") String codeGroupId);

    List<Map<String, Object>> selectCodesByGroupId(@Param("codeGroupId") String codeGroupId);

    // ── SERVICE 타입 ──────────────────────────────────────────────────────
    List<Map<String, Object>> selectServiceById(@Param("serviceId") String serviceId);

    List<Map<String, Object>> selectServiceRelationByServiceId(@Param("serviceId") String serviceId);

    List<Map<String, Object>> selectRelationParamByServiceId(@Param("serviceId") String serviceId);

    // ── ERROR 타입 ────────────────────────────────────────────────────────
    List<Map<String, Object>> selectErrorByCode(@Param("errorCode") String errorCode);

    List<Map<String, Object>> selectErrorDescByCode(@Param("errorCode") String errorCode);

    // ── PROPERTY 타입 ─────────────────────────────────────────────────────
    List<Map<String, Object>> selectPropertiesByPropertyId(@Param("propertyId") String propertyId);

    // ── COMPONENT 타입 ────────────────────────────────────────────────────
    List<Map<String, Object>> selectComponentById(@Param("componentId") String componentId);

    // ── WEBAPP 타입 ───────────────────────────────────────────────────────
    List<Map<String, Object>> selectWebappByMenuUrl(@Param("menuUrl") String menuUrl);
}
