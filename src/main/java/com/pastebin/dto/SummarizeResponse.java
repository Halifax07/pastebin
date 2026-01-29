package com.pastebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 总结响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeResponse {
    
    /**
     * 总结内容
     */
    private String summary;
    
    /**
     * 使用的 tokens 数量
     */
    private Integer tokens;
}
