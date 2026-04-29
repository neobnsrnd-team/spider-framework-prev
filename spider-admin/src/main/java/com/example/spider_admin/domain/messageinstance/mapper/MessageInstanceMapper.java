package com.example.spider_admin.domain.messageinstance.mapper;

import com.example.spider_admin.domain.messageinstance.dto.MessageInstanceResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 전문 내역(FWK_MESSAGE_INSTANCE) 테이블에 대한 기본 CRUD 작업을 담당하는 Mapper입니다.
 *
 * <p>
 * 단건 조회, 삭제와 같은 기본적인 데이터 변경 및 조회 작업만을 수행합니다.
 * </p>
 */
@Mapper
public interface MessageInstanceMapper {

    /**
     * 검색 조건을 적용하여 전문 내역 목록을 조회합니다.
     *
     * @param userId        사용자 ID
     * @param trxTrackingNo 거래 추적 번호
     * @param globalId      글로벌 ID (인스턴스 ID)
     * @param orgId         기관 ID
     * @param orgMessageId  기관 전문 ID (메시지 ID)
     * @param trxDateFrom   거래 시작 일자 (YYYYMMDD)
     * @param trxDateTo     거래 종료 일자 (YYYYMMDD)
     * @param trxTimeFrom   거래 시작 시간 (HHMM)
     * @param trxTimeTo     거래 종료 시간 (HHMM)
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향
     * @return {@link List} {@link MessageInstanceResponse} 검색 결과 목록
     */
    List<MessageInstanceResponse> findAllWithSearch(
            @Param("userId") String userId,
            @Param("trxTrackingNo") String trxTrackingNo,
            @Param("globalId") String globalId,
            @Param("orgId") String orgId,
            @Param("orgMessageId") String orgMessageId,
            @Param("trxDateFrom") String trxDateFrom,
            @Param("trxDateTo") String trxDateTo,
            @Param("trxTimeFrom") String trxTimeFrom,
            @Param("trxTimeTo") String trxTimeTo,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 해당하는 전문 내역 수를 조회합니다.
     */
    long countAllWithSearch(
            @Param("userId") String userId,
            @Param("trxTrackingNo") String trxTrackingNo,
            @Param("globalId") String globalId,
            @Param("orgId") String orgId,
            @Param("orgMessageId") String orgMessageId,
            @Param("trxDateFrom") String trxDateFrom,
            @Param("trxDateTo") String trxDateTo,
            @Param("trxTimeFrom") String trxTimeFrom,
            @Param("trxTimeTo") String trxTimeTo);

    /**
     * 거래 추적 번호로 전문 내역 목록을 조회합니다.
     *
     * @param trxTrackingNo 거래 추적 번호
     * @return {@link List} {@link MessageInstanceResponse} 전문 내역 목록
     */
    List<MessageInstanceResponse> findByTrxTrackingNo(@Param("trxTrackingNo") String trxTrackingNo);
}
