package com.pastebin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIProperties {
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API Base URL
     */
    private String baseUrl;
    
    /**
     * AI 模型名称
     */
    private String model = "qwen-plus";
    
    /**
     * 最大生成 token 数
     */
    private Integer maxTokens = 500;
    
    /**
     * 温度参数（0-1，越高越随机）
     */
    private Double temperature = 0.7;
}
