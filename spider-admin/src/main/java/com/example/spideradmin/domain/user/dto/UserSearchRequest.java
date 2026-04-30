package com.example.spideradmin.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User search request DTO
 * Contains search criteria for user filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    /**
     * Search field: "userName" or "userId"
     */
    private String searchField;

    /**
     * Search keyword
     */
    private String searchValue;

    /**
     * Role filter (roleId)
     */
    private String roleFilter;

    /**
     * Class name filter (직급)
     */
    private String classNameFilter;

    /**
     * User state code filter (사용자 상태)
     */
    private String userStateCodeFilter;
}
