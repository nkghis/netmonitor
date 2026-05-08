package com.netwatch.service;

import com.netwatch.entity.Link;
import com.netwatch.entity.PingResult;
import com.netwatch.repository.AlertLogRepository;
import com.netwatch.repository.LinkRepository;
import com.netwatch.repository.PingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LinkService {

    private final LinkRepository linkRepository;
    private final PingResultRepository pingResultRepository;
    private final AlertLogRepository alertLogRepository;

    public List<Link> findAll() {
        return linkRepository.findAll();
    }

    public List<Link> findAllEnabled() {
        return linkRepository.findByEnabledTrue();
    }

    public Optional<Link> findById(Long id) {
        return linkRepository.findById(id);
    }

    public Link save(Link link) {
        return linkRepository.save(link);
    }

    public void deleteById(Long id) {

        // Supprimer d'abord les enregistrements liés (contraintes FK MySQL)
        alertLogRepository.deleteByLinkId(id);
        pingResultRepository.deleteByLinkId(id);
        linkRepository.deleteById(id);
    }

    public boolean ipAddressExists(String ip) {
        return linkRepository.existsByIpAddress(ip);
    }

    public List<PingResult> getRecentPings(Long linkId, int count) {
        return pingResultRepository.findByLinkIdOrderByPingedAtDesc(
            linkId, PageRequest.of(0, count));
    }

    public List<PingResult> getPingsSince(Long linkId, LocalDateTime since) {
        return pingResultRepository.findByLinkIdSince(linkId, since);
    }

    public DashboardStats getDashboardStats() {
        List<Link> allLinks = linkRepository.findAll();
        long total = allLinks.size();
        long up = allLinks.stream().filter(l -> l.getStatus() == Link.LinkStatus.UP).count();
        long down = allLinks.stream().filter(l -> l.getStatus() == Link.LinkStatus.DOWN).count();
        long degraded = allLinks.stream().filter(l -> l.getStatus() == Link.LinkStatus.DEGRADED).count();
        long unknown = allLinks.stream().filter(l -> l.getStatus() == Link.LinkStatus.UNKNOWN).count();
        Double avgUptime = linkRepository.findAverageUptimePercentage();

        return new DashboardStats(total, up, down, degraded, unknown,
            avgUptime != null ? avgUptime : 0.0,
            alertLogRepository.count());
    }

    public record DashboardStats(long total, long up, long down, long degraded,
                                  long unknown, double avgUptime, long totalAlerts) {}
}
