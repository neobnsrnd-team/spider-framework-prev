package com.example.spider_admin.domain.trxmessage.mapper;

import com.example.spider_admin.domain.trxmessage.dto.MessageBrowseResponse;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageWithTrxResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 트랜잭션-메시지 Command Mapper (CRUD)
 * - insert / update / delete / 단건 조회 (Command 중심)
 * - 목록 조회, 검색 등 Query 전용 SQL은 TrxMessageMapper에서 처리
 */
@Mapper
public interface TrxMessageMapper {

    // 수정 (동적 SET)
    void updateTrxMessage(
            @Param("trxId") String trxId,
            @Param("orgId") String orgId,
            @Param("ioType") String ioType,
            @Param("params") Map<String, Object> params);

    // 존재 확인 (중복 체크용)
    int countByCompositeKey(@Param("trxId") String trxId, @Param("orgId") String orgId, @Param("ioType") String ioType);

    // ==================== 목록 조회 ====================

    /**
     * 검색 조건으로 트랜잭션-메시지 목록 조회 (Trx 정보 포함)
     */
    List<TrxMessageWithTrxResponse> findAllWithTrxAndSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("ioTypeFilter") String ioTypeFilter,
            @Param("trxIdFilter") String trxIdFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("allowedOrgIds") List<String> allowedOrgIds,
            @Param("deduplicate") boolean deduplicate,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 해당하는 트랜잭션-메시지 수를 조회합니다.
     */
    long countAllWithTrxAndSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("ioTypeFilter") String ioTypeFilter,
            @Param("trxIdFilter") String trxIdFilter,
            @Param("allowedOrgIds") List<String> allowedOrgIds,
            @Param("deduplicate") boolean deduplicate);

    /**
     * trxId와 ioType으로 모든 트랜잭션-메시지 조회 (Trx 상세 정보 포함)
     * @param trxId 거래 ID
     * @param ioType IO 타입
     * @return 해당 trxId와 ioType을 가진 모든 메시지 (여러 orgId 포함)
     */
    List<TrxMessageWithTrxResponse> findAllByTrxIdAndIoTypeWithTrx(
            @Param("trxId") String trxId, @Param("ioType") String ioType);

    // ==================== 전문 조회 (Browse) ====================

    List<MessageBrowseResponse> browseMessagesWithPaging(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("ioTypeFilter") String ioTypeFilter,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countBrowseMessages(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("ioTypeFilter") String ioTypeFilter,
            @Param("orgIdFilter") String orgIdFilter);
}
