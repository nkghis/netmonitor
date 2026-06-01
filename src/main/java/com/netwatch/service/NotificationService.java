package com.netwatch.service;

import com.netwatch.entity.AlertLog;
import com.netwatch.entity.Link;
import com.netwatch.entity.User;
import com.netwatch.repository.AlertLogRepository;
import com.netwatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final AlertLogRepository alertLogRepository;

    @Value("${netwatch.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${netwatch.whatsapp.callmebot-api-url}")
    private String callmebotApiUrl;

    @Value("${netwatch.notifications.from-name}")
    private String fromName;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Async
    public void sendLinkDownAlert(Link link) {
        String subject = "🔴 ALERTE - Lien " + link.getName() + " INDISPONIBLE";
        String htmlBody = buildDownAlertHtml(link);
        String textBody = buildDownAlertText(link);

        sendToAllRecipients(link, subject, htmlBody, textBody, AlertLog.AlertType.LINK_DOWN);
    }

    @Async
    public void sendLinkDegradedAlert(Link link) {
        String subject = "🟡 ALERTE - Lien " + link.getName() + " DÉGRADÉ";
        String htmlBody = buildDegradedAlertHtml(link);
        String textBody = buildDegradedAlertText(link);

        sendToAllRecipients(link, subject, htmlBody, textBody, AlertLog.AlertType.DEGRADED);
    }

    @Async
    public void sendLinkUpAlert(Link link) {
        String subject = "🟢 RÉTABLI - Lien " + link.getName() + " est de nouveau disponible";
        String htmlBody = buildUpAlertHtml(link);
        String textBody = buildUpAlertText(link);

        sendToAllRecipients(link, subject, htmlBody, textBody, AlertLog.AlertType.LINK_UP);
    }

    private void sendToAllRecipients(Link link, String subject, String htmlBody,
                                      String textBody, AlertLog.AlertType type) {
        List<User> emailUsers = userRepository.findByNotifyEmailTrueAndEnabledTrue();
        for (User user : emailUsers) {
            sendEmail(link, user.getEmail(), subject, htmlBody, type);
        }

        if (whatsappEnabled) {
            List<User> whatsappUsers = userRepository.findByNotifyWhatsappTrueAndEnabledTrue();
            for (User user : whatsappUsers) {
                if (user.getWhatsappNumber() != null && user.getWhatsappApiKey() != null) {
                    sendWhatsApp(link, user, textBody, type);
                }
            }
        }
    }

    private void sendEmail(Link link, String recipient, String subject, String htmlBody, AlertLog.AlertType type) {
        AlertLog alertLog = AlertLog.builder()
            .link(link)
            .type(type)
            .recipientEmail(recipient)
            .channel("EMAIL")
            .messageContent(subject)
            .build();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            alertLog.setSuccess(true);
            log.info("Email envoyé à {} pour le lien {}", recipient, link.getName());
        } catch (Exception e) {
            alertLog.setSuccess(false);
            alertLog.setErrorMessage(e.getMessage());
            log.error("Erreur envoi email à {} : {}", recipient, e.getMessage());
        } finally {
            alertLogRepository.save(alertLog);
        }
    }

    private void sendWhatsApp(Link link, User user, String message, AlertLog.AlertType type) {
        AlertLog alertLog = AlertLog.builder()
            .link(link)
            .type(type)
            .recipientPhone(user.getWhatsappNumber())
            .channel("WHATSAPP")
            .messageContent(message)
            .build();

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(callmebotApiUrl)
                .queryParam("phone", user.getWhatsappNumber())
                .queryParam("text", message)
                .queryParam("apikey", user.getWhatsappApiKey())
                .build().toUriString();

            restTemplate.getForObject(url, String.class);
            alertLog.setSuccess(true);
            log.info("WhatsApp envoyé à {} pour le lien {}", user.getWhatsappNumber(), link.getName());
        } catch (Exception e) {
            alertLog.setSuccess(false);
            alertLog.setErrorMessage(e.getMessage());
            log.error("Erreur envoi WhatsApp à {} : {}", user.getWhatsappNumber(), e.getMessage());
        } finally {
            alertLogRepository.save(alertLog);
        }
    }

    // ===== Templates HTML Email =====

    private String buildDegradedAlertHtml(Link link) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: #fd7e14; padding: 20px; text-align: center;">
                  <h1 style="color: #fff; margin: 0; font-size: 24px;">🟡 LIEN DÉGRADÉ</h1>
                </div>
                <div style="padding: 30px;">
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Connexion</td>
                        <td style="padding: 8px; color: #333;">""" + link.getName() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Adresse IP</td>
                        <td style="padding: 8px; color: #333; font-family: monospace;">""" + link.getIpAddress() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Détecté le</td>
                        <td style="padding: 8px; color: #333;">""" + (link.getLastFailureAt() != null ? link.getLastFailureAt().format(FORMATTER) : "N/A") + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Échecs consécutifs</td>
                        <td style="padding: 8px; color: #fd7e14; font-weight: bold;">""" + link.getConsecutiveFailures() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Disponibilité</td>
                        <td style="padding: 8px; color: #333;">""" + String.format("%.1f%%", link.getUptimePercentage()) + """
                    </td></tr>
                  </table>
                  <div style="background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 15px; margin-top: 20px;">
                    <strong>⚠️ Avertissement :</strong> Des pertes de paquets ont été détectées. Surveillez ce lien, une indisponibilité totale est possible.
                  </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center; color: #666; font-size: 12px;">
                  NetWatch Monitor | Ivoire Cartes Systemes | Abidjan, Côte d'Ivoire
                </div>
              </div>
            </body>
            </html>
            """;
    }

    private String buildDegradedAlertText(Link link) {
        return String.format(
            "🟡 ALERTE NETWATCH\n\nLien DÉGRADÉ !\n\nConnexion : %s\nIP : %s\nDétecté le : %s\nÉchecs : %d\nDisponibilité : %.1f%%\n\nSurveillance recommandée.\n\n-- NetWatch | Ivoire Cartes Systemes",
            link.getName(), link.getIpAddress(),
            link.getLastFailureAt() != null ? link.getLastFailureAt().format(FORMATTER) : "N/A",
            link.getConsecutiveFailures(), link.getUptimePercentage()
        );
    }

    private String buildDownAlertHtml(Link link) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: #dc3545; padding: 20px; text-align: center;">
                  <h1 style="color: #fff; margin: 0; font-size: 24px;">🔴 LIEN INDISPONIBLE</h1>
                </div>
                <div style="padding: 30px;">
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Connexion</td>
                        <td style="padding: 8px; color: #333;">""" + link.getName() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Adresse IP</td>
                        <td style="padding: 8px; color: #333; font-family: monospace;">""" + link.getIpAddress() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Détecté le</td>
                        <td style="padding: 8px; color: #333;">""" + (link.getLastFailureAt() != null ? link.getLastFailureAt().format(FORMATTER) : "N/A") + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Échecs consécutifs</td>
                        <td style="padding: 8px; color: #dc3545; font-weight: bold;">""" + link.getConsecutiveFailures() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Disponibilité</td>
                        <td style="padding: 8px; color: #333;">""" + String.format("%.1f%%", link.getUptimePercentage()) + """
                    </td></tr>
                  </table>
                  <div style="background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 15px; margin-top: 20px;">
                    <strong>⚠️ Action requise :</strong> Vérifiez immédiatement votre connexion internet et l'équipement réseau associé.
                  </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center; color: #666; font-size: 12px;">
                  NetWatch Monitor | Ivoire Cartes Systemes | Abidjan, Côte d'Ivoire
                </div>
              </div>
            </body>
            </html>
            """;
    }

    private String buildUpAlertHtml(Link link) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px;">
              <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <div style="background: #198754; padding: 20px; text-align: center;">
                  <h1 style="color: #fff; margin: 0; font-size: 24px;">🟢 LIEN RÉTABLI</h1>
                </div>
                <div style="padding: 30px;">
                  <table style="width: 100%; border-collapse: collapse;">
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Connexion</td>
                        <td style="padding: 8px; color: #333;">""" + link.getName() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Adresse IP</td>
                        <td style="padding: 8px; color: #333; font-family: monospace;">""" + link.getIpAddress() + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Rétabli le</td>
                        <td style="padding: 8px; color: #198754; font-weight: bold;">""" + (link.getLastSuccessAt() != null ? link.getLastSuccessAt().format(FORMATTER) : "N/A") + """
                    </td></tr>
                    <tr><td style="padding: 8px; font-weight: bold; color: #666;">Disponibilité</td>
                        <td style="padding: 8px; color: #333;">""" + String.format("%.1f%%", link.getUptimePercentage()) + """
                    </td></tr>
                  </table>
                  <div style="background: #d1e7dd; border: 1px solid #198754; border-radius: 4px; padding: 15px; margin-top: 20px;">
                    <strong>✅ Information :</strong> La connexion est de nouveau opérationnelle et les services ont repris normalement.
                  </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center; color: #666; font-size: 12px;">
                  NetWatch Monitor | Ivoire Cartes Systemes | Abidjan, Côte d'Ivoire
                </div>
              </div>
            </body>
            </html>
            """;
    }

    private String buildDownAlertText(Link link) {
        return String.format(
            "🔴 ALERTE NETWATCH\n\nLien INDISPONIBLE !\n\nConnexion : %s\nIP : %s\nDétecté le : %s\nÉchecs : %d\nDisponibilité : %.1f%%\n\nVérifiez votre réseau immédiatement.\n\n-- NetWatch | Ivoire Cartes Systemes",
            link.getName(), link.getIpAddress(),
            link.getLastFailureAt() != null ? link.getLastFailureAt().format(FORMATTER) : "N/A",
            link.getConsecutiveFailures(), link.getUptimePercentage()
        );
    }

    private String buildUpAlertText(Link link) {
        return String.format(
            "🟢 NETWATCH - RÉTABLI\n\nLien de nouveau disponible !\n\nConnexion : %s\nIP : %s\nRétabli le : %s\nDisponibilité : %.1f%%\n\n-- NetWatch | Ivoire Cartes Systemes",
            link.getName(), link.getIpAddress(),
            link.getLastSuccessAt() != null ? link.getLastSuccessAt().format(FORMATTER) : "N/A",
            link.getUptimePercentage()
        );
    }
}
