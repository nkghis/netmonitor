package com.netwatch.repository;

import com.netwatch.entity.AlertLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    List<AlertLog> findByOrderBySentAtDesc(Pageable pageable);
    List<AlertLog> findByLinkIdOrderBySentAtDesc(Long linkId, Pageable pageable);
    long countBySuccessTrue();
    long countBySuccessFalse();

    @Modifying
    @Query("DELETE FROM AlertLog a WHERE a.link.id = :linkId")
    int deleteByLinkId(Long linkId);
}
