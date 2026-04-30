package com.example.spideradmin.domain.cmsabtest.mapper;

import com.example.spideradmin.domain.cmsabtest.dto.CmsAbGroupPageResponse;
import com.example.spideradmin.domain.cmsabtest.dto.CmsAbPageResponse;
import com.example.spideradmin.domain.cmsabtest.dto.CmsAbTestListRequest;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CMS A/B 테스트용 페이지·그룹 데이터를 조회하고 갱신하는 MyBatis 매퍼.
 *
 * <p>대시보드 조회, 그룹 편집, 우승안 승격이 모두 CMS 페이지 상태와 연결되므로
 * 관련 SQL을 한 인터페이스에 모아 서비스 계층이 규칙 검증에 집중할 수 있게 한다.
 */
@Mapper
public interface CmsAbTestMapper {

    /** 승인된 페이지 중 대시보드 표에 노출할 목록을 조회한다. */
    List<CmsAbPageResponse> findApprovedPages(
            @Param("req") CmsAbTestListRequest req, @Param("offset") int offset, @Param("endRow") int endRow);

    /** 승인된 페이지 목록의 전체 건수를 반환한다. */
    long countApprovedPages(@Param("req") CmsAbTestListRequest req);

    /** 현재 A/B 그룹에 묶여 있는 페이지들을 검색 조건으로 조회한다. */
    List<CmsAbPageResponse> findAbGroupPages(@Param("search") String search);

    /** 그룹에 추가 가능한 승인 페이지 후보를 조회한다. */
    List<CmsAbPageResponse> findApprovedPageOptions(@Param("search") String search);

    /** 특정 그룹에 속한 페이지 상세와 가중치를 조회한다. */
    List<CmsAbGroupPageResponse> findGroupPages(@Param("groupId") String groupId);

    /** 그룹에 연결된 페이지 수를 세어 존재 여부를 판단한다. */
    long countGroupPages(@Param("groupId") String groupId);

    /** 전달한 페이지들이 모두 승인된 공개 가능 페이지인지 확인한다. */
    long countApprovedPagesByIds(@Param("pageIds") List<String> pageIds);

    /** 다른 그룹에 이미 속한 충돌 페이지가 있는지 확인한다. */
    long countConflictingPages(@Param("groupId") String groupId, @Param("pageIds") List<String> pageIds);

    /** 특정 페이지가 지정한 그룹의 구성원인지 확인한다. */
    long countPageInGroup(@Param("groupId") String groupId, @Param("pageId") String pageId);

    /** 페이지를 그룹에 연결하거나 기존 연결의 가중치를 갱신한다. */
    int updateAbGroup(
            @Param("pageId") String pageId,
            @Param("groupId") String groupId,
            @Param("weight") BigDecimal weight,
            @Param("modifierId") String modifierId);

    /** 그룹에 속한 모든 페이지의 A/B 연결을 해제한다. */
    int clearAbGroup(@Param("groupId") String groupId, @Param("modifierId") String modifierId);

    /** 특정 페이지 하나만 A/B 그룹에서 제외한다. */
    int clearPageAbGroup(@Param("pageId") String pageId, @Param("modifierId") String modifierId);

    /** 우승안 외 나머지 페이지를 패배안 상태로 정리한다. */
    int promoteLosers(
            @Param("groupId") String groupId,
            @Param("winnerPageId") String winnerPageId,
            @Param("modifierId") String modifierId);

    /** 우승 페이지를 최종안으로 승격한다. */
    int setWinner(@Param("winnerPageId") String winnerPageId, @Param("modifierId") String modifierId);
}
