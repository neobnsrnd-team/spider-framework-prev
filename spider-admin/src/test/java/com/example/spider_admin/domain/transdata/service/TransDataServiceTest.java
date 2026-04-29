package com.example.spider_admin.domain.transdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.spider_admin.domain.transdata.dto.TransDataTimesResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataTimesSearchRequest;
import com.example.spider_admin.domain.transdata.mapper.TransDataHisMapper;
import com.example.spider_admin.domain.transdata.mapper.TransDataTimesMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransDataService 테스트")
class TransDataServiceTest {

    @Mock
    private TransDataTimesMapper transDataTimesMapper;

    @Mock
    private TransDataHisMapper transDataHisMapper;

    @InjectMocks
    private TransDataService transDataService;

    @Test
    @DisplayName("[엑셀] 정상 데이터로 엑셀 바이트 배열을 반환한다")
    void exportTransDataTimes_withData_returnsBytes() {
        TransDataTimesSearchRequest search =
                TransDataTimesSearchRequest.builder().userId("test").build();

        List<TransDataTimesResponse> data = List.of(
                TransDataTimesResponse.builder()
                        .tranSeq(1L)
                        .userId("test")
                        .tranReason("테스트")
                        .tranTime("20260310100000")
                        .tranResult("S")
                        .totalCount(5L)
                        .failCount(0L)
                        .successCount(5L)
                        .build(),
                TransDataTimesResponse.builder()
                        .tranSeq(2L)
                        .userId("test")
                        .tranReason("테스트2")
                        .tranTime("20260310110000")
                        .tranResult("F")
                        .totalCount(3L)
                        .failCount(1L)
                        .successCount(2L)
                        .build());

        given(transDataTimesMapper.findAllForExport("test", null, null, null)).willReturn(data);

        byte[] result = transDataService.exportTransDataTimes(search, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 빈 데이터로 엑셀을 생성해도 성공한다")
    void exportTransDataTimes_emptyData_returnsBytes() {
        TransDataTimesSearchRequest search =
                TransDataTimesSearchRequest.builder().build();

        given(transDataTimesMapper.findAllForExport(null, null, "tranTime", "DESC"))
                .willReturn(List.of());

        byte[] result = transDataService.exportTransDataTimes(search, "tranTime", "DESC");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("[엑셀] enrichTimesDTO로 tranResultName이 설정된다")
    void exportTransDataTimes_enrichesTranResultName() {
        TransDataTimesSearchRequest search =
                TransDataTimesSearchRequest.builder().build();

        TransDataTimesResponse item = TransDataTimesResponse.builder()
                .tranSeq(1L)
                .tranResult("S")
                .totalCount(1L)
                .failCount(0L)
                .successCount(1L)
                .build();

        given(transDataTimesMapper.findAllForExport(null, null, null, null)).willReturn(List.of(item));

        byte[] result = transDataService.exportTransDataTimes(search, null, null);

        assertThat(result).isNotNull();
        // enrichTimesDTO가 호출되어 tranResultName이 설정됨
        assertThat(item.getTranResultName()).isEqualTo("성공");
    }
}
