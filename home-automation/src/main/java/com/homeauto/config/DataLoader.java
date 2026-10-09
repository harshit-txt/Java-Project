package com.homeauto.config;

import com.homeauto.model.AutomationRule;
import com.homeauto.model.Device;
import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.RuleRepository;
import com.homeauto.repository.UserRepository;
import com.homeauto.service.PasswordUtil;
import com.homeauto.service.SettingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Adds demo data the first time the app starts (only when the database is empty)
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final EnvironmentRepository environmentRepository;
    private final SettingService settingService;

    public DataLoader(UserRepository userRepository,
                      DeviceRepository deviceRepository,
                      RuleRepository ruleRepository,
                      EnvironmentRepository environmentRepository,
                      SettingService settingService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.ruleRepository = ruleRepository;
        this.environmentRepository = environmentRepository;
        this.settingService = settingService;
    }

    @Override
    public void run(String... args) {
        settingService.createDefaults();

        if (userRepository.count() > 0) {
            return;
        }

        User admin = new User("Admin", "admin@home.com", PasswordUtil.hash("admin123"), "ADMIN");
        userRepository.save(admin);

        User owner = new User("Demo Homeowner", "user@home.com", PasswordUtil.hash("user123"), "HOMEOWNER");
        owner.setHomeName("Sharma Residence");
        userRepository.save(owner);

        environmentRepository.save(new EnvironmentStatus(owner.getId()));

        Device light = new Device("Living Room Light", "Light", "Living Room", "Wi-Fi", owner);
        light.setStatus("APPROVED");
        light.setPower(true);
        light.setLevel(70);
        deviceRepository.save(light);

        Device fan = new Device("Bedroom Fan", "Fan", "Bedroom", "Wi-Fi", owner);
        fan.setStatus("APPROVED");
        deviceRepository.save(fan);

        Device ac = new Device("Bedroom AC", "AC", "Bedroom", "Wi-Fi", owner);
        ac.setStatus("APPROVED");
        deviceRepository.save(ac);

        Device lock = new Device("Front Door Lock", "Door Lock", "Entrance", "Zigbee", owner);
        lock.setStatus("APPROVED");
        lock.setPower(true);
        deviceRepository.save(lock);

        // Still waiting for the admin, so the admin has something to approve
        Device camera = new Device("Garden Camera", "Camera", "Garden", "Wi-Fi", owner);
        deviceRepository.save(camera);

        AutomationRule rule = new AutomationRule();
        rule.setName("Cool the bedroom");
        rule.setConditionType("TEMP_ABOVE");
        rule.setConditionValue(30);
        rule.setAction("ON");
        rule.setDevice(fan);
        rule.setOwner(owner);
        ruleRepository.save(rule);
    }
}
