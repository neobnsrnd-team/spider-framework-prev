package com.example.spideradmin.domain.message.mapper;

import com.example.spideradmin.domain.message.dto.HeaderMessageResponse;
import com.example.spideradmin.domain.message.dto.MessageCreateRequest;
import com.example.spideradmin.domain.message.dto.MessageListItemResponse;
import com.example.spideradmin.domain.message.dto.MessageUpdateRequest;
import com.example.spideradmin.domain.message.dto.MessageVersionResponse;
import com.example.spideradmin.domain.message.dto.StdMessageSearchResponse;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Message 테이블에 대한 CRUD 및 조회 작업을 담당하는 Mapper입니다.
 */
public interface MessageMapper {

    /**
     * 기관 ID와 전문 ID를 기준으로 단건 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return {@link MessageResponse} 전문 응답 DTO
     */
    MessageResponse selectResponseById(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 새로운 전문을 생성합니다.
     *
     * @param dto               생성 요청 DTO
     * @param lastUpdateDtime   최종 수정 일시
     * @param lastUpdateUserId  최종 수정 사용자 ID
     */
    void insertMessage(
            @Param("dto") MessageCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 기존 전문을 수정합니다.
     *
     * @param orgId             기관 ID
     * @param messageId         전문 ID
     * @param dto               수정 요청 DTO
     * @param lastUpdateDtime   최종 수정 일시
     * @param lastUpdateUserId  최종 수정 사용자 ID
     */
    void updateMessage(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("dto") MessageUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 기관 ID와 전문 ID를 기준으로 삭제합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     */
    void deleteMessage(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 기관 ID와 전문 ID 기준 전문 개수를 조회합니다. (중복 체크용)
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return {@code int} 동일 전문 개수
     */
    int countByMessageId(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 기관 ID와 전문 ID를 기준으로 전문과 연관된 필드 목록을 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return {@link List} {@link MessageFieldResponse} 필드 목록
     */
    List<MessageFieldResponse> findFieldsByMessageId(
            @Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 검색 조건을 적용하여 전문 목록을 조회합니다.
     */
    List<MessageListItemResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("headerYnFilter") String headerYnFilter,
            @Param("parentMessageIdFilter") String parentMessageIdFilter,
            @Param("messageTypeFilter") String messageTypeFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건을 적용하여 전문 목록을 조회합니다. (비페이징)
     */
    List<MessageResponse> findAllBySearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("headerYnFilter") String headerYnFilter,
            @Param("parentMessageIdFilter") String parentMessageIdFilter,
            @Param("messageTypeFilter") String messageTypeFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 검색 조건에 해당하는 전문 수를 조회합니다.
     */
    long countAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("headerYnFilter") String headerYnFilter,
            @Param("parentMessageIdFilter") String parentMessageIdFilter,
            @Param("messageTypeFilter") String messageTypeFilter);

    /**
     * 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<MessageListItemResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("headerYnFilter") String headerYnFilter,
            @Param("parentMessageIdFilter") String parentMessageIdFilter,
            @Param("messageTypeFilter") String messageTypeFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 전체 전문 수를 조회합니다.
     */
    long countAll();

    /**
     * 중복 제거된 기관 ID 목록을 조회합니다.
     */
    List<String> findDistinctOrgIds();

    List<String> findDistinctMessageTypes();

    List<String> findDistinctValidationRuleIds();

    /**
     * 기관 ID에 해당하는 헤더 전문 목록을 조회합니다. (HEADER_YN = 'Y')
     *
     * @param orgId 기관 ID
     * @return {@link List} {@link HeaderMessageResponse} 헤더 전문 목록
     */
    List<HeaderMessageResponse> findHeaderMessagesByOrgId(@Param("orgId") String orgId);

    // === Backup/Restore ===

    /**
     * FWK_MESSAGE_HISTORY 테이블에서 최대 버전을 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 최대 버전 번호 (없으면 null)
     */
    Integer getMaxVersion(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * FWK_MESSAGE → FWK_MESSAGE_HISTORY로 복사합니다. (INSERT SELECT)
     */
    void insertMessageHistory(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("version") int version,
            @Param("historyReason") String historyReason,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * FWK_MESSAGE_FIELD → FWK_MESSAGE_FIELD_HISTORY로 복사합니다. (INSERT SELECT)
     */
    void insertMessageFieldHistory(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("version") int version,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * FWK_MESSAGE 테이블의 CUR_VERSION을 업데이트합니다.
     */
    void updateMessageVersion(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("version") int version,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 특정 전문의 모든 백업 버전 목록을 조회합니다. (드롭다운용)
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 버전 목록 (최신순)
     */
    List<MessageVersionResponse> findVersionsByMessageId(
            @Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 현재 전문 필드를 모두 삭제합니다. (복원 전 정리용)
     */
    void deleteMessageFields(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * FWK_MESSAGE_HISTORY에서 FWK_MESSAGE로 복원합니다. (MERGE)
     */
    void restoreMessageFromHistory(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("version") int version,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * FWK_MESSAGE_FIELD_HISTORY에서 FWK_MESSAGE_FIELD로 복원합니다.
     */
    void restoreMessageFieldsFromHistory(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("version") int version,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * FWK_MESSAGE_FIELD_HISTORY에서 특정 버전의 필드 목록을 조회합니다. (복원 전 비교용)
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @param version   버전 번호
     * @return {@link List} {@link MessageFieldResponse} 해당 버전의 필드 목록
     */
    List<MessageFieldResponse> findFieldsByVersion(
            @Param("orgId") String orgId, @Param("messageId") String messageId, @Param("version") int version);

    /**
     * 필드풀 검증 — 전문필드ID 목록에 대해 FWK_FIELD_POOL 등록 여부를 확인합니다.
     */
    List<com.example.spideradmin.domain.message.dto.FieldPoolVerifyResponse> verifyFieldPool(
            @Param("fieldIds") List<String> fieldIds);

    /**
     * Oracle 시스템 뷰에서 테이블 컬럼 정보를 조회합니다.
     */
    List<com.example.spideradmin.domain.message.dto.TableColumnResponse> findTableColumns(
            @Param("tableName") String tableName);

    /**
     * 표준전문조회복사용 — 거래ID/거래명으로 FWK_TRX_MESSAGE + FWK_TRX + FWK_MESSAGE를 검색합니다.
     */
    List<StdMessageSearchResponse> findTrxMessagesForStdSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 표준전문조회복사용 — 검색 조건에 해당하는 건수를 조회합니다.
     */
    long countTrxMessagesForStdSearch(
            @Param("searchField") String searchField, @Param("searchValue") String searchValue);
}
