package com.example.spideradmin.domain.user.service;

import com.example.spideradmin.domain.role.mapper.RoleMapper;
import com.example.spideradmin.domain.user.dto.ProfileResponse;
import com.example.spideradmin.domain.user.dto.ProfileUpdateRequest;
import com.example.spideradmin.domain.user.dto.UserCreateRequest;
import com.example.spideradmin.domain.user.dto.UserResponse;
import com.example.spideradmin.domain.user.dto.UserSearchRequest;
import com.example.spideradmin.domain.user.dto.UserSimpleResponse;
import com.example.spideradmin.domain.user.dto.UserUpdateRequest;
import com.example.spideradmin.domain.user.dto.UserWithRoleResponse;
import com.example.spideradmin.domain.user.mapper.UserMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.SecurityUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserWithRoleResponse> searchUsers(PageRequest pageRequest, UserSearchRequest searchDTO) {
        long total = userMapper.countAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getRoleFilter(),
                searchDTO.getClassNameFilter(),
                searchDTO.getUserStateCodeFilter());

        List<UserWithRoleResponse> users = userMapper.findAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getRoleFilter(),
                searchDTO.getClassNameFilter(),
                searchDTO.getUserStateCodeFilter(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(users, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public UserResponse getUserById(String userId) {
        UserResponse user = userMapper.selectResponseById(userId);
        if (user == null) {
            throw new NotFoundException("userId: " + userId);
        }
        return user;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest requestDTO) {
        // 중복 체크
        if (userMapper.countByUserName(requestDTO.getUserName()) > 0) {
            throw new DuplicateException("userName: " + requestDTO.getUserName());
        }

        if (requestDTO.getEmail() != null && userMapper.countByEmail(requestDTO.getEmail()) > 0) {
            throw new DuplicateException("email: " + requestDTO.getEmail());
        }

        // Role 존재여부 검증
        validateRoleExists(requestDTO.getRoleId());

        // 비밀번호 인코딩
        String encodedPassword = encodePassword(requestDTO.getPassword());

        // 저장
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        userMapper.insertUser(requestDTO, encodedPassword, now, currentUserId);

        return userMapper.selectResponseById(requestDTO.getUserId());
    }

    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest requestDTO) {
        // 기존 사용자 조회
        UserResponse existing = userMapper.selectResponseById(userId);
        if (existing == null) {
            throw new NotFoundException("userId: " + userId);
        }

        // 사용자명 중복 체크 (자기 자신 제외)
        if (!Objects.equals(existing.getUserName(), requestDTO.getUserName())
                && userMapper.countByUserName(requestDTO.getUserName()) > 0) {
            throw new DuplicateException("userName: " + requestDTO.getUserName());
        }

        // 이메일 중복 체크 (자기 자신 제외)
        if (requestDTO.getEmail() != null
                && !Objects.equals(requestDTO.getEmail(), existing.getEmail())
                && userMapper.countByEmail(requestDTO.getEmail()) > 0) {
            throw new DuplicateException("email: " + requestDTO.getEmail());
        }

        // Role 존재여부 검증
        validateRoleExists(requestDTO.getRoleId());

        // 비밀번호 인코딩 (제공된 경우에만)
        String encodedPassword = encodePassword(requestDTO.getPassword());

        // 저장
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        userMapper.updateUser(userId, requestDTO, encodedPassword, now, currentUserId);

        return userMapper.selectResponseById(userId);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (userMapper.countByUserId(userId) == 0) {
            throw new NotFoundException("userId: " + userId);
        }

        userMapper.deleteUserById(userId);
    }

    public boolean existsByUserName(String userName) {
        return userMapper.countByUserName(userName) > 0;
    }

    public boolean existsByEmail(String email) {
        return userMapper.countByEmail(email) > 0;
    }

    @Transactional
    public void resetLoginError(String userId) {
        if (userMapper.countByUserId(userId) == 0) {
            throw new NotFoundException("userId: " + userId);
        }

        String userSsn = userMapper.selectUserSsnById(userId);
        String rawPassword = (userSsn != null && !userSsn.isBlank()) ? userSsn : userId;
        String encodedPassword = passwordEncoder.encode(rawPassword);
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        userMapper.resetLoginError(userId, encodedPassword, now, currentUserId);
    }

    public byte[] exportUsers(
            String searchField,
            String searchValue,
            String roleFilter,
            String classNameFilter,
            String userStateCodeFilter,
            String sortBy,
            String sortDirection) {
        List<UserWithRoleResponse> data = userMapper.findAllForExport(
                searchField, searchValue, roleFilter, classNameFilter, userStateCodeFilter, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("사용자ID", 15, "userId"),
                new ExcelColumnDefinition("사용자명", 15, "userName"),
                new ExcelColumnDefinition("직급", 12, "className"),
                new ExcelColumnDefinition("직번", 12, "userSsn"),
                new ExcelColumnDefinition("권한명", 15, "roleName"),
                new ExcelColumnDefinition("소속", 12, "positionName"),
                new ExcelColumnDefinition("사용자상태", 10, "userStateCode"),
                new ExcelColumnDefinition("수정일시", 18, "lastUpdateDtime"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (UserWithRoleResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", item.getUserId());
            row.put("userName", item.getUserName());
            row.put("className", item.getClassName());
            row.put("userSsn", item.getUserSsn());
            row.put("roleName", item.getRoleName());
            row.put("positionName", item.getPositionName());
            row.put("userStateCode", item.getUserStateCode());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("사용자", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    private void validateRoleExists(String roleId) {
        if (roleId != null && roleMapper.countByRoleId(roleId) == 0) {
            throw new NotFoundException("roleId: " + roleId);
        }
    }

    /**
     * 비밀번호가 제공된 경우 BCrypt 인코딩, 아닌 경우 null 반환
     */
    private String encodePassword(String rawPassword) {
        if (rawPassword != null && !rawPassword.isEmpty()) {
            return passwordEncoder.encode(rawPassword);
        }
        return null;
    }

    /**
     * 비밀번호 복잡성 규칙 검증
     * <p>규칙: 8~50자, 영문, 숫자, 특수문자(@$!%*#?&) 모두 포함</p>
     *
     * @param password 검증할 비밀번호
     * @return 유효하면 true, 아니면 false
     */
    private boolean isValidPasswordFormat(String password) {
        if (password == null || password.length() < 8 || password.length() > 50) {
            return false;
        }
        // 영문, 숫자, 특수문자(@$!%*#?&) 모두 포함 여부 검증
        String complexityRegex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,50}$";
        return password.matches(complexityRegex);
    }

    public ProfileResponse getProfile() {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new NotFoundException("userId: 현재 로그인 사용자");
        }

        UserWithRoleResponse user = userMapper.findByUserIdWithRole(currentUserId);
        if (user == null) {
            throw new NotFoundException("userId: " + currentUserId);
        }

        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest requestDTO) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new NotFoundException("userId: 현재 로그인 사용자");
        }

        // 기존 사용자 조회
        UserResponse existing = userMapper.selectResponseById(currentUserId);
        if (existing == null) {
            throw new NotFoundException("userId: " + currentUserId);
        }

        // 사용자ID 변경 시 중복 체크
        String newUserId = requestDTO.getUserId();
        boolean userIdChanged = !Objects.equals(currentUserId, newUserId);
        if (userIdChanged && userMapper.countByUserId(newUserId) > 0) {
            throw new DuplicateException("userId: " + newUserId);
        }

        // 비밀번호 검증 (새 비밀번호가 입력된 경우에만)
        if (requestDTO.getNewPassword() != null && !requestDTO.getNewPassword().isEmpty()) {
            // 비밀번호 복잡성 검증 (8~50자, 영문/숫자/특수문자 포함)
            if (!isValidPasswordFormat(requestDTO.getNewPassword())) {
                throw new InvalidInputException("비밀번호는 8~50자이며, 영문, 숫자, 특수문자(@$!%*#?&)를 모두 포함해야 합니다");
            }
            // 비밀번호 확인 일치 검증
            if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
                throw new InvalidInputException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
            }
        }

        // 사용자명 중복 체크 (자기 자신 제외)
        if (!Objects.equals(existing.getUserName(), requestDTO.getUserName())
                && userMapper.countByUserName(requestDTO.getUserName()) > 0) {
            throw new DuplicateException("userName: " + requestDTO.getUserName());
        }

        // 이메일 중복 체크 (자기 자신 제외)
        if (requestDTO.getEmail() != null
                && !Objects.equals(requestDTO.getEmail(), existing.getEmail())
                && userMapper.countByEmail(requestDTO.getEmail()) > 0) {
            throw new DuplicateException("email: " + requestDTO.getEmail());
        }

        // 비밀번호 인코딩 (제공된 경우에만)
        String encodedPassword = encodePassword(requestDTO.getNewPassword());

        // 저장
        String now = AuditUtil.now();
        String auditUserId = AuditUtil.currentUserId();
        userMapper.updateProfile(currentUserId, requestDTO, encodedPassword, now, auditUserId);

        // 업데이트된 정보 조회 후 반환
        String queryUserId = userIdChanged ? newUserId : currentUserId;
        UserWithRoleResponse updatedUser = userMapper.findByUserIdWithRole(queryUserId);
        return toProfileResponse(updatedUser);
    }

    /**
     * UserWithRoleResponse를 ProfileResponse로 변환합니다.
     */
    private ProfileResponse toProfileResponse(UserWithRoleResponse dto) {
        if (dto == null) {
            return null;
        }

        String userStateName = getUserStateName(dto.getUserStateCode());

        return ProfileResponse.builder()
                .userId(dto.getUserId())
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .userSsn(dto.getUserSsn())
                .className(dto.getClassName())
                .positionName(dto.getPositionName())
                .roleId(dto.getRoleId())
                .roleName(dto.getRoleName())
                .userStateCode(dto.getUserStateCode())
                .userStateName(userStateName)
                .accessIp(dto.getAccessIp())
                .lastUpdateDtime(dto.getLastUpdateDtime())
                .lastUpdateUserId(dto.getLastUpdateUserId())
                .build();
    }

    /**
     * 사용자 상태 코드를 상태명으로 변환합니다.
     */
    private String getUserStateName(String stateCode) {
        if (stateCode == null) {
            return null;
        }
        return switch (stateCode) {
            case "1" -> "정상";
            case "2" -> "삭제";
            case "3" -> "정지";
            default -> stateCode;
        };
    }

    /** 권한이양 대상 사용자 검색 — userId·userName LIKE, 최대 20건. */
    public List<UserSimpleResponse> searchForTransfer(String keyword) {
        return userMapper.searchForTransfer(keyword);
    }
}
