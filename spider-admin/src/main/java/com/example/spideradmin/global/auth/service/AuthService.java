package com.example.spideradmin.global.auth.service;

import com.example.spideradmin.domain.menu.dto.MenuResponse;
import com.example.spideradmin.domain.menu.service.MenuService;
import com.example.spideradmin.domain.user.dto.UserResponse;
import com.example.spideradmin.domain.user.mapper.UserMapper;
import com.example.spideradmin.global.auth.dto.LoginRequest;
import com.example.spideradmin.global.auth.dto.LoginResponse;
import com.example.spideradmin.global.exception.ErrorType;
import com.example.spideradmin.global.exception.base.BaseException;
import com.example.spideradmin.global.security.dto.UserAuthInfo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserMapper userMapper;
    private final MenuService menuService;
    private final PasswordEncoder passwordEncoder;

    private static final String TIMING_SAFE_HASH =
            new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        UserAuthInfo authInfo = userMapper.selectAuthInfoById(loginRequest.getUserId());

        String invalidCredentialsMessage = "아이디 또는 비밀번호가 일치하지 않습니다";

        if (authInfo == null) {
            passwordEncoder.matches(loginRequest.getPassword(), TIMING_SAFE_HASH);
            throw new BaseException(ErrorType.UNAUTHORIZED, invalidCredentialsMessage);
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), authInfo.getPasswd())) {
            throw new BaseException(ErrorType.UNAUTHORIZED, invalidCredentialsMessage);
        }

        if (!"1".equals(authInfo.getUserStateCode())) {
            throw new BaseException(ErrorType.UNAUTHORIZED, invalidCredentialsMessage);
        }

        UserResponse user = userMapper.selectResponseById(loginRequest.getUserId());
        List<MenuResponse> menus = getMenusByUser();

        return LoginResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .roleId(user.getRoleId())
                .menus(menus)
                .build();
    }

    private List<MenuResponse> getMenusByUser() {
        // TODO: Role-Menu 및 User-Menu 매핑 기반으로 권한별 메뉴 필터링
        return menuService.getMenuTree();
    }

    public boolean validateCredentials(String userId, String password) {
        try {
            UserAuthInfo authInfo = userMapper.selectAuthInfoById(userId);

            if (authInfo == null) {
                passwordEncoder.matches(password, TIMING_SAFE_HASH);
                return false;
            }

            return passwordEncoder.matches(password, authInfo.getPasswd()) && "1".equals(authInfo.getUserStateCode());

        } catch (Exception e) {
            log.error("Error validating credentials for user: {}", userId, e);
            return false;
        }
    }

    public boolean hasMenuPermission(String userId, String menuId) {
        // TODO: User-Menu 및 Role-Menu 체크 로직 구현
        return true;
    }
}
