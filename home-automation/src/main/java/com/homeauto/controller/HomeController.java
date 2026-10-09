package com.homeauto.controller;

import com.homeauto.model.AutomationRule;
import com.homeauto.model.Device;
import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.RuleRepository;
import com.homeauto.repository.UserRepository;
import com.homeauto.service.AutomationService;
import com.homeauto.service.DeviceService;
import com.homeauto.service.PasswordUtil;
import com.homeauto.service.SettingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping("/home")
public class HomeController {

    private static final List<String> CONDITION_TYPES = List.of("TEMP_ABOVE", "TEMP_BELOW", "HUMIDITY_ABOVE");

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final EnvironmentRepository environmentRepository;
    private final SettingService settingService;
    private final AutomationService automationService;
    private final DeviceService deviceService;

    public HomeController(UserRepository userRepository,
                          DeviceRepository deviceRepository,
                          RuleRepository ruleRepository,
                          EnvironmentRepository environmentRepository,
                          SettingService settingService,
                          AutomationService automationService,
                          DeviceService deviceService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.ruleRepository = ruleRepository;
        this.environmentRepository = environmentRepository;
        this.settingService = settingService;
        this.automationService = automationService;
        this.deviceService = deviceService;
    }

    // ---------------- helpers ----------------

    private User currentUser(HttpSession session) {
        return userRepository.findById((Long) session.getAttribute("userId")).orElseThrow();
    }

    private EnvironmentStatus getEnvironment(User user) {
        return environmentRepository.findById(user.getId())
                .orElseGet(() -> environmentRepository.save(new EnvironmentStatus(user.getId())));
    }

    // returns the device only if it belongs to this user
    private Device ownedDevice(Long id, User user) {
        return deviceRepository.findById(id)
                .filter(d -> d.getOwner() != null && d.getOwner().getId().equals(user.getId()))
                .orElse(null);
    }

    // ---------------- Monitor Home Environment ----------------

    @GetMapping({"", "/"})
    public String environment(HttpSession session, Model model) {
        User user = currentUser(session);
        List<Device> devices = deviceRepository.findByOwnerOrderByIdAsc(user);
        long onCount = devices.stream().filter(d -> d.isApproved() && d.isPower()).count();

        model.addAttribute("user", user);
        model.addAttribute("env", getEnvironment(user));
        model.addAttribute("devices", devices);
        model.addAttribute("onCount", onCount);
        return "home/environment";
    }

    @PostMapping("/environment")
    public String updateEnvironment(@RequestParam double temperature,
                                    @RequestParam double humidity,
                                    @RequestParam String securityStatus,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        if (temperature < -20 || temperature > 60) {
            redirectAttributes.addFlashAttribute("error", "Temperature must be between -20 and 60.");
            return "redirect:/home";
        }
        if (humidity < 0 || humidity > 100) {
            redirectAttributes.addFlashAttribute("error", "Humidity must be between 0 and 100.");
            return "redirect:/home";
        }
        if (!"Secure".equals(securityStatus) && !"Alert".equals(securityStatus)) {
            redirectAttributes.addFlashAttribute("error", "Security status must be Secure or Alert.");
            return "redirect:/home";
        }

        saveReadings(currentUser(session), temperature, humidity, securityStatus, redirectAttributes);
        return "redirect:/home";
    }

    // Pretends a sensor sent new values (handy for demos without hardware)
    @PostMapping("/environment/simulate")
    public String simulate(HttpSession session, RedirectAttributes redirectAttributes) {
        Random random = new Random();
        double temperature = Math.round((15 + random.nextDouble() * 25) * 10) / 10.0;
        double humidity = Math.round((30 + random.nextDouble() * 50) * 10) / 10.0;
        String security = (random.nextInt(10) == 0) ? "Alert" : "Secure";

        saveReadings(currentUser(session), temperature, humidity, security, redirectAttributes);
        return "redirect:/home";
    }

