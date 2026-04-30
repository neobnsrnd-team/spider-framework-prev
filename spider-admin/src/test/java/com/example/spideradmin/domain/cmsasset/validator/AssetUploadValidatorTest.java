package com.example.spideradmin.domain.cmsasset.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.spideradmin.global.exception.InvalidInputException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("AssetUploadValidator 테스트")
class AssetUploadValidatorTest {

    private final AssetUploadValidator validator = new AssetUploadValidator();

    // ─── 매직 바이트 샘플 ───────────────────────────────────────────

    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] JPEG_MAGIC = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] GIF89A_MAGIC = {'G', 'I', 'F', '8', '9', 'a', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] WEBP_MAGIC = {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};
    private static final byte[] EXE_MAGIC = {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0, 0};

    // ─── 정상 케이스 ────────────────────────────────────────────────

    @Test
    @DisplayName("[정상] PNG 파일")
    void validate_png_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_MAGIC);
        assertThat(validator.validate(file)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("[정상] JPEG — .jpg 확장자")
    void validate_jpgExt_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_MAGIC);
        assertThat(validator.validate(file)).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("[정상] JPEG — .jpeg 확장자")
    void validate_jpegExt_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpeg", "image/jpeg", JPEG_MAGIC);
        assertThat(validator.validate(file)).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("[정상] GIF")
    void validate_gif_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", GIF89A_MAGIC);
        assertThat(validator.validate(file)).isEqualTo("image/gif");
    }

    @Test
    @DisplayName("[정상] WebP")
    void validate_webp_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", WEBP_MAGIC);
        assertThat(validator.validate(file)).isEqualTo("image/webp");
    }

    // ─── 실패 케이스 ────────────────────────────────────────────────

    @Test
    @DisplayName("[실패] 파일 없음")
    void validate_empty_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidInputException.class)
                .extracting("detailMessage", InstanceOfAssertFactories.STRING)
                .contains("파일을 선택");
    }

    @Test
    @DisplayName("[실패] 20MB 초과")
    void validate_oversize_throws() {
        byte[] big = new byte[(int) (AssetUploadValidator.MAX_FILE_SIZE_BYTES + 1)];
        System.arraycopy(PNG_MAGIC, 0, big, 0, PNG_MAGIC.length);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", big);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidInputException.class)
                .extracting("detailMessage", InstanceOfAssertFactories.STRING)
                .contains("20MB");
    }

    @Test
    @DisplayName("[실패] SVG 확장자는 허용되지 않음")
    void validate_svg_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidInputException.class)
                .extracting("detailMessage", InstanceOfAssertFactories.STRING)
                .contains("허용하지 않는 파일 형식");
    }

    @Test
    @DisplayName("[실패] 확장자 없음")
    void validate_noExt_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "noext", "image/png", PNG_MAGIC);
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[실패] 확장자는 .png 인데 실제 바이트는 EXE — 이미지로 식별되지 않음")
    void validate_mismatch_exeAsPng_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", EXE_MAGIC);
        // EXE 매직바이트(MZ)는 허용 이미지 포맷 중 어느 것과도 매치되지 않으므로
        // "확장자/포맷 불일치" 단계보다 앞서 "유효한 이미지 파일이 아닙니다" 로 거부된다.
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidInputException.class)
                .extracting("detailMessage", InstanceOfAssertFactories.STRING)
                .contains("유효한 이미지 파일");
    }

    @Test
    @DisplayName("[실패] 확장자는 .png 인데 실제 바이트는 JPEG (cross-format swap)")
    void validate_mismatch_jpegAsPng_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", JPEG_MAGIC);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidInputException.class)
                .extracting("detailMessage", InstanceOfAssertFactories.STRING)
                .contains("확장자와 실제 형식");
    }
}
