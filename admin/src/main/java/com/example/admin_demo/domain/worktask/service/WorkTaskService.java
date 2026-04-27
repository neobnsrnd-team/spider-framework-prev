package com.example.admin_demo.domain.worktask.service;

import com.example.admin_demo.domain.worktask.dto.WorkTaskGroupMoveRequest;
import com.example.admin_demo.domain.worktask.dto.WorkTaskResponse;
import com.example.admin_demo.domain.worktask.dto.WorkTaskTransferRequest;
import com.example.admin_demo.domain.worktask.mapper.WorkTaskMapper;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h3>작업함 서비스</h3>
 * <p>작업함 목록 조회, 그룹 이동, 권한이양 비즈니스 로직을 구현합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkTaskService {

    private final WorkTaskMapper workTaskMapper;

    /**
     * 사용자의 작업함 목록을 조회합니다.
     * @param userId      사용자 ID
     * @param workGroupId 그룹 필터 (null 이면 전체)
     */
    public List<WorkTaskResponse> getWorkTasks(String userId, String workGroupId) {
        log.info("Fetching WorkTasks: userId={}, workGroupId={}", userId, workGroupId);
        return workTaskMapper.findWorkTasks(userId, workGroupId);
    }

    /**
     * 선택한 작업함 항목들을 다른 그룹으로 이동합니다.
     * FWK_USER_WORK_TASK UPSERT (Oracle MERGE INTO).
     */
    @Transactional
    public void moveGroup(WorkTaskGroupMoveRequest dto) {
        log.info(
                "Moving WorkTasks to group: userId={}, groupId={}, menuCount={}",
                dto.getUserId(),
                dto.getWorkGroupId(),
                dto.getMenuIds().size());

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        for (String menuId : dto.getMenuIds()) {
            workTaskMapper.upsertWorkTask(dto.getUserId(), menuId, dto.getWorkGroupId(), now, currentUserId);
        }

        log.info(
                "WorkTasks moved: userId={}, count={}",
                dto.getUserId(),
                dto.getMenuIds().size());
    }

    /**
     * 선택한 메뉴 권한을 다른 사용자에게 이양합니다.
     * 1. fromUserId 의 FWK_USER_MENU 레코드를 toUserId 로 복사 (미존재 시에만)
     * 2. fromUserId 의 FWK_USER_MENU 레코드 삭제
     * 3. fromUserId 의 FWK_USER_WORK_TASK 레코드 삭제
     */
    @Transactional
    public void transferAuthority(WorkTaskTransferRequest dto) {
        log.info(
                "Transferring authority: from={}, to={}, menuCount={}",
                dto.getFromUserId(),
                dto.getToUserId(),
                dto.getMenuIds().size());

        if (dto.getFromUserId().equals(dto.getToUserId())) {
            throw new InvalidInputException("이양 출처와 대상 사용자가 동일합니다.");
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 대상 사용자에게 메뉴 권한 복사
        workTaskMapper.insertUserMenuIfNotExists(
                dto.getFromUserId(), dto.getToUserId(), dto.getMenuIds(), now, currentUserId);

        // 출처 사용자의 메뉴 권한 삭제
        workTaskMapper.deleteUserMenuByMenuIds(dto.getFromUserId(), dto.getMenuIds());

        // 출처 사용자의 작업함 그룹 매핑 삭제
        workTaskMapper.deleteWorkTaskByMenuIds(dto.getFromUserId(), dto.getMenuIds());

        log.info(
                "Authority transferred: from={}, to={}, count={}",
                dto.getFromUserId(),
                dto.getToUserId(),
                dto.getMenuIds().size());
    }
}