    private void saveReadings(User user, double temperature, double humidity, String security,
                              RedirectAttributes redirectAttributes) {
        EnvironmentStatus env = getEnvironment(user);
        env.setTemperature(temperature);
        env.setHumidity(humidity);
        env.setSecurityStatus(security);
        env.setUpdatedAt(LocalDateTime.now());
        environmentRepository.save(env);

        List<String> ruleMessages = automationService.runRules(user, env);
        redirectAttributes.addFlashAttribute("msg", "Environment updated successfully.");
        redirectAttributes.addFlashAttribute("ruleMsgs", ruleMessages);
    }

    // ---------------- Device Control ----------------

    @GetMapping("/devices")
    public String devices(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("devices", deviceRepository.findByOwnerOrderByIdAsc(user));
        model.addAttribute("types", Device.TYPES);
        model.addAttribute("protocols", Device.PROTOCOLS);
        model.addAttribute("maxDevices", settingService.getMaxDevices());
        return "home/devices";
    }

    @PostMapping("/devices/add")
    public String addDevice(@RequestParam String name,
                            @RequestParam String type,
                            @RequestParam String room,
                            @RequestParam String protocol,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        User user = currentUser(session);
        String cleanName = name.trim();
        String cleanRoom = room.trim();

        if (cleanName.isEmpty() || cleanRoom.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Device name and room are required.");
        } else if (!Device.TYPES.contains(type) || !Device.PROTOCOLS.contains(protocol)) {
            redirectAttributes.addFlashAttribute("error", "Please choose a valid type and protocol.");
        } else if (deviceRepository.countByOwner(user) >= settingService.getMaxDevices()) {
            redirectAttributes.addFlashAttribute("error",
                    "You can add at most " + settingService.getMaxDevices() + " devices.");
        } else {
            deviceRepository.save(new Device(cleanName, type, cleanRoom, protocol, user));
            redirectAttributes.addFlashAttribute("msg",
                    cleanName + " was added. It can be used once the admin approves it.");
        }
        return "redirect:/home/devices";
    }

    @PostMapping("/devices/{id}/toggle")
    public String toggleDevice(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Device device = ownedDevice(id, currentUser(session));
        if (device == null) {
            redirectAttributes.addFlashAttribute("error", "Device not found.");
        } else if (!device.isApproved()) {
            redirectAttributes.addFlashAttribute("error", "This device is not approved yet.");
        } else {
            device.setPower(!device.isPower());
            deviceRepository.save(device);
            redirectAttributes.addFlashAttribute("msg",
                    device.getName() + " is now " + device.getStateText().toLowerCase() + ".");
        }
        return "redirect:/home/devices";
    }

    @PostMapping("/devices/{id}/level")
    public String setLevel(@PathVariable Long id,
                           @RequestParam int level,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Device device = ownedDevice(id, currentUser(session));
        if (device == null) {
            redirectAttributes.addFlashAttribute("error", "Device not found.");
        } else if (!device.isApproved() || !device.isAdjustable()) {
            redirectAttributes.addFlashAttribute("error", "This device cannot be adjusted.");
        } else {
            int safeLevel = Math.max(device.getMin(), Math.min(device.getMax(), level));
            device.setLevel(safeLevel);
            deviceRepository.save(device);
            redirectAttributes.addFlashAttribute("msg",
                    device.getName() + " set to " + safeLevel + ".");
        }
        return "redirect:/home/devices";
    }

    @PostMapping("/devices/{id}/delete")
    public String deleteDevice(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Device device = ownedDevice(id, currentUser(session));
        if (device == null) {
            redirectAttributes.addFlashAttribute("error", "Device not found.");
        } else {
            String name = device.getName();
            deviceService.deleteDevice(device);
            redirectAttributes.addFlashAttribute("msg", name + " was removed.");
        }
        return "redirect:/home/devices";
    }

    // ---------------- Set Automation Rules ----------------

    @GetMapping("/rules")
    public String rules(HttpSession session, Model model) {
        User user = currentUser(session);
        List<Device> approved = new ArrayList<>();
        for (Device d : deviceRepository.findByOwnerOrderByIdAsc(user)) {
            if (d.isApproved()) approved.add(d);
        }
        model.addAttribute("rules", ruleRepository.findByOwnerOrderByIdAsc(user));
        model.addAttribute("devices", approved);
        return "home/rules";
    }

