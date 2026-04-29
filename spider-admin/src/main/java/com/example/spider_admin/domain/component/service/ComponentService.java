package com.example.spider_admin.domain.component.service;

import com.example.spider_admin.domain.component.dto.ComponentCreateRequest;
import com.example.spider_admin.domain.component.dto.ComponentResponse;
import com.example.spider_admin.domain.component.dto.ComponentSearchRequest;
import com.example.spider_admin.domain.component.dto.ComponentUpdateRequest;
import com.example.spider_admin.domain.component.mapper.ComponentMapper;
import com.example.spider_admin.global.aop.WorkListRecord;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 컴포넌트 관리 Service (FWK_COMPONENT + FWK_COMPONENT_PARAM 1:N) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentService {

    private final ComponentMapper componentMapper;

    /** 검색 조건으로 컴포넌트 목록 조회 (파라미터 미포함) */
    public PageResponse<ComponentResponse> getComponentsWithSearch(ComponentSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = componentMapper.countAllWithSearch(
                searchDTO.getComponentId(),
                searchDTO.getComponentName(),
                searchDTO.getComponentType(),
                searchDTO.getUseYn());

        List<ComponentResponse> components = componentMapper.findAllWithSearch(
                searchDTO.getComponentId(),
                searchDTO.getComponentName(),
                searchDTO.getComponentType(),
                searchDTO.getUseYn(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(components, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 컴포넌트 단건 조회 (파라미터 포함) */
    public ComponentResponse getById(String componentId) {
        ComponentResponse response = componentMapper.selectResponseById(componentId);
        if (response == null) {
            throw new NotFoundException("componentId: " + componentId);
        }
        response.setParams(componentMapper.findParamsByComponentId(componentId));
        return response;
    }

    /** 컴포넌트 등록 (파라미터 배치 등록 포함) */
    @WorkListRecord(
            workIdExpression = "#dto.componentType",
            crudType = "C",
            pkExpression = "#dto.componentId",
            workName = "컴포넌트")
    @Transactional
    public ComponentResponse create(ComponentCreateRequest dto) {
        try {
            componentMapper.insertComponent(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("componentId: " + dto.getComponentId());
        }
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            componentMapper.insertParamBatch(dto.getComponentId(), dto.getParams());
        }
        return getById(dto.getComponentId());
    }

    /** 컴포넌트 수정 (파라미터 DELETE → batch INSERT) */
    @WorkListRecord(
            workIdExpression = "#dto.componentType",
            crudType = "U",
            pkExpression = "#componentId",
            workName = "컴포넌트")
    @Transactional
    public ComponentResponse update(String componentId, ComponentUpdateRequest dto) {
        if (componentMapper.countById(componentId) == 0) {
            throw new NotFoundException("componentId: " + componentId);
        }
        componentMapper.updateComponent(componentId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        componentMapper.deleteParamsByComponentId(componentId);
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            componentMapper.insertParamBatch(componentId, dto.getParams());
        }
        return getById(componentId);
    }

    /** 컴포넌트 삭제 (FK 제약 상 파라미터 먼저 삭제).
     * componentType은 WorkListRecord 이력 적재에 사용된다 (컨트롤러에서 조회 후 전달).
     */
    @WorkListRecord(
            workIdExpression = "#componentType",
            crudType = "D",
            pkExpression = "#componentId",
            workName = "컴포넌트")
    @Transactional
    public void delete(String componentId, String componentType) {
        if (componentMapper.countById(componentId) == 0) {
            throw new NotFoundException("componentId: " + componentId);
        }
        componentMapper.deleteParamsByComponentId(componentId);
        componentMapper.deleteById(componentId);
    }
}
