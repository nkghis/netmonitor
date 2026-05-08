package com.netwatch.controller;

import com.netwatch.entity.AlertLog;
import com.netwatch.entity.Link;
import com.netwatch.entity.PingResult;
import com.netwatch.repository.AlertLogRepository;
import com.netwatch.repository.PingResultRepository;
import com.netwatch.service.LinkService;
import com.netwatch.service.PingService;
import com.netwatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final LinkService linkService;
    private final PingService pingService;
    private final UserService userService;
    private final AlertLogRepository alertLogRepository;
    private final PingResultRepository pingResultRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        userService.updateLastLogin(auth.getName());

        LinkService.DashboardStats stats = linkService.getDashboardStats();
        List<Link> links = linkService.findAll();
        List<AlertLog> recentAlerts = alertLogRepository.findByOrderBySentAtDesc(PageRequest.of(0, 10));

        model.addAttribute("stats", stats);
        model.addAttribute("links", links);
        model.addAttribute("recentAlerts", recentAlerts);
        model.addAttribute("username", auth.getName());
        model.addAttribute("currentPage", "dashboard");
        return "dashboard/index";
    }

    @GetMapping("/links/{id}")
    public String linkDetail(@PathVariable Long id, Model model, Authentication auth) {
        return linkService.findById(id).map(link -> {
            List<PingResult> recentPings = linkService.getRecentPings(id, 50);
            List<AlertLog> alerts = alertLogRepository.findByLinkIdOrderBySentAtDesc(id, PageRequest.of(0, 20));

            model.addAttribute("link", link);
            model.addAttribute("recentPings", recentPings);
            model.addAttribute("alerts", alerts);
            model.addAttribute("username", auth.getName());
            model.addAttribute("currentPage", "links");
            return "dashboard/link-detail";
        }).orElse("redirect:/dashboard");
    }

    // API JSON pour refresh temps réel
    @GetMapping("/api/links/status")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getLinksStatus() {
        List<Link> links = linkService.findAll();
        List<Map<String, Object>> result = links.stream().map(link -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", link.getId());
            m.put("name", link.getName());
            m.put("ip", link.getIpAddress());
            m.put("status", link.getStatus().name());
            m.put("responseTimeMs", link.getResponseTimeMs());
            m.put("uptime", String.format("%.1f", link.getUptimePercentage()));
            m.put("consecutiveFailures", link.getConsecutiveFailures());
            m.put("lastPingAt", link.getLastPingAt() != null ? link.getLastPingAt().toString() : null);
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        LinkService.DashboardStats stats = linkService.getDashboardStats();
        Map<String, Object> m = new HashMap<>();
        m.put("total", stats.total());
        m.put("up", stats.up());
        m.put("down", stats.down());
        m.put("degraded", stats.degraded());
        m.put("avgUptime", String.format("%.1f", stats.avgUptime()));
        m.put("totalAlerts", stats.totalAlerts());
        return ResponseEntity.ok(m);
    }

    // Ping manuel
    @PostMapping("/api/links/{id}/ping")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> manualPing(@PathVariable Long id) {
        return linkService.findById(id).map(link -> {
            PingResult result = pingService.pingLink(link);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("responseTimeMs", result.getResponseTimeMs());
            response.put("status", link.getStatus().name());
            response.put("message", result.isSuccess() ?
                "Lien disponible (" + result.getResponseTimeMs() + "ms)" :
                "Lien indisponible : " + result.getErrorMessage());
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/links/{id}/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getLinkHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<PingResult> pings = linkService.getPingsSince(id, since);

        List<Map<String, Object>> result = pings.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("time", p.getPingedAt().toString());
            m.put("success", p.isSuccess());
            m.put("responseTimeMs", p.getResponseTimeMs());
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
