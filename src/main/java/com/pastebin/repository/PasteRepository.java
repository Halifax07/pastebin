package com.pastebin.repository;

import com.pastebin.entity.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paste 数据访问层
 */
@Repository
public interface PasteRepository extends JpaRepository<Paste, String> {

    /**
     * 查找所有已过期的 Paste
     * @param now 当前时间
     * @return 已过期的 Paste 列表
     */
    List<Paste> findByExpireAtBefore(LocalDateTime now);

    /**
     * 批量删除已过期的 Paste
     * @param now 当前时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM Paste p WHERE p.expireAt < :now")
    int deleteExpiredPastes(LocalDateTime now);
}
