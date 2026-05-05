package com.example.spideradmin.domain.transdata.mapper;

import com.example.spideradmin.domain.transdata.dto.TransDataDetailResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataTimesResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 이행 실행 이력 Command Mapper
 * 단건 조회 전용 (READ-ONLY 도메인)
 */
public interface TransDataTimesMapper {

    /**
     * 이행 시퀀스로 존재 여부 확인
     * @param tranSeq 이행 시퀀스
     * @return 존재하면 1 이상
     */
    int countByTranSeq(Long tranSeq);

    /**
     * 검색 조건에 따른 이행 실행 이력 목록 조회
     *
     * @param userId        사용자 ID (LIKE 검색)
     * @param tranResult    이행 결과 (정확 매치)
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향
     * @return 이행 실행 이력 목록
     */
    List<TransDataTimesResponse> findAllWithSearch(
            @Param("userId") String userId,
            @Param("tranResult") String tranResult,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 따른 이행 실행 이력 건수 조회
     */
    long countAllWithSearch(@Param("userId") String userId, @Param("tranResult") String tranResult);

    TransDataDetailResponse findDetailByTranSeq(@Param("tranSeq") Long tranSeq);

    /**
     * 엑셀 내보내기용 이행 실행 이력 전체 조회 (페이징 없음)
     *
     * @param userId        사용자 ID (LIKE 검색)
     * @param tranResult    이행 결과 (정확 매치)
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향
     * @return 이행 실행 이력 목록
     */
    List<TransDataTimesResponse> findAllForExport(
            @Param("userId") String userId,
            @Param("tranResult") String tranResult,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
