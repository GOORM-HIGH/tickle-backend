package com.profect.tickle.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageFileDownloadDto {
    private String filePath;
    private String fileName;
    private String fileType;
    private Integer fileSize;
}