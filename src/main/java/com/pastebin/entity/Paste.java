package com.pastebin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Paste 实体类
 * 存储加密后的文本内容（客户端加密，后端只存密文）
 */
@Entity
@Table(name = "paste", indexes = {
    @Index(name = "idx_expire_at", columnList = "expireAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paste {

    /**
     * 唯一短码，作为主键（例如：A1b2C）
     */
    @Id
    @Column(name = "paste_key", length = 20, nullable = false, unique = true)
    private String key;

    /**
     * 存储内容（已加密的密文或明文）
     * 使用 CLOB 类型存储长文本
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "CLOB")
    private String content;

    /**
     * 代码语言/语法高亮类型（如 java, python, javascript 等）
     */
    @Column(name = "syntax", length = 50)
    private String syntax;

    /**
     * 阅后即焚标记
     * true: 读取一次后立即删除
     * false: 正常保留直到过期
     */
    @Column(name = "is_burn_after_reading", nullable = false)
    private Boolean isBurnAfterReading;

    /**
     * 过期时间
     * null 表示永不过期
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /**
     * 创建时间（自动记录）
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
