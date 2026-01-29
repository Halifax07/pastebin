package com.pastebin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 总结请求 DTO
 */
@Data
public class SummarizeRequest {
    
    @NotBlank(message = "内容不能为空")
    private String content;
}
