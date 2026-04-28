package com.example.admin_demo.domain.user.mapper;

import com.example.admin_demo.domain.user.dto.ProfileUpdateRequest;
import com.example.admin_demo.domain.user.dto.UserCreateRequest;
import com.example.admin_demo.domain.user.dto.UserResponse;
import com.example.admin_demo.domain.user.dto.UserSimpleResponse;
import com.example.admin_demo.domain.user.dto.UserUpdateRequest;
import com.example.admin_demo.domain.user.dto.UserWithRoleResponse;
import com.example.admin_demo.global.auth.dto.CmsApproverResponse;
import com.example.admin_demo.global.security.dto.UserAuthInfo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자에 대한 CRUD 및 조회 작업을 담당하는 Mapper입니다.
 */
public interface UserMapper {

    /**
     * 인증 정보 조회 (Security 레이어 전용)
     *
     * @param userId 사용자 식별자
     * @return {@link UserAuthInfo} 인증 정보
     */
    UserAuthInfo selectAuthInfoById(String userId);

    /**
     * 사용자 식별자를 기준으로 ResponseDTO를 직접 반환합니다.
     *
     * @param userId 사용자 식별자
     * @return {@link UserResponse} 사용자 응답 DTO
     */
    UserResponse selectResponseById(String userId);

    /**
     * 사용자 ID 기준 존재 확인 (카운트)
     *
     * @param userId 사용자 식별자
     * @return {@code int} 존재하면 1, 없으면 0
     */
    int countByUserId(String userId);

    /**
     * 직번(USER_SSN) 조회 (비밀번호 리셋용)
     *
     * @param userId 사용자 식별자
     * @return 직번 문자열
     */
    String selectUserSsnById(String userId);

    /**
     * 새로운 사용자를 생성합니다.
     *
     * @param dto             사용자 생성 요청 DTO
     * @param encodedPassword 암호화된 비밀번호
     * @param lastUpdateDtime 수정일시
     * @param lastUpdateUserId 수정자 ID
     */
    void insertUser(
            @Param("dto") UserCreateRequest dto,
            @Param("encodedPassword") String encodedPassword,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 기존 사용자를 수정합니다.
     *
     * @param userId           사용자 식별자
     * @param dto              사용자 수정 요청 DTO
     * @param encodedPassword  암호화된 비밀번호 (null이면 변경하지 않음)
     * @param lastUpdateDtime  수정일시
     * @param lastUpdateUserId 수정자 ID
     */
    void updateUser(
            @Param("userId") String userId,
            @Param("dto") UserUpdateRequest dto,
            @Param("encodedPassword") String encodedPassword,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로필 수정 (수정 가능 필드만)
     *
     * @param userId           사용자 식별자
     * @param dto              프로필 수정 요청 DTO
     * @param encodedPassword  암호화된 비밀번호 (null이면 변경하지 않음)
     * @param lastUpdateDtime  수정일시
     * @param lastUpdateUserId 수정자 ID
     */
    void updateProfile(
            @Param("userId") String userId,
            @Param("dto") ProfileUpdateRequest dto,
            @Param("encodedPassword") String encodedPassword,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 사용자 식별자를 기준으로 삭제합니다.
     *
     * @param userId 사용자 식별자
     */
    void deleteUserById(String userId);

    /**
     * 로그인 오류 횟수를 0으로 초기화하고 비밀번호를 직번으로 재설정합니다.
     *
     * @param userId            사용자 식별자
     * @param encodedPassword   암호화된 비밀번호
     * @param lastUpdateDtime   수정일시
     * @param lastUpdateUserId  수정자 ID
     */
    void resetLoginError(
            @Param("userId") String userId,
            @Param("encodedPassword") String encodedPassword,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * userName 기준 사용자 개수를 조회합니다.
     *
     * @param userName 사용자 아이디
     * @return {@code int} 동일 userName 개수
     */
    int countByUserName(String userName);

    /**
     * email 기준 사용자 개수를 조회합니다.
     *
     * @param email 이메일 주소
     * @return {@code int} 동일 email 개수
     */
    int countByEmail(String email);

    /**
     * 검색 조건을 적용하여 사용자 목록을 조회합니다.
     *
     * @param searchField         검색 대상 필드
     * @param searchValue         검색어
     * @param roleFilter          권한 필터
     * @param classNameFilter     클래스/조직 필터
     * @param userStateCodeFilter 사용자 상태 필터
     * @param sortBy              정렬 기준 필드
     * @param sortDirection       정렬 방향
     * @return {@link List} {@link UserWithRoleResponse} 검색 결과 목록
     */
    List<UserWithRoleResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("roleFilter") String roleFilter,
            @Param("classNameFilter") String classNameFilter,
            @Param("userStateCodeFilter") String userStateCodeFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 맞는 사용자 수를 조회합니다.
     */
    long countAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("roleFilter") String roleFilter,
            @Param("classNameFilter") String classNameFilter,
            @Param("userStateCodeFilter") String userStateCodeFilter);

    /**
     * 사용자 식별자를 기준으로
     * Role 정보를 포함한 사용자 정보를 조회합니다.
     *
     * @param userId 사용자 식별자
     * @return {@link UserWithRoleResponse} 사용자 정보
     */
    UserWithRoleResponse findByUserIdWithRole(@Param("userId") String userId);

    // ==================== 로그인 시도 추적 ====================

    void incrementLoginFailCount(
            @Param("userId") String userId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    Integer selectLoginFailCount(String userId);

    void resetLoginFailCount(
            @Param("userId") String userId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void lockUser(
            @Param("userId") String userId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * CMS 결재자 목록 조회 (v3_cms_admin_approvals 쓰기 권한 보유 활성 사용자)
     *
     * @return {@link List} {@link CmsApproverResponse} 결재자 목록
     */
    List<CmsApproverResponse> findCmsApprovers();

    /**
     * React CMS 결재자 목록 조회 (v3_react_cms_admin_approvals 쓰기 권한 보유 활성 사용자)
     *
     * @return {@link List} {@link CmsApproverResponse} 결재자 목록
     */
    List<CmsApproverResponse> findReactCmsApprovers();

    /**
     * 엑셀 내보내기용 전체 목록 조회 (페이징 없음)
     */
    List<UserWithRoleResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("roleFilter") String roleFilter,
            @Param("classNameFilter") String classNameFilter,
            @Param("userStateCodeFilter") String userStateCodeFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /** 권한이양 대상 사용자 검색 — userId·userName LIKE, 최대 20건. */
    List<UserSimpleResponse> searchForTransfer(@Param("keyword") String keyword);
}
