package com.example.spideradmin.domain.menu.mapper;

import com.example.spideradmin.domain.menu.dto.UserMenuResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMenuMapper {

    // ==================== 단건 조회 / 존재 확인 ====================

    /**
     * 복합키로 존재 여부 확인
     */
    int countByUserIdAndMenuId(@Param("userId") String userId, @Param("menuId") String menuId);

    // ==================== 생성 ====================

    /**
     * 사용자-메뉴 매핑 생성
     */
    void insert(
            @Param("userId") String userId,
            @Param("menuId") String menuId,
            @Param("authCode") String authCode,
            @Param("favorMenuOrder") int favorMenuOrder,
            @Param("now") String now,
            @Param("currentUserId") String currentUserId);

    // ==================== 수정 ====================

    /**
     * 사용자-메뉴 매핑 수정
     */
    void update(
            @Param("userId") String userId,
            @Param("menuId") String menuId,
            @Param("authCode") String authCode,
            @Param("favorMenuOrder") int favorMenuOrder,
            @Param("now") String now,
            @Param("currentUserId") String currentUserId);

    // ==================== 삭제 ====================

    void deleteById(@Param("userId") String userId, @Param("menuId") String menuId);

    void deleteByUserId(String userId);

    void deleteByMenuId(String menuId);

    // ==================== 조회 (Response DTO 반환) ====================

    /**
     * 복합키로 단건 조회 (상세 포함)
     */
    UserMenuResponse selectResponseById(@Param("userId") String userId, @Param("menuId") String menuId);

    /**
     * 사용자 ID로 메뉴 매핑 목록 조회 (메뉴 상세 포함)
     */
    List<UserMenuResponse> selectByUserIdWithDetails(String userId);

    /**
     * 즐겨찾기 메뉴 조회
     */
    List<UserMenuResponse> selectFavoriteMenusByUserId(String userId);

    /**
     * 메뉴 ID로 사용자-메뉴 매핑 목록 조회
     */
    List<UserMenuResponse> selectByMenuId(String menuId);

    /**
     * 사용자 ID + 권한 코드로 조회
     */
    List<UserMenuResponse> selectByUserIdAndAuthCode(
            @Param("userId") String userId, @Param("authCode") String authCode);

    // ==================== 권한 검사 ====================

    /**
     * 사용자가 특정 메뉴에 대해 가진 권한 코드를 조회
     * @return 권한 코드 ("R", "W") 또는 null (권한 없음)
     */
    String selectAuthCodeByUserIdAndMenuId(@Param("userId") String userId, @Param("menuId") String menuId);
}
