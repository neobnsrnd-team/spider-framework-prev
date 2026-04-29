package com.example.spider_admin.domain.worklist.service;

import com.example.spider_admin.domain.worklist.dto.WorkListApprovalRequest;
import com.example.spider_admin.domain.worklist.dto.WorkListGroupMoveRequest;
import com.example.spider_admin.domain.worklist.dto.WorkListResponse;
import com.example.spider_admin.domain.worklist.dto.WorkListTransferRequest;
import com.example.spider_admin.domain.worklist.mapper.FwkSettlementMapper;
import com.example.spider_admin.domain.worklist.mapper.WorkListMapper;
import com.example.spider_admin.global.exception.InvalidInputException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FWK_WORK_LIST 조회·그룹이동·권한이양·결재요청 서비스. */
@Service
@RequiredArgsConstructor
public class WorkListService {

    private final WorkListMapper workListMapper;
    private final FwkSettlementMapper fwkSettlementMapper;

    /** userId 기준 작업함 목록 조회. groupId가 없으면 전체 반환. */
    public List<WorkListResponse> getWorkList(String userId, String groupId) {
        return workListMapper.findByUserId(userId, groupId);
    }

    /** 선택 항목을 지정 그룹으로 이동. */
    @Transactional
    public void moveGroup(WorkListGroupMoveRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", request.getGroupId());
        params.put("workSeqs", request.getWorkSeqs());
        workListMapper.moveGroup(params);
    }

    /** 권한이양 — fromUserId의 선택 항목을 toUserId의 기본 그룹으로 이전. */
    @Transactional
    public void transfer(WorkListTransferRequest request) {
        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new InvalidInputException("이양 대상자는 본인이 될 수 없습니다.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("toUserId", request.getToUserId());
        params.put("workSeqs", request.getWorkSeqs());
        params.put("fromUserId", request.getFromUserId());
        workListMapper.transfer(params);
    }

    /**
     * 결재요청 — FWK_SETTLEMENT 레코드 생성 후 선택 항목의 APPROVAL_SEQ 갱신.
     *
     * @param request       결재요청 정보 (결재명, 설명, 결재자, 대상 항목)
     * @param presenterUserId 신청자 사용자 ID
     */
    @Transactional
    public void createApproval(WorkListApprovalRequest request, String presenterUserId) {
        String approvalId = fwkSettlementMapper.generateApprovalId();

        Map<String, Object> settlementParams = new HashMap<>();
        settlementParams.put("approvalId", approvalId);
        settlementParams.put("approvalName", request.getApprovalName());
        settlementParams.put("approvalDesc", request.getApprovalDesc());
        settlementParams.put("presenter", presenterUserId);
        settlementParams.put("finalManager", request.getFinalManager());
        fwkSettlementMapper.insert(settlementParams);

        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("approvalId", approvalId);
        updateParams.put("workSeqs", request.getWorkSeqs());
        workListMapper.updateApprovalSeq(updateParams);
    }
}
