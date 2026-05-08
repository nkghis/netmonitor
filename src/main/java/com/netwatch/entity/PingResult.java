package com.netwatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ping_results", indexes = {
    @Index(name = "idx_link_pinged_at", columnList = "link_id, pinged_at"),
    @Index(name = "idx_pinged_at", columnList = "pinged_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "pinged_at", nullable = false)
    private LocalDateTime pingedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (pingedAt == null) pingedAt = LocalDateTime.now();
    }
}
