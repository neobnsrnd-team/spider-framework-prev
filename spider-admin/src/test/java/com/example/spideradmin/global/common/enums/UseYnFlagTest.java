package com.example.spideradmin.global.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.spideradmin.global.common.base.BaseEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <h3>UseYnFlag Enum 테스트</h3>
 * <p>TDD Red Phase: 테스트 먼저 작성</p>
 */
@DisplayName("UseYnFlag Enum 테스트")
class UseYnFlagTest {

    @Test
    @DisplayName("fromCode: 'Y'는 YES를 반환한다")
    void fromCode_Y_returnsYes() {
        // given
        String code = "Y";

        // when
        UseYnFlag result = UseYnFlag.fromCode(code);

        // then
        assertThat(result).isEqualTo(UseYnFlag.YES);
    }

    @Test
    @DisplayName("fromCode: 'N'은 NO를 반환한다")
    void fromCode_N_returnsNo() {
        // given
        String code = "N";

        // when
        UseYnFlag result = UseYnFlag.fromCode(code);

        // then
        assertThat(result).isEqualTo(UseYnFlag.NO);
    }

    @Test
    @DisplayName("fromCode: null은 null을 반환한다")
    void fromCode_null_returnsNull() {
        // when
        UseYnFlag result = UseYnFlag.fromCode(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromCode: 빈 문자열은 null을 반환한다")
    void fromCode_emptyString_returnsNull() {
        // when
        UseYnFlag result = UseYnFlag.fromCode("");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromCode: 공백은 null을 반환한다")
    void fromCode_blank_returnsNull() {
        // when
        UseYnFlag result = UseYnFlag.fromCode("   ");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromCode: 유효하지 않은 코드는 null을 반환한다")
    void fromCode_invalidCode_returnsNull() {
        // when
        UseYnFlag result = UseYnFlag.fromCode("INVALID");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromCode: 대소문자 구분 없이 동작한다 - 'y'")
    void fromCode_lowercase_y_returnsYes() {
        // when
        UseYnFlag result = UseYnFlag.fromCode("y");

        // then
        assertThat(result).isEqualTo(UseYnFlag.YES);
    }

    @Test
    @DisplayName("fromCode: 대소문자 구분 없이 동작한다 - 'n'")
    void fromCode_lowercase_n_returnsNo() {
        // when
        UseYnFlag result = UseYnFlag.fromCode("n");

        // then
        assertThat(result).isEqualTo(UseYnFlag.NO);
    }

    @Test
    @DisplayName("BaseEnum을 구현한다")
    void implementsBaseEnum() {
        // then
        assertThat(UseYnFlag.YES).isInstanceOf(BaseEnum.class);
        assertThat(UseYnFlag.NO).isInstanceOf(BaseEnum.class);
    }

    @Test
    @DisplayName("YES는 코드 'Y'를 가진다")
    void yes_hasCodeY() {
        // when
        String code = UseYnFlag.YES.getCode();

        // then
        assertThat(code).isEqualTo("Y");
    }

    @Test
    @DisplayName("NO는 코드 'N'을 가진다")
    void no_hasCodeN() {
        // when
        String code = UseYnFlag.NO.getCode();

        // then
        assertThat(code).isEqualTo("N");
    }

    @Test
    @DisplayName("YES는 설명을 가진다")
    void yes_hasDescription() {
        // when
        String description = UseYnFlag.YES.getDescription();

        // then
        assertThat(description).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("NO는 설명을 가진다")
    void no_hasDescription() {
        // when
        String description = UseYnFlag.NO.getDescription();

        // then
        assertThat(description).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("모든 enum 값이 유효한 code를 가진다")
    void allEnumValues_haveValidCode() {
        // when & then
        for (UseYnFlag flag : UseYnFlag.values()) {
            assertThat(flag.getCode()).isNotNull();
            assertThat(flag.getCode()).isNotBlank();
        }
    }

    @Test
    @DisplayName("모든 enum 값이 유효한 description을 가진다")
    void allEnumValues_haveValidDescription() {
        // when & then
        for (UseYnFlag flag : UseYnFlag.values()) {
            assertThat(flag.getDescription()).isNotNull();
            assertThat(flag.getDescription()).isNotBlank();
        }
    }

    @Test
    @DisplayName("모든 code는 fromCode로 역변환 가능하다")
    void allCodes_canBeConvertedBack() {
        // when & then
        for (UseYnFlag flag : UseYnFlag.values()) {
            UseYnFlag converted = UseYnFlag.fromCode(flag.getCode());
            assertThat(converted).isEqualTo(flag);
        }
    }

    @Test
    @DisplayName("YES.isTrue()는 true를 반환한다")
    void yes_isTrue_returnsTrue() {
        // when
        boolean result = UseYnFlag.YES.isTrue();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("NO.isTrue()는 false를 반환한다")
    void no_isTrue_returnsFalse() {
        // when
        boolean result = UseYnFlag.NO.isTrue();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("YES.isFalse()는 false를 반환한다")
    void yes_isFalse_returnsFalse() {
        // when
        boolean result = UseYnFlag.YES.isFalse();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("NO.isFalse()는 true를 반환한다")
    void no_isFalse_returnsTrue() {
        // when
        boolean result = UseYnFlag.NO.isFalse();

        // then
        assertThat(result).isTrue();
    }
}
