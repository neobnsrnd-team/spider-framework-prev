package com.example.spider_admin.domain.proxyresponse.mapper;

import com.example.spider_admin.domain.proxyresponse.dto.*;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataCreateRequest;
import com.example.spider_admin.domain.proxyresponse.dto.ProxyTestdataUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 당발 대응답 테스트 Command Mapper
 * INSERT, UPDATE, DELETE 등 데이터 변경 작업을 담당합니다.
 *
 * @see ProxyTestdataMapper
 */
@Mapper
public interface ProxyTestdataMapper {

    /**
     * 새로운 당발 대응답 테스트를 등록합니다.
     *
     * @param dto 생성 요청 DTO
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void insertMessageTest(
            @Param("dto") ProxyTestdataCreateRequest dto, @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 당발 대응답 테스트를 수정합니다.
     *
     * @param dto 수정 요청 DTO
     * @param lastUpdateUserId 최종 수정 사용자 ID
     */
    void updateMessageTest(
            @Param("dto") ProxyTestdataUpdateRequest dto, @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 당발 대응답 테스트를 삭제합니다.
     *
     * @param testSno     테스트 일련번호
     * @param testGroupId 테스트 그룹 ID
     */
    void deleteMessageTest(@Param("testSno") Long testSno, @Param("testGroupId") String testGroupId);

    /**
     * 대응답 필드 구분값(PROXY_FIELD) 일괄 업데이트
     * ORG_ID, TRX_ID, TEST_GROUP_ID가 일치하는 모든 레코드의 PROXY_FIELD를 변경합니다.
     */
    void updateProxyField(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testGroupId") String testGroupId,
            @Param("proxyField") String proxyField,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 대응답 값(PROXY_VALUE) 중복 건수 조회
     */
    int countByProxyValue(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testGroupId") String testGroupId,
            @Param("proxyValue") String proxyValue);

    /**
     * 대응답 값(PROXY_VALUE) 초기화
     * 동일한 PROXY_VALUE를 가진 레코드의 PROXY_VALUE를 빈 문자열로 변경합니다.
     */
    void clearProxyValue(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testGroupId") String testGroupId,
            @Param("proxyValue") String proxyValue);

    /**
     * 대응답 값(PROXY_VALUE) 업데이트
     */
    void updateProxyValue(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testSno") Long testSno,
            @Param("testGroupId") String testGroupId,
            @Param("proxyValue") String proxyValue,
            @Param("lastUpdateUserId") String lastUpdateUserId,
            @Param("userId") String userId,
            @Param("testName") String testName);

    /**
     * 기본 대응답 초기화 (DEFAULT_PROXY_YN → 'N')
     * ORG_ID, TRX_ID, TEST_GROUP_ID가 일치하는 모든 레코드 대상
     */
    void clearDefaultProxy(
            @Param("orgId") String orgId, @Param("trxId") String trxId, @Param("testGroupId") String testGroupId);

    /**
     * 기본 대응답 설정 (DEFAULT_PROXY_YN → 'Y')
     * 특정 TEST_SNO에 대해 설정
     */
    void setDefaultProxy(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testSno") Long testSno,
            @Param("testGroupId") String testGroupId,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 검색 조회 (JOIN 포함)
     */
    List<ProxyTestdataListResponse> findAllWithSearch(
            @Param("orgIdFilter") String orgIdFilter,
            @Param("trxIdFilter") String trxIdFilter,
            @Param("testNameFilter") String testNameFilter,
            @Param("userIdFilter") String userIdFilter,
            @Param("ioType") String ioType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 해당하는 전문 테스트 수를 조회합니다.
     */
    long countAllWithSearch(
            @Param("orgIdFilter") String orgIdFilter,
            @Param("trxIdFilter") String trxIdFilter,
            @Param("testNameFilter") String testNameFilter,
            @Param("userIdFilter") String userIdFilter,
            @Param("ioType") String ioType);

    /**
     * 부모 전문 체인을 재귀적으로 조회합니다. (Recursive CTE 사용)
     *
     * <p>자식 전문부터 부모 전문까지 최대 10단계까지 조회하며,
     * 순환 참조가 있을 경우 자동으로 중단됩니다.</p>
     *
     * @param orgId     기관 ID
     * @param messageId 시작 전문 ID (자식)
     * @return 부모 → 자식 순서로 정렬된 messageId 목록 (최상위 부모부터)
     */
    List<String> findMessageIdChain(@Param("orgId") String orgId, @Param("messageId") String messageId);

    /**
     * 거래조회 모달용 검색 (FWK_TRX_MESSAGE + FWK_ORG + FWK_TRX)
     */
    List<ProxyTestdataTrxSearchResponse> findTrxMessagesWithSearch(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("trxName") String trxName,
            @Param("ioType") String ioType);

    /**
     * 상세 조회 (JOIN 포함: 기관명, 거래명)
     */
    ProxyTestdataDetailResponse findDetailByTestSno(@Param("testSno") Long testSno);

    /**
     * 거래-전문 매핑에서 MESSAGE_ID, STD_MESSAGE_ID를 조회합니다.
     * IO_TYPE에 따라: O → MESSAGE_ID/STD_MESSAGE_ID, I → RES_MESSAGE_ID/STD_RES_MESSAGE_ID
     *
     * @param orgId  기관 ID
     * @param trxId  거래 ID
     * @param ioType I/O 타입 (O: 요청, I: 응답)
     * @return {@link ProxyTestdataMsgIdsResponse} 전문 ID 매핑 정보
     */
    ProxyTestdataMsgIdsResponse findTrxMessageIds(
            @Param("orgId") String orgId, @Param("trxId") String trxId, @Param("ioType") String ioType);

    /**
     * 기관 전문 필드와 표준 전문 필드를 LEFT OUTER JOIN하여 조회합니다.
     * makeRealValue 처리 전 원본 데이터를 반환합니다.
     *
     * @param orgId         기관 ID
     * @param messageId     기관 전문 ID
     * @param stdMessageId  표준 전문 ID (nullable)
     * @return 기관/표준 USE_MODE, DEFAULT_VALUE가 포함된 필드 목록
     */
    List<ProxyTestdataFieldResponse> findFieldsWithStd(
            @Param("orgId") String orgId,
            @Param("messageId") String messageId,
            @Param("stdMessageId") String stdMessageId);

    /**
     * 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<ProxyTestdataListResponse> findAllForExport(
            @Param("orgIdFilter") String orgIdFilter,
            @Param("trxIdFilter") String trxIdFilter,
            @Param("testNameFilter") String testNameFilter,
            @Param("userIdFilter") String userIdFilter,
            @Param("ioType") String ioType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 전체 건수 조회
     */
    long countAll();

    /**
     * 전체 목록 조회 (엑셀 내보내기용, 페이징 없음)
     */
    List<ProxyTestdataListResponse> findAllForExport();

    /**
     * 대응답 설정 목록 조회 (ORG_ID, TRX_ID 기준, TEST_SNO 정렬)
     */
    List<ProxySettingListResponse> findProxySettings(
            @Param("orgId") String orgId,
            @Param("trxId") String trxId,
            @Param("testGroupId") String testGroupId,
            @Param("testName") String testName,
            @Param("userId") String userId);

    /**
     * 기본 대응답 조회 (DEFAULT_PROXY_YN='Y')
     */
    ProxySettingListResponse findDefaultProxy(
            @Param("orgId") String orgId, @Param("trxId") String trxId, @Param("testGroupId") String testGroupId);

    /**
     * 테스트 그룹 ID 목록 조회 (DISTINCT)
     */
    List<String> findGroupIds(@Param("orgId") String orgId, @Param("trxId") String trxId);
}
