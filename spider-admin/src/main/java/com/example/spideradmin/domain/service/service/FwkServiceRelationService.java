package com.example.spideradmin.domain.service.service;

import com.example.spideradmin.domain.service.dto.FwkServiceRelationItemRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationParamRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationSaveRequest;
import com.example.spideradmin.domain.service.mapper.FwkServiceMapper;
import com.example.spideradmin.domain.service.mapper.FwkServiceRelationMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서비스 연결 컴포넌트 관리 Service (FWK_SERVICE_RELATION + FWK_RELATION_PARAM) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FwkServiceRelationService {

    private final FwkServiceRelationMapper fwkServiceRelationMapper;
    private final FwkServiceMapper fwkServiceMapper;

    /** 서비스 연결 컴포넌트 + 파라미터 목록 조회 */
    public List<FwkServiceRelationResponse> getRelations(String serviceId) {
        if (fwkServiceMapper.countById(serviceId) == 0) {
            throw new NotFoundException("serviceId: " + serviceId);
        }
        return fwkServiceRelationMapper.findRelationsByServiceId(serviceId);
    }

    /**
     * 서비스 연결 컴포넌트 저장 (replace 전략).
     *
     * <p>저장 순서: FWK_RELATION_PARAM DELETE → FWK_SERVICE_RELATION DELETE → batch INSERT
     */
    @Transactional
    public List<FwkServiceRelationResponse> saveRelations(String serviceId, FwkServiceRelationSaveRequest dto) {
        if (fwkServiceMapper.countById(serviceId) == 0) {
            throw new NotFoundException("serviceId: " + serviceId);
        }

        fwkServiceRelationMapper.deleteParamsByServiceId(serviceId);
        fwkServiceRelationMapper.deleteRelationsByServiceId(serviceId);

        List<FwkServiceRelationItemRequest> relations = dto.getRelations() != null ? dto.getRelations() : List.of();

        if (!relations.isEmpty()) {
            fwkServiceRelationMapper.insertRelationBatch(
                    serviceId, relations, AuditUtil.now(), AuditUtil.currentUserId());

            List<FwkServiceRelationParamRequest> allParams = collectParams(serviceId, relations);
            if (!allParams.isEmpty()) {
                fwkServiceRelationMapper.insertRelationParamBatch(serviceId, allParams);
            }
        }

        return fwkServiceRelationMapper.findRelationsByServiceId(serviceId);
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────

    private List<FwkServiceRelationParamRequest> collectParams(
            String serviceId, List<FwkServiceRelationItemRequest> relations) {
        List<FwkServiceRelationParamRequest> result = new ArrayList<>();
        for (FwkServiceRelationItemRequest relation : relations) {
            if (relation.getParams() == null) {
                continue;
            }
            for (FwkServiceRelationParamRequest p : relation.getParams()) {
                result.add(FwkServiceRelationParamRequest.builder()
                        .serviceSeqNo(relation.getServiceSeqNo())
                        .componentId(relation.getComponentId())
                        .paramSeqNo(p.getParamSeqNo())
                        .paramValue(p.getParamValue())
                        .build());
            }
        }
        return result;
    }
}
