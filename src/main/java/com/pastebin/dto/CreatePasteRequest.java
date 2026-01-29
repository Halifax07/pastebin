package com.pastebin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 Paste 请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePasteRequest {

    @NotBlank(message = "内容不能为空")
    private String content;

    private String syntax;

    private Boolean isBurnAfterReading;

    private Integer expireMinutes;
}
