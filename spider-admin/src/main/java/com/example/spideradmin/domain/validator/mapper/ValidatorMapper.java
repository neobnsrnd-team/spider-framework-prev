package com.example.spideradmin.domain.validator.mapper;

import com.example.spideradmin.domain.validator.dto.ValidatorCreateRequest;
import com.example.spideradmin.domain.validator.dto.ValidatorResponse;
import com.example.spideradmin.domain.validator.dto.ValidatorUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Validator Mapper (CRUD + Query)
 */
@Mapper
public interface ValidatorMapper {

    /**
     * Validator ID로 단건 조회 (ResponseDTO 반환)
     *
     * @param validatorId Validator 식별자
     * @return ValidatorResponse DTO
     */
    ValidatorResponse selectResponseById(@Param("validatorId") String validatorId);

    /**
     * Validator ID 존재 확인용 카운트
     *
     * @param validatorId Validator 식별자
     * @return 존재하면 1, 없으면 0
     */
    int countById(@Param("validatorId") String validatorId);

    /**
     * 새 Validator 등록
     *
     * @param dto 생성 요청 DTO
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void insert(
            @Param("dto") ValidatorCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * Validator 수정
     *
     * @param validatorId Validator 식별자
     * @param dto 수정 요청 DTO
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void update(
            @Param("validatorId") String validatorId,
            @Param("dto") ValidatorUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * Validator 삭제
     *
     * @param validatorId Validator 식별자
     */
    void deleteById(@Param("validatorId") String validatorId);

    /**
     * 검색 조건으로 Validator 목록 조회 (FWK_CODE JOIN으로 bizDomainName 포함)
     */
    List<ValidatorResponse> findAllWithSearch(
            @Param("validatorId") String validatorId,
            @Param("validatorName") String validatorName,
            @Param("bizDomain") String bizDomain,
            @Param("useYn") String useYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건으로 Validator 전체 건수 조회
     */
    long countAllWithSearch(
            @Param("validatorId") String validatorId,
            @Param("validatorName") String validatorName,
            @Param("bizDomain") String bizDomain,
            @Param("useYn") String useYn);

    /**
     * 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<ValidatorResponse> findAllForExport(
            @Param("validatorId") String validatorId,
            @Param("validatorName") String validatorName,
            @Param("bizDomain") String bizDomain,
            @Param("useYn") String useYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
