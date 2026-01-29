package com.pastebin.controller;

import com.pastebin.dto.SummarizeRequest;
import com.pastebin.dto.SummarizeResponse;
import com.pastebin.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 相关接口
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * AI 总结接口
     * POST /api/ai/summarize
     */
    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        log.info("收到 AI 总结请求，内容长度: {}", request.getContent().length());
        
        try {
            SummarizeResponse response = aiService.summarize(request.getContent());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("AI 总结失败", e);
            return ResponseEntity.internalServerError()
                    .body(SummarizeResponse.builder()
                            .summary("AI 总结失败: " + e.getMessage())
                            .tokens(0)
                            .build());
        }
    }
}
