package com.pastebin.service;

import com.pastebin.entity.Paste;
import com.pastebin.repository.PasteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Paste 业务逻辑层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasteService {

    private final PasteRepository pasteRepository;

    /**
     * 保存 Paste
     * @param paste Paste 实体
     * @return 保存后的 Paste
     */
    @Transactional
    public Paste save(Paste paste) {
        return pasteRepository.save(paste);
    }

    /**
     * 根据 key 查询 Paste
     * @param key 唯一短码
     * @return Paste 对象（如果存在）
     */
    @Transactional(readOnly = true)
    public Optional<Paste> findByKey(String key) {
        return pasteRepository.findById(key);
    }

    /**
     * 阅后即焚：原子性获取并删除
     * 
     * 实现逻辑：
     * 1. 在事务中先查询 Paste
     * 2. 如果 isBurnAfterReading 为 true，立即删除
     * 3. 返回查询到的数据
     * 
     * @Transactional 确保查询和删除在同一事务中原子执行
     * 防止并发情况下多次读取
     * 
     * @param key 唯一短码
     * @return Paste 对象（如果存在）
     */
    @Transactional
    public Optional<Paste> getAndBurn(String key) {
        // 查询 Paste
        Optional<Paste> pasteOpt = pasteRepository.findById(key);
        
        if (pasteOpt.isPresent()) {
            Paste paste = pasteOpt.get();
            
            // 检查是否需要阅后即焚
            if (Boolean.TRUE.equals(paste.getIsBurnAfterReading())) {
                // 立即删除（事务提交时生效）
                pasteRepository.delete(paste);
                log.info("Burn after reading: Paste with key '{}' has been deleted", key);
            }
        }
        
        return pasteOpt;
    }

    /**
     * 定时清理过期的 Paste
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000) // 60000ms = 1分钟, 延迟10秒启动
    @Transactional
    public void cleanupExpiredPastes() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            int deletedCount = pasteRepository.deleteExpiredPastes(now);
            
            if (deletedCount > 0) {
                log.info("Scheduled cleanup: Deleted {} expired paste(s)", deletedCount);
            }
        } catch (Exception e) {
            log.debug("Cleanup skipped: {}", e.getMessage());
        }
    }
}
