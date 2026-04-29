package com.example.spider_admin.domain.accessuser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import com.example.spider_admin.domain.accessuser.dto.AccessUserResponse;
import com.example.spider_admin.domain.accessuser.mapper.AccessUserMapper;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessUserService 테스트")
class AccessUserServiceTest {

    @Mock
    private AccessUserMapper accessUserMapper;

    @InjectMocks
    private AccessUserService accessUserService;

    // ─── exportAccessUsers ────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀] 데이터가 있으면 xlsx 바이트 배열을 반환한다")
    void exportAccessUsers_withData_returnsBytes() {
        given(accessUserMapper.findAllWithSearch(
                        anyString(), anyString(), anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .willReturn(List.of(buildResponse("TRX001", "USR001"), buildResponse("TRX002", "USR002")));

        byte[] result = accessUserService.exportAccessUsers("TRX001", "T", "USR001");

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 데이터가 없어도 빈 xlsx 바이트 배열을 반환한다")
    void exportAccessUsers_emptyData_returnsBytes() {
        given(accessUserMapper.findAllWithSearch(isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .willReturn(List.of());

        byte[] result = accessUserService.exportAccessUsers(null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 행 수가 한도를 초과하면 InvalidInputException을 던진다")
    void exportAccessUsers_exceedsLimit_throwsInvalidInputException() {
        List<AccessUserResponse> overLimit =
                Collections.nCopies(ExcelExportUtil.MAX_ROW_LIMIT + 1, buildResponse("TRX001", "USR001"));
        given(accessUserMapper.findAllWithSearch(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(overLimit);

        assertThatThrownBy(() -> accessUserService.exportAccessUsers(null, null, null))
                .isInstanceOf(InvalidInputException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private AccessUserResponse buildResponse(String trxId, String custUserId) {
        return AccessUserResponse.builder()
                .gubunType("T")
                .trxId(trxId)
                .custUserId(custUserId)
                .useYn("Y")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
