package com.example.spideradmin.global.security;

import com.example.spideradmin.domain.user.enums.UserState;
import com.example.spideradmin.domain.user.mapper.UserMapper;
import com.example.spideradmin.global.security.dto.AuthenticatedUser;
import com.example.spideradmin.global.security.dto.UserAuthInfo;
import com.example.spideradmin.global.security.provider.AuthorityProvider;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final AuthorityProvider authorityProvider;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserAuthInfo authInfo = userMapper.selectAuthInfoById(userId);
        if (authInfo == null) {
            throw new UsernameNotFoundException(userId);
        }

        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                authInfo.getUserName(),
                authInfo.getRoleId(),
                authInfo.getPasswd(),
                UserState.fromCode(authInfo.getUserStateCode()));
        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities(userId, authInfo.getRoleId());
        return new CustomUserDetails(user, authorities);
    }
}
