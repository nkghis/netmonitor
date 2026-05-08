package com.netwatch.repository;

import com.netwatch.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    List<Link> findByEnabledTrue();
    boolean existsByIpAddress(String ipAddress);

    @Query("SELECT l FROM Link l WHERE l.status = 'DOWN' OR l.status = 'DEGRADED'")
    List<Link> findUnhealthyLinks();

    @Query("SELECT AVG(l.uptimePercentage) FROM Link l WHERE l.enabled = true")
    Double findAverageUptimePercentage();
}
