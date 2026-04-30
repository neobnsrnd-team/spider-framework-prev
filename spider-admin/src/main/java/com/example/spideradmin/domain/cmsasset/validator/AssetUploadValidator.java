package com.example.spideradmin.domain.cmsasset.validator;

import com.example.spideradmin.global.exception.InvalidInputException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * CMS 이미지 업로드 검증기 — Issue #65.
 *
 * <p>CMS 측 MIME 검증이 없으므로 Admin 이 유일한 방어선이다.
 * 확장자 화이트리스트 + 매직바이트 스니프 + 크기 상한 3단 검증을 수행한다.
 * 클라이언트가 보낸 Content-Type 은 로깅만 하고 신뢰하지 않는다.
 */
@Slf4j
@Component
public class AssetUploadValidator {

    /** 최대 허용 파일 크기 (20MB) */
    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    /** 허용 확장자 (소문자) */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    /** 매직바이트 검사 시 읽어올 최대 바이트 수 (WebP 식별을 위해 최소 12바이트 필요) */
    private static final int MAGIC_BYTE_READ_SIZE = 12;

    /**
     * 업로드 파일을 검증한다. 실패 시 {@link InvalidInputException} 을 던진다.
     *
     * @param file 멀티파트 파일
     * @return 매직바이트로 식별된 MIME 타입 (image/png, image/jpeg, image/gif, image/webp)
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("업로드할 파일을 선택하세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidInputException(
                    String.format("파일 크기는 %dMB 이하로 업로드하세요.", MAX_FILE_SIZE_BYTES / (1024 * 1024)));
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidInputException("허용하지 않는 파일 형식입니다. (png, jpg, jpeg, gif, webp 만 허용)");
        }

        byte[] header = readHeaderBytes(file);
        String sniffedMime = sniffMimeType(header);
        if (sniffedMime == null) {
            throw new InvalidInputException("유효한 이미지 파일이 아닙니다.");
        }

        List<String> expectedExtensionGroup = mimeToExtensionGroup(sniffedMime);
        if (!expectedExtensionGroup.contains(extension)) {
            // 확장자-실제 포맷 불일치 (예: .exe 를 .png 로 rename)
            log.warn("확장자({}) 와 매직바이트({}) 불일치 — 업로드 거부. 파일명={}", extension, sniffedMime, file.getOriginalFilename());
            throw new InvalidInputException("파일 확장자와 실제 형식이 일치하지 않습니다.");
        }

        log.debug("업로드 검증 통과: filename={}, size={}, mime={}", file.getOriginalFilename(), file.getSize(), sniffedMime);
        return sniffedMime;
    }

    /** 파일명에서 소문자 확장자 추출. 확장자 없으면 빈 문자열. */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }

    /** 파일 헤더의 첫 N 바이트를 읽어 반환. IO 오류 시 InvalidInputException. */
    private byte[] readHeaderBytes(MultipartFile file) {
        // try-with-resources 로 스트림을 명시적으로 닫아 리소스 누수를 방지한다.
        try (InputStream is = file.getInputStream()) {
            byte[] buf = new byte[MAGIC_BYTE_READ_SIZE];
            int read = is.readNBytes(buf, 0, MAGIC_BYTE_READ_SIZE);
            if (read < 3) {
                throw new InvalidInputException("파일을 읽을 수 없습니다.");
            }
            return Arrays.copyOf(buf, read);
        } catch (IOException e) {
            throw new InvalidInputException("파일을 읽는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 매직바이트로 MIME 타입 식별.
     *
     * <ul>
     *   <li>PNG: 89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>JPEG: FF D8 FF</li>
     *   <li>GIF: 47 49 46 38 (37|39) 61 ("GIF87a" / "GIF89a")</li>
     *   <li>WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50 ("RIFF....WEBP")</li>
     * </ul>
     */
    private String sniffMimeType(byte[] h) {
        if (h.length >= 8
                && h[0] == (byte) 0x89
                && h[1] == (byte) 0x50
                && h[2] == (byte) 0x4E
                && h[3] == (byte) 0x47
                && h[4] == (byte) 0x0D
                && h[5] == (byte) 0x0A
                && h[6] == (byte) 0x1A
                && h[7] == (byte) 0x0A) {
            return "image/png";
        }
        if (h.length >= 3 && h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (h.length >= 6
                && h[0] == 'G'
                && h[1] == 'I'
                && h[2] == 'F'
                && h[3] == '8'
                && (h[4] == '7' || h[4] == '9')
                && h[5] == 'a') {
            return "image/gif";
        }
        if (h.length >= 12
                && h[0] == 'R'
                && h[1] == 'I'
                && h[2] == 'F'
                && h[3] == 'F'
                && h[8] == 'W'
                && h[9] == 'E'
                && h[10] == 'B'
                && h[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /** 매직바이트로 식별된 MIME 을 허용 확장자 리스트로 매핑 (jpeg 는 jpg/jpeg 모두 허용). */
    private List<String> mimeToExtensionGroup(String mime) {
        return switch (mime) {
            case "image/png" -> List.of("png");
            case "image/jpeg" -> List.of("jpg", "jpeg");
            case "image/gif" -> List.of("gif");
            case "image/webp" -> List.of("webp");
            default -> List.of();
        };
    }
}
