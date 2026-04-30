package com.example.spideradmin.domain.messagefield.mapper;

import com.example.spideradmin.domain.messagefield.dto.FieldCreateRequest;
import com.example.spideradmin.domain.messagefield.dto.FieldUpdateRequest;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MessageField Mapper (CRUD + Query)
 * FWK_MESSAGE_FIELD 테이블에 대한 CRUD 및 조회 작업 담당
 */
@Mapper
public interface FieldMapper {

    /**
     * 필드 단건 조회 (MessageFieldResponse DTO 직접 반환)
     *
     * @param orgId          기관 ID
     * @param messageId      전문 ID
     * @param messageFieldId 필드 ID
     * @return 조회된 필드 응답 DTO
     */
    MessageFieldResponse selectResponseById(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("messageFieldId") String messageFieldId);

    /**
     * 필드 생성
     *
     * @param dto              생성 요청 DTO
     * @param lastUpdateDtime  최종 수정 일시
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void insertField(
            @Param("dto") FieldCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 필드 일괄 생성 (Oracle용 UNION ALL 패턴)
     *
     * @param fields           생성할 필드 요청 DTO 목록
     * @param lastUpdateDtime  최종 수정 일시
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void insertFieldBatch(
            @Param("fields") List<FieldCreateRequest> fields,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 필드 수정
     *
     * @param orgId            기관 ID
     * @param messageId        전문 ID
     * @param messageFieldId   필드 ID
     * @param dto              수정 요청 DTO
     * @param lastUpdateDtime  최종 수정 일시
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void updateField(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("messageFieldId") String messageFieldId,
            @Param("dto") FieldUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 필드 삭제
     *
     * @param orgId          기관 ID
     * @param messageId      전문 ID
     * @param messageFieldId 필드 ID
     */
    void deleteField(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("messageFieldId") String messageFieldId);

    /**
     * 필드 존재 확인 (중복 체크용)
     *
     * @param orgId          기관 ID
     * @param messageId      전문 ID
     * @param messageFieldId 필드 ID
     * @return 필드 개수
     */
    int countByFieldId(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("messageFieldId") String messageFieldId);

    /**
     * 전문 ID에 해당하는 필드 목록을 순번 오름차순으로 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 필드 응답 DTO 목록
     */
    List<MessageFieldResponse> findByMessageId(@Param("orgId") String orgId, @Param("messageId") String messageId);
}
