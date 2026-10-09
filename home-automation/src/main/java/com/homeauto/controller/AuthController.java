package com.homeauto.controller;

import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.UserRepository;
import com.homeauto.service.PasswordUtil;
import com.homeauto.service.SettingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;
    private final SettingService settingService;

    public AuthController(UserRepository userRepository,
                           EnvironmentRepository environmentRepository,
                           SettingService settingService) {
        this.userRepository = userRepository;
        this.environmentRepository = environmentRepository;
        this.settingService = settingService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        Object role = session.getAttribute("role");
        if ("ADMIN".equals(role)) return "redirect:/admin";
        if (role != null) return "redirect:/home";
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        Optional<User> found = userRepository.findByEmail(email.trim().toLowerCase());

        if (found.isEmpty() || !found.get().getPassword().equals(PasswordUtil.hash(password))) {
            model.addAttribute("error", "Wrong email or password.");
            model.addAttribute("email", email);
            return "login";
        }

        User user = found.get();
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("role", user.getRole());

        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationOpen", settingService.isRegistrationAllowed());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        boolean open = settingService.isRegistrationAllowed();
        model.addAttribute("registrationOpen", open);
        model.addAttribute("name", name);
        model.addAttribute("email", email);

        if (!open) {
            model.addAttribute("error", "Registration is closed right now.");
            return "register";
        }

        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase();

        String error = null;
        if (cleanName.isEmpty() || cleanEmail.isEmpty()) {
            error = "Please fill in your name and email.";
        } else if (!cleanEmail.contains("@")) {
            error = "Please enter a valid email address.";
        } else if (password.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (userRepository.findByEmail(cleanEmail).isPresent()) {
            error = "That email is already registered.";
        }

        if (error != null) {
            model.addAttribute("error", error);
            return "register";
        }

        User user = new User(cleanName, cleanEmail, PasswordUtil.hash(password), "HOMEOWNER");
        userRepository.save(user);
        environmentRepository.save(new EnvironmentStatus(user.getId()));

        redirectAttributes.addFlashAttribute("msg", "Account created. You can log in now.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
