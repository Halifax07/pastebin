package com.pastebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Paste 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasteResponse {

    private String key;

    private String content;

    private String syntax;

    private Boolean isBurnAfterReading;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
