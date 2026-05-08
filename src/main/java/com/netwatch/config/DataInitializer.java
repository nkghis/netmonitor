package com.netwatch.config;

import com.netwatch.entity.Link;
import com.netwatch.entity.User;
import com.netwatch.repository.LinkRepository;
import com.netwatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final LinkRepository linkRepository;

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initLinks();
    }

    private void initUsers() {
        if (userService.count() == 0) {
            // Admin par défaut
            User admin = User.builder()
                .username("admin")
                .password("admin")
                //.email("admin@icone-communication.ci")
                .email("ghislain.nkagou@ics.ci")
                .fullName("Administrateur NetWatch")
                .role(User.Role.ADMIN)
                .notifyEmail(true)
                .notifyWhatsapp(false)
                .build();
            userService.create(admin);

            // On crée le user avec le password "Admin@2024!"
            // Changer après la première connexion !
            User adminWithPass = userService.findByUsername("admin").get();
            userService.updatePassword(adminWithPass, "Admin@2024!");


            log.info("✅ Utilisateur admin créé. Login: admin / Password: Admin@2024!");

            log.warn("⚠️  CHANGEZ LE MOT DE PASSE ADMIN APRÈS LA PREMIÈRE CONNEXION !");
        }
    }

    private void initLinks() {
        if (linkRepository.count() == 0) {
            // Les 4 connexions d'Ivoire Cartes Systemes
            String[][] defaultLinks = {
                {"Connexion 1 - Principale", "105.235.100.12", "Lien Internet principal ICS COMMISSARIAT"},
               /* {"Connexion 1 - Principale", "160.154.207.178", "Lien Internet principal Ivoire Cartes Systemes"},
                {"Connexion 2 - Backup", "105.235.6.210", "Lien Internet de secours Ivoire Cartes Systemes"},
                {"Connexion 3 - Secondaire", "41.66.42.46", "Lien Internet secondaire Ivoire Cartes Systemes"},
                {"Connexion 4 - Backup 2", "105.235.6.163", "Lien Internet backup secondaire Ivoire Cartes Systemes"},*/
            };

            for (String[] linkData : defaultLinks) {
                Link link = Link.builder()
                    .name(linkData[0])
                    .ipAddress(linkData[1])
                    .description(linkData[2])
                    .enabled(true)
                    .status(Link.LinkStatus.UNKNOWN)
                    .createdBy("system")
                    .build();
                linkRepository.save(link);
            }

            log.info("✅ {} connexions internet initialisées pour le monitoring", defaultLinks.length);
        }
    }
}
