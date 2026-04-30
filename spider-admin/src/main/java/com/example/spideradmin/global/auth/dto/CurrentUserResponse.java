package com.example.spideradmin.global.auth.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentUserResponse {

    private String userId;
    private String userName;
    private String roleId;
    private Set<String> authorities;
}
