package com.homeauto.controller;

import com.homeauto.model.Device;
import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.RuleRepository;
import com.homeauto.repository.UserRepository;
import com.homeauto.service.DeviceService;
import com.homeauto.service.PasswordUtil;
import com.homeauto.service.SettingService;
import com.homeauto.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final EnvironmentRepository environmentRepository;
    private final SettingService settingService;
    private final UserService userService;
    private final DeviceService deviceService;

    public AdminController(UserRepository userRepository,
                           DeviceRepository deviceRepository,
                           RuleRepository ruleRepository,
                           EnvironmentRepository environmentRepository,
                           SettingService settingService,
                           UserService userService,
                           DeviceService deviceService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.ruleRepository = ruleRepository;
        this.environmentRepository = environmentRepository;
        this.settingService = settingService;
        this.userService = userService;
        this.deviceService = deviceService;
    }

    // ---------------- Overview + System Monitoring ----------------

    @GetMapping({"", "/"})
    public String overview(Model model) {
        long pending = deviceRepository.countByStatus("PENDING");

        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("homeownerCount", userRepository.countByRole("HOMEOWNER"));
        model.addAttribute("deviceCount", deviceRepository.count());
        model.addAttribute("pendingCount", pending);
        model.addAttribute("ruleCount", ruleRepository.count());

        long uptimeMinutes = ManagementFactory.getRuntimeMXBean().getUptime() / 60000;
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        model.addAttribute("uptimeMinutes", uptimeMinutes);
        model.addAttribute("usedMb", usedMb);
        model.addAttribute("maxMb", maxMb);

        List<String> alerts = new ArrayList<>();
        if (pending > 0) {
            alerts.add(pending + " device(s) waiting for approval.");
        }
        for (EnvironmentStatus env : environmentRepository.findAll()) {
            Optional<User> owner = userRepository.findById(env.getUserId());
            if (owner.isEmpty()) continue;
            String who = owner.get().getHomeName() + " (" + owner.get().getName() + ")";
            if ("Alert".equals(env.getSecurityStatus())) {
                alerts.add("Security alert at " + who + ".");
            }
            if (env.getTemperature() > 35) {
                alerts.add("High temperature at " + who + ".");
            }
        }
        model.addAttribute("alerts", alerts);

        return "admin/overview";
    }

    // ---------------- User Management ----------------

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        User form = new User();
        form.setRole("HOMEOWNER");
        model.addAttribute("formUser", form);
        return "admin/user-form";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        model.addAttribute("formUser", found.get());
        return "admin/user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String role,
                           @RequestParam(defaultValue = "") String password,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase();

        // this copy is only used to refill the form if something is wrong
        User form = new User();
        form.setId(id);
        form.setName(cleanName);
        form.setEmail(cleanEmail);
        form.setRole(role);

        String error = null;
        if (cleanName.isEmpty() || cleanEmail.isEmpty()) {
            error = "Name and email are required.";
        } else if (!cleanEmail.contains("@")) {
            error = "Please enter a valid email address.";
        } else if (!"ADMIN".equals(role) && !"HOMEOWNER".equals(role)) {
            error = "Please choose a role.";
        } else if (id == null && password.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (!password.isEmpty() && password.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (id != null && id.equals(session.getAttribute("userId")) && !"ADMIN".equals(role)) {
            error = "You cannot remove your own admin role.";
        } else {
            Optional<User> sameEmail = userRepository.findByEmail(cleanEmail);
            if (sameEmail.isPresent() && !sameEmail.get().getId().equals(id)) {
                error = "That email is already used by another account.";
            }
        }

        if (error != null) {
            model.addAttribute("formUser", form);
            model.addAttribute("error", error);
            return "admin/user-form";
        }

        User user;
        if (id == null) {
            user = new User();
        } else {
            user = userRepository.findById(id).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found.");
                return "redirect:/admin/users";
            }
        }

        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setRole(role);
        if (!password.isEmpty()) {
            user.setPassword(PasswordUtil.hash(password));
        }
        userRepository.save(user);

        if (id == null) {
            environmentRepository.save(new EnvironmentStatus(user.getId()));
            redirectAttributes.addFlashAttribute("msg", "User " + cleanName + " created successfully.");
        } else {
            redirectAttributes.addFlashAttribute("msg", "User " + cleanName + " updated successfully.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (id.equals(session.getAttribute("userId"))) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete your own account.");
            return "redirect:/admin/users";
        }
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        } else {
            String name = found.get().getName();
            userService.deleteUser(found.get());
            redirectAttributes.addFlashAttribute("msg", "User " + name + " deleted successfully.");
        }
        return "redirect:/admin/users";
    }

    // ---------------- Device Compatibility Management ----------------

    @GetMapping("/devices")
    public String devices(Model model) {
        model.addAttribute("devices", deviceRepository.findAll());
        return "admin/devices";
    }

    @PostMapping("/devices/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return changeStatus(id, "APPROVED", redirectAttributes);
    }

    @PostMapping("/devices/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return changeStatus(id, "REJECTED", redirectAttributes);
    }

    private String changeStatus(Long id, String status, RedirectAttributes redirectAttributes) {
        Optional<Device> found = deviceRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Device not found.");
        } else {
            Device device = found.get();
            device.setStatus(status);
            if ("REJECTED".equals(status)) {
                device.setPower(false);
            }
            deviceRepository.save(device);
            String word = "APPROVED".equals(status) ? "approved" : "rejected";
            redirectAttributes.addFlashAttribute("msg", device.getName() + " was " + word + ".");
        }
        return "redirect:/admin/devices";
    }

    @PostMapping("/devices/{id}/delete")
    public String deleteDevice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Device> found = deviceRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Device not found.");
        } else {
            String name = found.get().getName();
            deviceService.deleteDevice(found.get());
            redirectAttributes.addFlashAttribute("msg", "Device " + name + " deleted.");
        }
        return "redirect:/admin/devices";
    }

    // ---------------- System Settings ----------------

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("siteNameValue", settingService.getSiteName());
        model.addAttribute("maxDevicesValue", settingService.getMaxDevices());
        model.addAttribute("registrationValue", settingService.isRegistrationAllowed());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String siteName,
                               @RequestParam int maxDevices,
                               @RequestParam(required = false) String allowRegistration,
                               RedirectAttributes redirectAttributes) {

        String cleanName = siteName.trim();
        if (cleanName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Site name cannot be empty.");
            return "redirect:/admin/settings";
        }
        if (maxDevices < 1 || maxDevices > 100) {
            redirectAttributes.addFlashAttribute("error", "Max devices must be between 1 and 100.");
            return "redirect:/admin/settings";
        }

        settingService.set(SettingService.SITE_NAME, cleanName);
        settingService.set(SettingService.MAX_DEVICES, String.valueOf(maxDevices));
        settingService.set(SettingService.ALLOW_REGISTRATION, allowRegistration != null ? "true" : "false");

        redirectAttributes.addFlashAttribute("msg", "Settings updated successfully.");
        return "redirect:/admin/settings";
    }
}
