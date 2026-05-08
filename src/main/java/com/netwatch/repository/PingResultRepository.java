package com.netwatch.repository;

import com.netwatch.entity.PingResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PingResultRepository extends JpaRepository<PingResult, Long> {
    List<PingResult> findByLinkIdOrderByPingedAtDesc(Long linkId, Pageable pageable);

    @Query("SELECT p FROM PingResult p WHERE p.link.id = :linkId AND p.pingedAt >= :since ORDER BY p.pingedAt DESC")
    List<PingResult> findByLinkIdSince(Long linkId, LocalDateTime since);

    @Query("SELECT COUNT(p) FROM PingResult p WHERE p.link.id = :linkId AND p.success = false AND p.pingedAt >= :since")
    long countFailuresSince(Long linkId, LocalDateTime since);

    @Modifying
    @Query("DELETE FROM PingResult p WHERE p.pingedAt < :before")
    int deleteOlderThan(LocalDateTime before);

    @Modifying
    @Query("DELETE FROM PingResult p WHERE p.link.id = :linkId")
    int deleteByLinkId(Long linkId);

    @Query("SELECT p FROM PingResult p WHERE p.pingedAt >= :since ORDER BY p.pingedAt DESC")
    List<PingResult> findAllSince(LocalDateTime since);
}
