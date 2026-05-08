package com.netwatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            Model model) {
        if (error != null) model.addAttribute("error", "Identifiant ou mot de passe incorrect.");
        if (logout != null) model.addAttribute("message", "Vous avez été déconnecté avec succès.");
        if (expired != null) model.addAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
        return "auth/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/profile")
    public String profile() {
        return "dashboard/profile";
    }
}
