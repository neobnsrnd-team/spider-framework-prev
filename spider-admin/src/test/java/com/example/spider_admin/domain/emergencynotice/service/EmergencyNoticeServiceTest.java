package com.example.spider_admin.domain.emergencynotice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.emergencynotice.DisplayType;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeBulkSaveRequest;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeSaveRequest;
import com.example.spider_admin.domain.emergencynotice.mapper.EmergencyNoticeMapper;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyNoticeService 테스트")
class EmergencyNoticeServiceTest {

    @Mock
    private EmergencyNoticeMapper emergencyNoticeMapper;

    @InjectMocks
    private EmergencyNoticeService emergencyNoticeService;

    // ─── getAll ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 긴급공지 목록이 존재하면 List를 반환해야 한다")
    void getAll_exists_returnsList() {
        List<EmergencyNoticeResponse> data = List.of(buildResponse("EMERGENCY_KO"), buildResponse("EMERGENCY_EN"));
        given(emergencyNoticeMapper.selectAll()).willReturn(data);

        List<EmergencyNoticeResponse> result = emergencyNoticeService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPropertyId()).isEqualTo("EMERGENCY_KO");
        assertThat(result.get(1).getPropertyId()).isEqualTo("EMERGENCY_EN");
    }

    @Test
    @DisplayName("[조회] 초기 데이터가 없으면 NotFoundException을 발생시켜야 한다")
    void getAll_empty_throwsNotFoundException() {
        given(emergencyNoticeMapper.selectAll()).willReturn(List.of());

        assertThatThrownBy(() -> emergencyNoticeService.getAll()).isInstanceOf(NotFoundException.class);
    }

    // ─── getDisplayType ───────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 노출 타입이 존재하면 String을 반환해야 한다")
    void getDisplayType_exists_returnsString() {
        given(emergencyNoticeMapper.selectDisplayType()).willReturn("N");

        String result = emergencyNoticeService.getDisplayType();

        assertThat(result).isEqualTo("N");
    }

    @Test
    @DisplayName("[조회] 노출 타입 데이터가 없으면 NotFoundException을 발생시켜야 한다")
    void getDisplayType_null_throwsNotFoundException() {
        given(emergencyNoticeMapper.selectDisplayType()).willReturn(null);

        assertThatThrownBy(() -> emergencyNoticeService.getDisplayType()).isInstanceOf(NotFoundException.class);
    }

    // ─── saveAll ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[저장] 유효한 요청으로 저장 시 updateNotice와 updateDisplayType이 호출되어야 한다")
    void saveAll_valid_callsUpdateMethods() {
        EmergencyNoticeBulkSaveRequest request = buildBulkSaveRequest();
        given(emergencyNoticeMapper.countByPropertyId(anyString())).willReturn(1);

        emergencyNoticeService.saveAll(request);

        then(emergencyNoticeMapper).should().updateNotice(any(), anyString(), anyString());
        then(emergencyNoticeMapper).should().updateDisplayType(eq("N"), anyString(), anyString());
    }

    @Test
    @DisplayName("[저장] 복수 공지 저장 시 언어 수만큼 updateNotice가 호출되어야 한다")
    void saveAll_multipleNotices_callsUpdateNoticeForEach() {
        EmergencyNoticeBulkSaveRequest request = EmergencyNoticeBulkSaveRequest.builder()
                .notices(List.of(buildSaveRequest("EMERGENCY_KO"), buildSaveRequest("EMERGENCY_EN")))
                .displayType(DisplayType.A)
                .build();
        given(emergencyNoticeMapper.countByPropertyId(anyString())).willReturn(1);

        emergencyNoticeService.saveAll(request);

        then(emergencyNoticeMapper).should(org.mockito.Mockito.times(2)).updateNotice(any(), anyString(), anyString());
        then(emergencyNoticeMapper).should().updateDisplayType(eq("A"), anyString(), anyString());
    }

    @Test
    @DisplayName("[저장] 초기 데이터가 없으면 NotFoundException을 발생시켜야 한다")
    void saveAll_missingInitialData_throwsNotFoundException() {
        EmergencyNoticeBulkSaveRequest request = buildBulkSaveRequest();
        given(emergencyNoticeMapper.countByPropertyId(anyString())).willReturn(0);

        assertThatThrownBy(() -> emergencyNoticeService.saveAll(request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[저장] _$BR 마커는 백엔드에서 변환 없이 그대로 저장된다 (변환은 프론트엔드 담당)")
    void saveAll_lineBreakMarkerPassthrough() {
        EmergencyNoticeBulkSaveRequest request = EmergencyNoticeBulkSaveRequest.builder()
                .notices(List.of(EmergencyNoticeSaveRequest.builder()
                        .propertyId("EMERGENCY_KO")
                        .title("제목")
                        .content("첫 줄_$BR둘째 줄") // 프론트에서 \n → _$BR 변환 후 전달한 값
                        .build()))
                .displayType(DisplayType.N)
                .build();
        given(emergencyNoticeMapper.countByPropertyId(anyString())).willReturn(1);

        emergencyNoticeService.saveAll(request);

        // 서비스가 _$BR 을 별도 변환 없이 mapper 에 그대로 넘기는지 검증
        ArgumentCaptor<EmergencyNoticeSaveRequest> captor = ArgumentCaptor.forClass(EmergencyNoticeSaveRequest.class);
        then(emergencyNoticeMapper).should().updateNotice(captor.capture(), anyString(), anyString());
        assertThat(captor.getValue().getContent()).isEqualTo("첫 줄_$BR둘째 줄");
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private EmergencyNoticeResponse buildResponse(String propertyId) {
        return EmergencyNoticeResponse.builder()
                .propertyId(propertyId)
                .title("긴급공지 제목")
                .content("긴급공지 내용")
                .lastUpdateDtime("20260413120000")
                .lastUpdateUserId("admin")
                .build();
    }

    private EmergencyNoticeSaveRequest buildSaveRequest(String propertyId) {
        return EmergencyNoticeSaveRequest.builder()
                .propertyId(propertyId)
                .title("긴급공지 제목")
                .content("긴급공지 내용")
                .build();
    }

    private EmergencyNoticeBulkSaveRequest buildBulkSaveRequest() {
        return EmergencyNoticeBulkSaveRequest.builder()
                .notices(List.of(buildSaveRequest("EMERGENCY_KO")))
                .displayType(DisplayType.N)
                .build();
    }
}