    @PostMapping("/rules/add")
    public String addRule(@RequestParam String name,
                          @RequestParam String conditionType,
                          @RequestParam double conditionValue,
                          @RequestParam Long deviceId,
                          @RequestParam String action,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        User user = currentUser(session);
        Device device = ownedDevice(deviceId, user);
        String cleanName = name.trim();

        if (cleanName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please give the rule a name.");
        } else if (!CONDITION_TYPES.contains(conditionType)) {
            redirectAttributes.addFlashAttribute("error", "Please choose a valid condition.");
        } else if (!"ON".equals(action) && !"OFF".equals(action)) {
            redirectAttributes.addFlashAttribute("error", "Action must be ON or OFF.");
        } else if (device == null || !device.isApproved()) {
            redirectAttributes.addFlashAttribute("error", "Please choose one of your approved devices.");
        } else {
            AutomationRule rule = new AutomationRule();
            rule.setName(cleanName);
            rule.setConditionType(conditionType);
            rule.setConditionValue(conditionValue);
            rule.setAction(action);
            rule.setDevice(device);
            rule.setOwner(user);
            ruleRepository.save(rule);
            redirectAttributes.addFlashAttribute("msg", "Rule \"" + cleanName + "\" created successfully.");
        }
        return "redirect:/home/rules";
    }

    @PostMapping("/rules/{id}/toggle")
    public String toggleRule(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AutomationRule rule = ownedRule(id, currentUser(session));
        if (rule == null) {
            redirectAttributes.addFlashAttribute("error", "Rule not found.");
        } else {
            rule.setEnabled(!rule.isEnabled());
            ruleRepository.save(rule);
            redirectAttributes.addFlashAttribute("msg",
                    "Rule \"" + rule.getName() + "\" is now " + (rule.isEnabled() ? "enabled" : "disabled") + ".");
        }
        return "redirect:/home/rules";
    }

    @PostMapping("/rules/{id}/delete")
    public String deleteRule(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AutomationRule rule = ownedRule(id, currentUser(session));
        if (rule == null) {
            redirectAttributes.addFlashAttribute("error", "Rule not found.");
        } else {
            ruleRepository.delete(rule);
            redirectAttributes.addFlashAttribute("msg", "Rule \"" + rule.getName() + "\" deleted.");
        }
        return "redirect:/home/rules";
    }

    private AutomationRule ownedRule(Long id, User user) {
        return ruleRepository.findById(id)
                .filter(r -> r.getOwner() != null && r.getOwner().getId().equals(user.getId()))
                .orElse(null);
    }

    // ---------------- Profile Management ----------------

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        model.addAttribute("user", currentUser(session));
        return "home/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam String name,
                              @RequestParam String email,
                              @RequestParam String homeName,
                              @RequestParam(defaultValue = "") String newPassword,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User user = currentUser(session);
        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase();
        String cleanHome = homeName.trim();

        String error = null;
        if (cleanName.isEmpty() || cleanEmail.isEmpty() || cleanHome.isEmpty()) {
            error = "Name, email and home name are required.";
        } else if (!cleanEmail.contains("@")) {
            error = "Please enter a valid email address.";
        } else if (!newPassword.isEmpty() && newPassword.length() < 6) {
            error = "New password must be at least 6 characters.";
        } else {
            Optional<User> sameEmail = userRepository.findByEmail(cleanEmail);
            if (sameEmail.isPresent() && !sameEmail.get().getId().equals(user.getId())) {
                error = "That email is already used by another account.";
            }
        }

        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/home/profile";
        }

        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setHomeName(cleanHome);
        if (!newPassword.isEmpty()) {
            user.setPassword(PasswordUtil.hash(newPassword));
        }
        userRepository.save(user);
        session.setAttribute("userName", cleanName);

        redirectAttributes.addFlashAttribute("msg", "Profile updated successfully.");
        return "redirect:/home/profile";
    }
}
