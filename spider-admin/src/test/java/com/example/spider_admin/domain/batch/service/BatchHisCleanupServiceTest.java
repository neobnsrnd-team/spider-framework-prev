package com.example.spider_admin.domain.batch.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.spider_admin.domain.batch.mapper.BatchHisMapper;
import com.example.spider_admin.domain.batch.mapper.BatchHisPartitionMapper;
import com.example.spider_admin.global.config.SchedulingProperties;
import com.example.spider_admin.global.exception.InvalidInputException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchHisCleanupServiceTest {

    @Mock
    private BatchHisMapper batchHisMapper;

    @Mock
    private BatchHisPartitionMapper batchHisPartitionMapper;

    @Mock
    private SchedulingProperties schedulingProperties;

    @InjectMocks
    private BatchHisCleanupService service;

    private Method validatePartitionNameMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        validatePartitionNameMethod =
                BatchHisCleanupService.class.getDeclaredMethod("validatePartitionName", String.class);
        validatePartitionNameMethod.setAccessible(true);
    }

    // ─── validatePartitionName ──────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "DROP TABLE",
                "P_20240",
                "P_2024011",
                "X_202401",
                "'; DROP TABLE FWK_BATCH_HIS;--",
                "P_202401 OR 1=1",
                "P_abcdef"
            })
    @DisplayName("[파티션명 검증] 유효하지 않은 형식은 InvalidInputException이 발생한다")
    void validatePartitionName_invalidFormat_throwsException(String invalidName) {
        assertThatThrownBy(() -> validatePartitionNameMethod.invoke(service, invalidName))
                .hasCauseInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[파티션명 검증] 형식은 유효하지만 DB에 존재하지 않으면 InvalidInputException이 발생한다")
    void validatePartitionName_validFormatButNotExists_throwsException() {
        given(batchHisPartitionMapper.existsPartition("P_202401")).willReturn(0);

        assertThatThrownBy(() -> validatePartitionNameMethod.invoke(service, "P_202401"))
                .hasCauseInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[파티션명 검증] 유효한 형식이고 DB에 존재하면 예외 없이 통과한다")
    void validatePartitionName_validAndExists_noException() {
        given(batchHisPartitionMapper.existsPartition("P_202401")).willReturn(1);

        assertThatCode(() -> validatePartitionNameMethod.invoke(service, "P_202401"))
                .doesNotThrowAnyException();
    }
}
