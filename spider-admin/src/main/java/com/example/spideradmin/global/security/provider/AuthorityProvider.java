package com.example.spideradmin.global.security.provider;

import java.util.Set;
import org.springframework.security.core.GrantedAuthority;

public interface AuthorityProvider {

    Set<GrantedAuthority> getAuthorities(String userId, String roleId);
}
