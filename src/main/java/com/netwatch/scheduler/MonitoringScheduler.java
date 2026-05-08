package com.netwatch.scheduler;

import com.netwatch.entity.Link;
import com.netwatch.repository.LinkRepository;
import com.netwatch.repository.PingResultRepository;
import com.netwatch.service.PingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler {

    private final LinkRepository linkRepository;
    private final PingService pingService;
    private final PingResultRepository pingResultRepository;

    @Value("${netwatch.monitoring.ping-interval-seconds:60}")
    private int pingIntervalSeconds;

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    /**
     * Ping toutes les connexions activées en parallèle
     */
    @Scheduled(fixedRateString = "${netwatch.monitoring.ping-interval-seconds:60}000")
    public void pingAllLinks() {
        List<Link> links = linkRepository.findByEnabledTrue();

        if (links.isEmpty()) {
            log.debug("Aucun lien actif à monitorer");
            return;
        }

        log.debug("Démarrage du cycle de ping sur {} lien(s)", links.size());

        List<CompletableFuture<Void>> futures = links.stream()
            .map(link -> CompletableFuture.runAsync(() -> {
                try {
                    pingService.pingLink(link);
                } catch (Exception e) {
                    log.error("Erreur lors du ping de {} : {}", link.getIpAddress(), e.getMessage());
                }
            }, executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.debug("Cycle de ping terminé pour {} lien(s)", links.size());
    }

    /**
     * Nettoyage automatique des anciens résultats (tous les jours à 2h)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanOldPingResults() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = pingResultRepository.deleteOlderThan(threshold);
        log.info("Nettoyage : {} anciens résultats de ping supprimés (antérieurs au {})",
            deleted, threshold.toLocalDate());
    }
}
