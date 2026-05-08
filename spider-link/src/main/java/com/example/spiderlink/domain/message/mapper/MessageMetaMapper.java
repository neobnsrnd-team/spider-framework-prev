package com.example.spiderlink.domain.message.mapper;

import com.example.spiderlink.domain.message.dto.MessageFieldMeta;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_MESSAGE / FWK_MESSAGE_FIELD 조회 전용 Mapper.
 *
 * <p>MessageStructureCache가 전문 구조를 DB에서 로드할 때만 사용한다.</p>
 */
@Mapper
public interface MessageMetaMapper {

    /**
     * 기관·전문 ID에 해당하는 전문 타입 코드를 반환한다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return MESSAGE_TYPE 코드 (J/F/X/I/C/D), 없으면 null
     */
    String selectMessageType(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 전문 필드 목록을 SORT_ORDER 오름차순으로 반환한다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 필드 메타데이터 목록
     */
    List<MessageFieldMeta> selectFields(@Param("orgId") String orgId, @Param("messageId") String messageId);
}
