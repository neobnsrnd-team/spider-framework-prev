package com.example.spideradmin.domain.transdata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이행 데이터 파일 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransDataFileResponse {

    private String fileType; // 이행대상 항목 코드 (상위 폴더명)

    private String fileName; // 이행대상 파일명

    private Long fileSize; // 파일 사이즈 (bytes)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate; // 이행파일 생성일

    private String filePath; // 상대 경로 (basePath 기준, 미리보기용)
}
