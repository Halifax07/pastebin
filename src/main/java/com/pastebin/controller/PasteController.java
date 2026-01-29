package com.pastebin.controller;

import com.pastebin.dto.CreatePasteRequest;
import com.pastebin.dto.CreatePasteResponse;
import com.pastebin.dto.PasteResponse;
import com.pastebin.entity.Paste;
import com.pastebin.service.PasteService;
import com.pastebin.util.KeyGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Paste 相关接口
 */
@RestController
@RequestMapping("/pastes")
@RequiredArgsConstructor
@Slf4j
public class PasteController {

    private final PasteService pasteService;

    /**
     * 创建 Paste
     * POST /api/pastes
     */
    @PostMapping
    public ResponseEntity<CreatePasteResponse> createPaste(@Valid @RequestBody CreatePasteRequest request) {
        log.info("收到创建 Paste 请求，内容长度: {}, 语法: {}, 阅后即焚: {}", 
                request.getContent().length(), 
                request.getSyntax(), 
                request.getIsBurnAfterReading());

        // 生成唯一短码
        String key = KeyGenerator.generate();

        // 计算过期时间
        LocalDateTime expireAt = null;
        if (request.getExpireMinutes() != null && request.getExpireMinutes() > 0) {
            expireAt = LocalDateTime.now().plusMinutes(request.getExpireMinutes());
        }

        // 构建实体
        Paste paste = Paste.builder()
                .key(key)
                .content(request.getContent())
                .syntax(request.getSyntax() != null ? request.getSyntax() : "plaintext")
                .isBurnAfterReading(request.getIsBurnAfterReading() != null ? request.getIsBurnAfterReading() : false)
                .expireAt(expireAt)
                .build();

        // 保存
        pasteService.save(paste);

        log.info("Paste 创建成功，key: {}", key);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreatePasteResponse.builder()
                        .key(key)
                        .url("/" + key)
                        .build());
    }

    /**
     * 获取 Paste
     * GET /api/pastes/{key}
     */
    @GetMapping("/{key}")
    public ResponseEntity<?> getPaste(@PathVariable String key) {
        log.info("获取 Paste，key: {}", key);

        // 使用阅后即焚方法获取（如果设置了阅后即焚，会自动删除）
        Optional<Paste> pasteOpt = pasteService.getAndBurn(key);

        if (pasteOpt.isEmpty()) {
            log.warn("Paste 不存在或已过期/删除，key: {}", key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("内容不存在或已过期"));
        }

        Paste paste = pasteOpt.get();

        // 检查是否过期
        if (paste.getExpireAt() != null && paste.getExpireAt().isBefore(LocalDateTime.now())) {
            log.warn("Paste 已过期，key: {}", key);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ErrorResponse("内容已过期"));
        }

        return ResponseEntity.ok(PasteResponse.builder()
                .key(paste.getKey())
                .content(paste.getContent())
                .syntax(paste.getSyntax())
                .isBurnAfterReading(paste.getIsBurnAfterReading())
                .expireAt(paste.getExpireAt())
                .createdAt(paste.getCreatedAt())
                .build());
    }

    /**
     * 错误响应
     */
    record ErrorResponse(String message) {}
}
