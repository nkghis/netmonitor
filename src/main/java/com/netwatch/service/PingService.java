package com.netwatch.service;

import com.netwatch.entity.Link;
import com.netwatch.entity.PingResult;
import com.netwatch.repository.LinkRepository;
import com.netwatch.repository.PingResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PingService {

    private final LinkRepository linkRepository;
    private final PingResultRepository pingResultRepository;
    private final NotificationService notificationService;

    @Value("${netwatch.monitoring.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${netwatch.monitoring.alert-threshold:3}")
    private int alertThreshold;

    @Value("${netwatch.monitoring.recovery-check-count:2}")
    private int recoveryCheckCount;

    @Transactional
    public PingResult pingLink(Link link) {
        long startTime = System.currentTimeMillis();
        boolean reachable = false;
        String errorMessage = null;

        try {
            // ✅ Utilise le ping système au lieu de InetAddress.isReachable()
            ProcessBuilder pb = new ProcessBuilder(
                    "ping", "-c", "1", "-W",
                    String.valueOf(timeoutMs / 1000),
                    link.getIpAddress()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (finished) {
                reachable = process.exitValue() == 0;
            } else {
                process.destroyForcibly();
                reachable = false;
            }

            if (!reachable) {
                errorMessage = "Hôte injoignable (timeout " + timeoutMs + "ms)";
            }

        } catch (IOException e) {
            errorMessage = "Erreur réseau : " + e.getMessage();
            log.warn("Erreur ping sur {} ({}) : {}", link.getName(), link.getIpAddress(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage = "Ping interrompu";
        }

        long responseTime = System.currentTimeMillis() - startTime;

        PingResult result = PingResult.builder()
                .link(link)
                .success(reachable)
                .responseTimeMs(reachable ? responseTime : null)
                .pingedAt(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();

        pingResultRepository.save(result);

        Link.LinkStatus previousStatus = link.getStatus();
        link.recordPingResult(reachable, responseTime);
        handleStatusChange(link, previousStatus, reachable);
        linkRepository.save(link);

        log.debug("Ping {} ({}) : {} en {}ms",
                link.getName(), link.getIpAddress(),
                reachable ? "OK" : "FAIL",
                responseTime);

        return result;
    }
    /*public PingResult pingLink(Link link) {
        long startTime = System.currentTimeMillis();
        boolean reachable = false;
        String errorMessage = null;

        try {
            InetAddress address = InetAddress.getByName(link.getIpAddress());
            reachable = address.isReachable(timeoutMs);
            if (!reachable) {
                errorMessage = "Hôte injoignable (timeout " + timeoutMs + "ms)";
            }
        } catch (IOException e) {
            errorMessage = "Erreur réseau : " + e.getMessage();
            log.warn("Erreur ping sur {} ({}) : {}", link.getName(), link.getIpAddress(), e.getMessage());
        }

        long responseTime = System.currentTimeMillis() - startTime;

        PingResult result = PingResult.builder()
            .link(link)
            .success(reachable)
            .responseTimeMs(reachable ? responseTime : null)
            .pingedAt(LocalDateTime.now())
            .errorMessage(errorMessage)
            .build();

        pingResultRepository.save(result);

        Link.LinkStatus previousStatus = link.getStatus();
        link.recordPingResult(reachable, responseTime);

        // Détecter les changements d'état et déclencher les alertes
        handleStatusChange(link, previousStatus, reachable);

        linkRepository.save(link);

        log.debug("Ping {} ({}) : {} en {}ms",
            link.getName(), link.getIpAddress(),
            reachable ? "OK" : "FAIL",
            responseTime);

        return result;
    }*/

    private void handleStatusChange(Link link, Link.LinkStatus previousStatus, boolean success) {
        Link.LinkStatus newStatus = computeNewStatus(link, previousStatus, success);

        if (newStatus == previousStatus) {
            return;
        }

        link.setStatus(newStatus);

        switch (newStatus) {
            case DOWN -> {
                link.setAlertSent(true);
                log.warn("🔴 LIEN DOWN : {} ({}) - {} échecs consécutifs",
                        link.getName(), link.getIpAddress(), link.getConsecutiveFailures());
                notificationService.sendLinkDownAlert(link);
            }
            case DEGRADED -> {
                link.setAlertSent(true);
                log.warn("🟡 LIEN DÉGRADÉ : {} ({}) - {} échec(s) consécutif(s)",
                        link.getName(), link.getIpAddress(), link.getConsecutiveFailures());
                notificationService.sendLinkDegradedAlert(link);
            }
            case UP -> {
                link.setAlertSent(false);
                log.info("🟢 LIEN RÉTABLI : {} ({}) - {} succès consécutifs",
                        link.getName(), link.getIpAddress(), link.getConsecutiveSuccesses());
                notificationService.sendLinkUpAlert(link);
            }
            default -> {}
        }
    }

    private Link.LinkStatus computeNewStatus(Link link, Link.LinkStatus previousStatus, boolean success) {
        if (!success) {
            if (link.getConsecutiveFailures() >= alertThreshold) {
                return Link.LinkStatus.DOWN;
            }
            if (link.getConsecutiveFailures() > 0 && previousStatus != Link.LinkStatus.DOWN) {
                // Ne pas rétrograder DOWN en DEGRADED lors d'une récupération partielle
                return Link.LinkStatus.DEGRADED;
            }
            // Lien DOWN avec échecs inférieurs au seuil (oscillation partielle) → reste DOWN
            return previousStatus;
        } else {
            if (link.getConsecutiveSuccesses() >= recoveryCheckCount
                    && (previousStatus == Link.LinkStatus.DOWN || previousStatus == Link.LinkStatus.DEGRADED)) {
                return Link.LinkStatus.UP;
            }
            if (previousStatus == Link.LinkStatus.UP || previousStatus == Link.LinkStatus.UNKNOWN) {
                return Link.LinkStatus.UP;
            }
            // Pas encore assez de succès consécutifs pour confirmer le rétablissement
            return previousStatus;
        }
    }
}
