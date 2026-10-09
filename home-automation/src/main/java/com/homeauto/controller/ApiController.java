package com.homeauto.controller;

import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.UserRepository;
import com.homeauto.service.AutomationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

// Very small API so hardware (for example an ESP32) can talk to the platform over HTTP.
// Demo only: there is no login on these two URLs.
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final EnvironmentRepository environmentRepository;
    private final AutomationService automationService;

    public ApiController(UserRepository userRepository,
                         DeviceRepository deviceRepository,
                         EnvironmentRepository environmentRepository,
                         AutomationService automationService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.environmentRepository = environmentRepository;
        this.automationService = automationService;
    }

    // Example: /api/sensor?email=user@home.com&temperature=31.5&humidity=60
    @GetMapping("/sensor")
    public String sensor(@RequestParam String email,
                         @RequestParam double temperature,
                         @RequestParam double humidity) {

        Optional<User> found = userRepository.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty()) {
            return "ERROR: user not found";
        }
        User user = found.get();

        EnvironmentStatus env = environmentRepository.findById(user.getId())
                .orElse(new EnvironmentStatus(user.getId()));
        env.setTemperature(temperature);
        env.setHumidity(humidity);
        env.setUpdatedAt(LocalDateTime.now());
        environmentRepository.save(env);

        automationService.runRules(user, env);
        return "OK";
    }

    // Example: /api/device/1  -> ON, OFF or NOT_FOUND (the ESP32 can use this to switch an LED)
    @GetMapping("/device/{id}")
    public String deviceState(@PathVariable Long id) {
        return deviceRepository.findById(id)
                .map(d -> (d.isApproved() && d.isPower()) ? "ON" : "OFF")
                .orElse("NOT_FOUND");
    }
}
