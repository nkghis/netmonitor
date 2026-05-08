package com.netwatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LinkStatus status = LinkStatus.UNKNOWN;

    @Column(name = "last_ping_at")
    private LocalDateTime lastPingAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "consecutive_failures")
    @Builder.Default
    private int consecutiveFailures = 0;

    @Column(name = "consecutive_successes")
    @Builder.Default
    private int consecutiveSuccesses = 0;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "uptime_percentage")
    @Builder.Default
    private double uptimePercentage = 0.0;

    @Column(name = "total_pings")
    @Builder.Default
    private long totalPings = 0;

    @Column(name = "successful_pings")
    @Builder.Default
    private long successfulPings = 0;

    @Column(name = "alert_sent")
    @Builder.Default
    private boolean alertSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void recordPingResult(boolean success, long responseTime) {
        this.lastPingAt = LocalDateTime.now();
        this.totalPings++;
        this.responseTimeMs = responseTime;

        if (success) {
            this.successfulPings++;
            this.consecutiveFailures = 0;
            this.consecutiveSuccesses++;
            this.lastSuccessAt = LocalDateTime.now();
            this.status = LinkStatus.UP;
        } else {
            this.consecutiveFailures++;
            this.consecutiveSuccesses = 0;
            this.lastFailureAt = LocalDateTime.now();
        }

        if (this.totalPings > 0) {
            this.uptimePercentage = (double) this.successfulPings / this.totalPings * 100.0;
        }
    }

    public enum LinkStatus {
        UP, DOWN, DEGRADED, UNKNOWN
    }
}
