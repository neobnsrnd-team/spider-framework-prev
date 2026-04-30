package com.example.spideradmin.global.util;

import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드/다운로드/삭제 유틸리티 클래스
 *
 * 업로드 경로는 application.yml의 file.upload-dir 설정으로 관리됩니다.
 * 설정이 없으면 기본값 ${user.dir}/uploads 사용
 */
@Slf4j
@Component
public class FileUtil {

    private static final String FILE_NAME_SEPARATOR = "_";
    private static final Pattern SAFE_PATH_COMPONENT = Pattern.compile("^[A-Za-z0-9_-]+$");

    private static String uploadBasePath;

    @Value("${file.upload-dir:${user.dir}/uploads}")
    public void setUploadBasePath(String path) {
        uploadBasePath = path;
        log.info("File upload base path configured: {}", uploadBasePath);
    }

    /**
     * 파일 업로드 (게시판ID/게시글ID 기반 경로)
     *
     * @param file 업로드할 파일
     * @param boardId 게시판 ID
     * @param articleSeq 게시글 순번
     * @return 저장된 파일 경로 ({boardId}/{articleSeq}/UUID_원본파일명.확장자)
     * @throws InvalidInputException 파일 경로가 유효하지 않을 때
     * @throws InternalException 파일 저장 실패 시
     */
    public static String uploadFile(MultipartFile file, String boardId, Long articleSeq) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // boardId 화이트리스트 검증 (경로 순회 차단)
        if (boardId == null || !SAFE_PATH_COMPONENT.matcher(boardId).matches()) {
            throw new InvalidInputException("유효하지 않은 게시판 ID: " + boardId);
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 경로 조작 공격 방지
        if (originalFileName.contains("..") || originalFileName.contains("/")) {
            throw new InvalidInputException("잘못된 파일 경로: " + originalFileName);
        }

        String storedFileName = UUID.randomUUID().toString() + FILE_NAME_SEPARATOR + originalFileName;

        try {
            Path baseDir = Paths.get(getUploadBasePath()).normalize();
            Path uploadPath =
                    baseDir.resolve(boardId).resolve(String.valueOf(articleSeq)).normalize();
            if (!uploadPath.startsWith(baseDir)) {
                throw new InvalidInputException("업로드 디렉토리 외부에 파일 저장 불가");
            }
            Files.createDirectories(uploadPath);

            Path destinationFile = uploadPath.resolve(storedFileName).normalize();

            // 저장 경로가 업로드 디렉토리 내에 있는지 확인
            if (!destinationFile.startsWith(uploadPath)) {
                throw new InvalidInputException("업로드 디렉토리 외부에 파일 저장 불가");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // DB에 저장될 상대 경로: {boardId}/{articleSeq}/UUID_원본파일명.확장자
            String storedPath = boardId + "/" + articleSeq + "/" + storedFileName;
            log.info("File uploaded successfully: {}", storedPath);

            return storedPath;

        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFileName, e);
            throw new InternalException("파일 저장 실패: " + originalFileName, e);
        }
    }

    /**
     * 파일 다운로드를 위한 Resource 반환
     *
     * @param filePath DB에 저장된 파일 경로
     * @return Resource
     * @throws InvalidInputException 파일 경로가 비어있을 때
     * @throws NotFoundException 파일을 찾을 수 없을 때
     * @throws InternalException 파일 경로 처리 중 오류 발생 시
     */
    public static Resource downloadFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new InvalidInputException("파일 경로가 비어있습니다");
        }

        try {
            Path baseDir = Paths.get(getUploadBasePath()).normalize();
            Path file = baseDir.resolve(filePath).normalize();
            if (!file.startsWith(baseDir)) {
                throw new InvalidInputException("업로드 디렉토리 외부 접근 불가");
            }
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new NotFoundException("파일: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new InternalException("파일 경로 오류: " + filePath, e);
        }
    }

    /**
     * 파일 삭제
     *
     * @param filePath DB에 저장된 파일 경로
     */
    public static void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path baseDir = Paths.get(getUploadBasePath()).normalize();
            Path file = baseDir.resolve(filePath).normalize();
            if (!file.startsWith(baseDir)) {
                throw new InvalidInputException("업로드 디렉토리 외부 접근 불가");
            }
            if (Files.deleteIfExists(file)) {
                log.info("File deleted: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }

    /**
     * 파일 존재 여부 확인
     *
     * @param filePath 파일 경로
     * @return 존재 여부
     */
    public static boolean exists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        Path baseDir = Paths.get(getUploadBasePath()).normalize();
        Path file = baseDir.resolve(filePath).normalize();
        if (!file.startsWith(baseDir)) {
            return false;
        }
        return Files.exists(file);
    }

    /**
     * 저장된 파일 경로에서 원본 파일명 추출
     *
     * @param filePath 저장된 파일 경로 (yyyyMMdd/UUID_원본파일명.확장자)
     * @return 원본 파일명
     */
    public static String extractOriginalFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        // yyyyMMdd/UUID_원본파일명.확장자 -> 원본파일명.확장자
        String fileName = Paths.get(filePath).getFileName().toString();
        int separatorIndex = fileName.indexOf(FILE_NAME_SEPARATOR);

        if (separatorIndex > 0 && separatorIndex < fileName.length() - 1) {
            return fileName.substring(separatorIndex + 1);
        }

        return fileName;
    }

    /**
     * 업로드 기본 경로 반환
     * 설정된 경로가 없으면 기본값 사용
     */
    private static String getUploadBasePath() {
        if (uploadBasePath == null || uploadBasePath.isBlank()) {
            return System.getProperty("user.dir") + "/uploads";
        }
        return uploadBasePath;
    }
}
