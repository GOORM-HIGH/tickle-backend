package com.profect.tickle.domain.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {

    private String fileName;        // 저장된 파일명
    private String originalName;    // 원본 파일명
    private String fileType;        // MIME 타입
    private Integer fileSize;       // 파일 크기
    private String displaySize;     // 표시용 크기
    private String filePath;        // 파일 경로
    private Boolean isImage;        // 이미지 여부

    public static FileUploadResponseDto of(String fileName, String originalName,
                                           String fileType, Integer fileSize, String filePath) {
        return FileUploadResponseDto.builder()
                .fileName(fileName)
                .originalName(originalName)
                .fileType(fileType)
                .fileSize(fileSize)
                .displaySize(formatFileSize(fileSize.longValue()))
                .filePath(filePath)
                .isImage(fileType != null && fileType.startsWith("image/"))
                .build();
    }

    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
