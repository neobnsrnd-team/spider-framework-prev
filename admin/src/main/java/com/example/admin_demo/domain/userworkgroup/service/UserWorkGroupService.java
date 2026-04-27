package com.example.admin_demo.domain.userworkgroup.service;

import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupCreateRequest;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupResponse;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupUpdateRequest;
import com.example.admin_demo.domain.userworkgroup.mapper.UserWorkGroupMapper;
import com.example.admin_demo.global.exception.NotFoundException;
import com.example.admin_demo.global.util.AuditUtil;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h3>사용자 작업 그룹 서비스</h3>
 * <p>FWK_USER_WORK_GROUP 테이블 대상 그룹 CRUD 비즈니스 로직을 구현합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWorkGroupService {

    private final UserWorkGroupMapper userWorkGroupMapper;

    @Transactional
    public UserWorkGroupResponse createUserWorkGroup(UserWorkGroupCreateRequest dto) {
        log.info("Creating UserWorkGroup: userId={}, groupName={}", dto.getUserId(), dto.getGroupName());

        // GROUP_ID 자동 생성 (VARCHAR2(20) 제약으로 UUID 앞 20자 사용)
        String groupId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        userWorkGroupMapper.insertUserWorkGroup(dto, groupId, now, currentUserId);

        log.info("UserWorkGroup created: userId={}, groupId={}", dto.getUserId(), groupId);
        return userWorkGroupMapper.selectResponseByPk(dto.getUserId(), groupId);
    }

    @Transactional
    public UserWorkGroupResponse updateUserWorkGroup(UserWorkGroupUpdateRequest dto) {
        log.info("Updating UserWorkGroup: userId={}, groupId={}", dto.getUserId(), dto.getGroupId());

        if (userWorkGroupMapper.existsByPk(dto.getUserId(), dto.getGroupId()) == 0) {
            throw new NotFoundException("userId: " + dto.getUserId() + ", groupId: " + dto.getGroupId());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        userWorkGroupMapper.updateUserWorkGroup(dto, now, currentUserId);

        log.info("UserWorkGroup updated: userId={}, groupId={}", dto.getUserId(), dto.getGroupId());
        return userWorkGroupMapper.selectResponseByPk(dto.getUserId(), dto.getGroupId());
    }

    @Transactional
    public void deleteUserWorkGroup(String userId, String groupId) {
        log.info("Deleting UserWorkGroup: userId={}, groupId={}", userId, groupId);

        int deletedRows = userWorkGroupMapper.deleteUserWorkGroup(userId, groupId);
        if (deletedRows == 0) {
            throw new NotFoundException("userId: " + userId + ", groupId: " + groupId);
        }

        log.info("UserWorkGroup deleted: userId={}, groupId={}", userId, groupId);
    }

    public List<UserWorkGroupResponse> getUserWorkGroups(String userId) {
        log.info("Fetching UserWorkGroups: userId={}", userId);
        return userWorkGroupMapper.findAllByUserId(userId);
    }

    public UserWorkGroupResponse getUserWorkGroup(String userId, String groupId) {
        log.info("Fetching UserWorkGroup: userId={}, groupId={}", userId, groupId);

        UserWorkGroupResponse response = userWorkGroupMapper.selectResponseByPk(userId, groupId);
        if (response == null) {
            throw new NotFoundException("userId: " + userId + ", groupId: " + groupId);
        }
        return response;
    }
}
