package com.pastebin.service;

import com.pastebin.config.AIProperties;
import com.pastebin.dto.ChatRequest;
import com.pastebin.dto.ChatResponse;
import com.pastebin.dto.SummarizeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AI 服务层
 * 调用 OpenAI 兼容接口实现文本总结
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final AIProperties aiProperties;
    private final WebClient.Builder webClientBuilder;

    /**
     * 总结文本内容
     * @param content 待总结的内容
     * @return 总结结果
     */
    public SummarizeResponse summarize(String content) {
        log.info("开始 AI 总结，内容长度: {}", content.length());

        // 构建 Prompt
        String prompt = "请用简练的中文总结以下代码或文本的核心功能：\n\n" + content;

        // 构建请求
        ChatRequest request = ChatRequest.builder()
                .model(aiProperties.getModel())
                .maxTokens(aiProperties.getMaxTokens())
                .temperature(aiProperties.getTemperature())
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();

        // 调用 API
        WebClient webClient = webClientBuilder
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            ChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("AI 返回结果为空");
            }

            String summary = response.getChoices().get(0).getMessage().getContent();
            Integer tokens = response.getUsage() != null ? response.getUsage().getTotalTokens() : 0;

            log.info("AI 总结完成，使用 tokens: {}", tokens);

            return SummarizeResponse.builder()
                    .summary(summary)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("AI 总结失败", e);
            throw new RuntimeException("AI 总结失败: " + e.getMessage());
        }
    }
}
