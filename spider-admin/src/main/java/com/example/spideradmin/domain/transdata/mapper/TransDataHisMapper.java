package com.example.spideradmin.domain.transdata.mapper;

import com.example.spideradmin.domain.transdata.dto.TransDataHisFailResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataHisResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 이행 상세 이력 Query Mapper
 * tranSeq 기준 상세 조회 전용
 */
public interface TransDataHisMapper {

    /**
     * 이행 시퀀스에 해당하는 상세 이력 목록 조회
     *
     * @param tranSeq 이행 시퀀스
     * @return 이행 상세 이력 목록
     */
    List<TransDataHisResponse> findByTranSeq(@Param("tranSeq") Long tranSeq);

    /**
     * 이행 상세 이력 페이지네이션 + 필터 검색
     *
     * @param tranSeq    이행 시퀀스
     * @param tranResult 이행결과 필터 (S/F, nullable)
     * @param tranType   이행유형 필터 (nullable)
     * @return 이행 상세 이력 목록
     */
    List<TransDataHisResponse> findByTranSeqWithSearch(
            @Param("tranSeq") Long tranSeq,
            @Param("tranResult") String tranResult,
            @Param("tranType") String tranType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 이행 상세 이력 건수 조회 (필터 포함)
     */
    long countByTranSeqWithSearch(
            @Param("tranSeq") Long tranSeq, @Param("tranResult") String tranResult, @Param("tranType") String tranType);

    /**
     * 이행 상세 이력 실패 상세 단건 조회
     *
     * @param tranSeq  이행 시퀀스
     * @param tranId   이행 ID
     * @param tranType 이행 유형
     * @return 실패 상세 정보
     */
    TransDataHisFailResponse findFailDetail(
            @Param("tranSeq") Long tranSeq, @Param("tranId") String tranId, @Param("tranType") String tranType);
}
