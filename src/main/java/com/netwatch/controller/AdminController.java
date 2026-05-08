package com.netwatch.controller;

import com.netwatch.entity.Link;
import com.netwatch.entity.User;
import com.netwatch.service.LinkService;
import com.netwatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final LinkService linkService;

    // ===== USERS =====

    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("username", auth.getName());
        model.addAttribute("currentPage", "users");
        model.addAttribute("newUser", new User());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@ModelAttribute User user,
                              @RequestParam String password,
                              RedirectAttributes redirect) {
        if (userService.usernameExists(user.getUsername())) {
            redirect.addFlashAttribute("error", "Ce nom d'utilisateur existe déjà.");
            return "redirect:/admin/users";
        }
        if (userService.emailExists(user.getEmail())) {
            redirect.addFlashAttribute("error", "Cet email est déjà utilisé.");
            return "redirect:/admin/users";
        }
        user.setPassword(password);
        userService.create(user);
        redirect.addFlashAttribute("success", "Utilisateur '" + user.getUsername() + "' créé avec succès.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirect) {
        userService.findById(id).ifPresent(user -> {
            user.setEnabled(!user.isEnabled());
            userService.update(user);
        });
        redirect.addFlashAttribute("success", "Statut utilisateur mis à jour.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        userService.findById(id).ifPresent(user -> {
            if (user.getUsername().equals(auth.getName())) {
                redirect.addFlashAttribute("error", "Vous ne pouvez pas supprimer votre propre compte.");
                return;
            }
            userService.delete(id);
            redirect.addFlashAttribute("success", "Utilisateur supprimé.");
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirect) {
        userService.findById(id).ifPresent(user -> {
            userService.updatePassword(user, newPassword);
        });
        redirect.addFlashAttribute("success", "Mot de passe réinitialisé avec succès.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/update-notifications")
    public String updateNotifications(@PathVariable Long id,
                                       @RequestParam(required = false) Boolean notifyEmail,
                                       @RequestParam(required = false) Boolean notifyWhatsapp,
                                       @RequestParam(required = false) String whatsappNumber,
                                       @RequestParam(required = false) String whatsappApiKey,
                                       RedirectAttributes redirect) {
        userService.findById(id).ifPresent(user -> {
            user.setNotifyEmail(notifyEmail != null && notifyEmail);
            user.setNotifyWhatsapp(notifyWhatsapp != null && notifyWhatsapp);
            user.setWhatsappNumber(whatsappNumber);
            user.setWhatsappApiKey(whatsappApiKey);
            userService.update(user);
        });
        redirect.addFlashAttribute("success", "Préférences de notification mises à jour.");
        return "redirect:/admin/users";
    }

    // ===== LINKS =====

    @GetMapping("/links")
    public String listLinks(Model model, Authentication auth) {
        model.addAttribute("links", linkService.findAll());
        model.addAttribute("username", auth.getName());
        model.addAttribute("currentPage", "links");
        model.addAttribute("newLink", new Link());
        return "admin/links";
    }

    @PostMapping("/links/create")
    public String createLink(@ModelAttribute Link link, Authentication auth, RedirectAttributes redirect) {
        if (linkService.ipAddressExists(link.getIpAddress())) {
            redirect.addFlashAttribute("error", "Cette adresse IP est déjà enregistrée.");
            return "redirect:/admin/links";
        }
        link.setCreatedBy(auth.getName());
        link.setStatus(Link.LinkStatus.UNKNOWN);
        linkService.save(link);
        redirect.addFlashAttribute("success", "Connexion '" + link.getName() + "' ajoutée avec succès.");
        return "redirect:/admin/links";
    }

    @PostMapping("/links/{id}/toggle")
    public String toggleLink(@PathVariable Long id, RedirectAttributes redirect) {
        linkService.findById(id).ifPresent(link -> {
            link.setEnabled(!link.isEnabled());
            linkService.save(link);
        });
        redirect.addFlashAttribute("success", "Statut de la connexion mis à jour.");
        return "redirect:/admin/links";
    }

    @PostMapping("/links/{id}/delete")
    public String deleteLink(@PathVariable Long id, RedirectAttributes redirect) {
        linkService.deleteById(id);
        redirect.addFlashAttribute("success", "Connexion supprimée.");
        return "redirect:/admin/links";
    }

    @PostMapping("/links/{id}/update")
    public String updateLink(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String description,
                              RedirectAttributes redirect) {
        linkService.findById(id).ifPresent(link -> {
            link.setName(name);
            link.setDescription(description);
            linkService.save(link);
        });
        redirect.addFlashAttribute("success", "Connexion mise à jour.");
        return "redirect:/admin/links";
    }
}
