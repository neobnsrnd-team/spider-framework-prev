package com.example.spider_admin.domain.emergencynotice.service;

import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeBulkSaveRequest;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeSaveRequest;
import com.example.spider_admin.domain.emergencynotice.mapper.EmergencyNoticeMapper;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 긴급공지 서비스
 *
 * FWK_PROPERTY 테이블의 'notice' 그룹(EMERGENCY_KO, EMERGENCY_EN, USE_YN)을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyNoticeService {

    private static final List<String> NOTICE_PROPERTY_IDS = List.of("EMERGENCY_KO", "EMERGENCY_EN", "USE_YN");

    private final EmergencyNoticeMapper emergencyNoticeMapper;

    /**
     * 언어별 긴급공지 목록과 노출 타입을 조회한다.
     *
     * @return 언어별 긴급공지 목록 (EMERGENCY_KO, EMERGENCY_EN)
     */
    public List<EmergencyNoticeResponse> getAll() {
        List<EmergencyNoticeResponse> notices = emergencyNoticeMapper.selectAll();
        if (notices == null || notices.isEmpty()) {
            throw new NotFoundException("긴급공지 데이터가 존재하지 않습니다. 초기 데이터를 확인해주세요.");
        }
        return notices;
    }

    /**
     * 노출 타입을 조회한다.
     *
     * @return 노출 타입 (A: 전체 / B: 기업 / C: 개인 / N: 사용안함)
     */
    public String getDisplayType() {
        String displayType = emergencyNoticeMapper.selectDisplayType();
        if (displayType == null) {
            throw new NotFoundException("긴급공지 노출 타입 데이터가 존재하지 않습니다. 초기 데이터를 확인해주세요.");
        }
        return displayType;
    }

    /**
     * 언어별 긴급공지와 노출 타입을 일괄 저장한다.
     *
     * <p>줄바꿈 마커({@code _$BR}) 변환은 프론트엔드에서 담당한다.
     * 백엔드는 전달받은 값을 변환 없이 그대로 저장·조회한다.
     *
     * @param request 언어별 공지 목록 + 노출 타입
     */
    @Transactional
    public void saveAll(EmergencyNoticeBulkSaveRequest request) {
        validateExistence();

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 언어별 긴급공지 저장 (propertyId 유효성은 DTO @Pattern에서 검증)
        for (EmergencyNoticeSaveRequest notice : request.getNotices()) {
            emergencyNoticeMapper.updateNotice(notice, now, userId);
            log.info("긴급공지 저장 완료: propertyId={}", notice.getPropertyId());
        }

        // 노출 타입 저장 (Enum → DB 저장용 문자열 변환)
        emergencyNoticeMapper.updateDisplayType(request.getDisplayType().name(), now, userId);
        log.info("긴급공지 노출 타입 저장 완료: displayType={}", request.getDisplayType());
    }

    /**
     * DB에 notice 그룹 3행이 모두 존재하는지 확인한다.
     * 초기 데이터가 없을 경우 NotFoundException을 던진다.
     */
    private void validateExistence() {
        for (String propertyId : NOTICE_PROPERTY_IDS) {
            if (emergencyNoticeMapper.countByPropertyId(propertyId) == 0) {
                throw new NotFoundException("FWK_PROPERTY 'notice." + propertyId + "' 데이터가 존재하지 않습니다."
                        + " docs/sql/oracle/03_insert_initial_data.sql의 초기 데이터를 먼저 실행해주세요.");
            }
        }
    }
}
