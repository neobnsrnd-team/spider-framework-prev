package com.example.spider_admin.domain.accessuser.mapper;

import com.example.spider_admin.domain.accessuser.dto.AccessUserCreateRequest;
import com.example.spider_admin.domain.accessuser.dto.AccessUserResponse;
import com.example.spider_admin.domain.accessuser.dto.AccessUserUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <h3>중지거래 접근허용자 Mapper</h3>
 * <p>데이터 변경(insert, update, delete) 및 조회를 담당합니다.</p>
 */
@Mapper
public interface AccessUserMapper {

    /**
     * 중지거래 접근허용자를 생성합니다.
     * @param dto 생성 요청 DTO
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     * @return 생성된 행 수
     */
    int insertAccessUser(
            @Param("dto") AccessUserCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 중지거래 접근허용자를 수정합니다.
     * @param dto 수정 요청 DTO
     * @param lastUpdateDtime 최종 수정 일시
     * @param lastUpdateUserId 최종 수정자 ID
     * @return 수정된 행 수
     */
    int updateAccessUser(
            @Param("dto") AccessUserUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 중지거래 접근허용자를 삭제합니다. (복합 PK)
     * @param gubunType 구분유형
     * @param trxId 거래/서비스 ID
     * @param custUserId 접근허용 사용자 ID
     * @return 삭제된 행 수
     */
    int deleteAccessUser(
            @Param("gubunType") String gubunType, @Param("trxId") String trxId, @Param("custUserId") String custUserId);

    /**
     * 복합 PK로 중지거래 접근허용자 존재 여부를 확인합니다.
     * @param gubunType 구분유형
     * @param trxId 거래/서비스 ID
     * @param custUserId 접근허용 사용자 ID
     * @return 존재 여부 (1: 존재, 0: 미존재)
     */
    int existsByPk(
            @Param("gubunType") String gubunType, @Param("trxId") String trxId, @Param("custUserId") String custUserId);

    /**
     * 복합 PK로 중지거래 접근허용자 응답 DTO를 조회합니다.
     * @param gubunType 구분유형
     * @param trxId 거래/서비스 ID
     * @param custUserId 접근허용 사용자 ID
     * @return 응답 DTO (존재하지 않으면 null)
     */
    AccessUserResponse selectResponseByPk(
            @Param("gubunType") String gubunType, @Param("trxId") String trxId, @Param("custUserId") String custUserId);

    /**
     * 전체 중지거래 접근허용자 목록을 조회합니다.
     * @return 중지거래 접근허용자 응답 DTO 목록
     */
    List<AccessUserResponse> findAll();

    /**
     * 검색 조건에 맞는 중지거래 접근허용자 목록을 조회합니다 (네이티브 ROWNUM 페이징).
     * @param trxId 거래/서비스ID (부분 검색)
     * @param gubunType 구분유형 (전체/거래/서비스)
     * @param custUserId 접근허용 사용자ID (부분 검색)
     * @param sortBy 정렬 컬럼
     * @param sortDirection 정렬 방향 (ASC/DESC)
     * @param offset ROWNUM 시작 offset (0-based)
     * @param endRow ROWNUM 끝 행 번호
     * @return 검색된 중지거래 접근허용자 응답 DTO 목록
     */
    List<AccessUserResponse> findAllWithSearch(
            @Param("trxId") String trxId,
            @Param("gubunType") String gubunType,
            @Param("custUserId") String custUserId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 맞는 중지거래 접근허용자 수를 조회합니다.
     * @param trxId 거래/서비스ID (부분 검색)
     * @param gubunType 구분유형 (전체/거래/서비스)
     * @param custUserId 접근허용 사용자ID (부분 검색)
     * @return 검색된 건수
     */
    long countAllWithSearch(
            @Param("trxId") String trxId, @Param("gubunType") String gubunType, @Param("custUserId") String custUserId);
}
