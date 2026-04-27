package com.example.admin_demo.domain.workgroup.service;

import com.example.admin_demo.domain.workgroup.dto.WorkGroupCreateRequest;
import com.example.admin_demo.domain.workgroup.dto.WorkGroupResponse;
import com.example.admin_demo.domain.workgroup.dto.WorkGroupUpdateRequest;
import com.example.admin_demo.domain.workgroup.mapper.WorkGroupMapper;
import com.example.admin_demo.global.exception.InternalException;
import com.example.admin_demo.global.exception.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FWK_WORK_GROUP CRUD 서비스. */
@Service
@RequiredArgsConstructor
public class WorkGroupService {

    private final WorkGroupMapper workGroupMapper;

    /** userId 소속 그룹 목록 반환. */
    public List<WorkGroupResponse> getGroups(String userId) {
        return workGroupMapper.findByUserId(userId);
    }

    /** 그룹 생성 후 생성된 레코드 반환. */
    @Transactional
    public WorkGroupResponse createGroup(WorkGroupCreateRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", request.getUserId());
        params.put("groupName", request.getGroupName());
        params.put("groupDesc", request.getGroupDesc() != null ? request.getGroupDesc() : "");
        int affected = workGroupMapper.insert(params);
        if (affected == 0) {
            throw new InternalException("그룹 생성에 실패했습니다.");
        }
        return workGroupMapper.findLatestByUserId(request.getUserId());
    }

    /** 그룹명·설명 수정. */
    @Transactional
    public WorkGroupResponse updateGroup(WorkGroupUpdateRequest request) {
        int affected = workGroupMapper.update(request);
        if (affected == 0) {
            throw new NotFoundException("그룹을 찾을 수 없습니다: " + request.getGroupId());
        }
        return workGroupMapper.findByGroupId(request.getGroupId());
    }

    /** 그룹 삭제 — FWK_WORK_LIST에 참조된 그룹은 삭제 불가. */
    @Transactional
    public void deleteGroup(String groupId) {
        int affected = workGroupMapper.delete(groupId);
        if (affected == 0) {
            throw new InternalException("그룹을 삭제할 수 없습니다. 작업 항목이 남아 있거나 존재하지 않는 그룹입니다.");
        }
    }
}
