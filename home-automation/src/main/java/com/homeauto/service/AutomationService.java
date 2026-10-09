package com.homeauto.service;

import com.homeauto.model.AutomationRule;
import com.homeauto.model.Device;
import com.homeauto.model.EnvironmentStatus;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AutomationService {

    private final RuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;

    public AutomationService(RuleRepository ruleRepository, DeviceRepository deviceRepository) {
        this.ruleRepository = ruleRepository;
        this.deviceRepository = deviceRepository;
    }

    // Checks every enabled rule of this user against the new readings.
    // Returns a message for each rule that changed a device.
    public List<String> runRules(User owner, EnvironmentStatus env) {
        List<String> messages = new ArrayList<>();

        for (AutomationRule rule : ruleRepository.findByOwnerOrderByIdAsc(owner)) {
            if (!rule.isEnabled()) continue;

            Device device = rule.getDevice();
            if (device == null || !device.isApproved()) continue;

            boolean conditionMet = false;
            if ("TEMP_ABOVE".equals(rule.getConditionType())) {
                conditionMet = env.getTemperature() > rule.getConditionValue();
            } else if ("TEMP_BELOW".equals(rule.getConditionType())) {
                conditionMet = env.getTemperature() < rule.getConditionValue();
            } else if ("HUMIDITY_ABOVE".equals(rule.getConditionType())) {
                conditionMet = env.getHumidity() > rule.getConditionValue();
            }

            if (conditionMet) {
                boolean turnOn = "ON".equals(rule.getAction());
                if (device.isPower() != turnOn) {
                    device.setPower(turnOn);
                    deviceRepository.save(device);
                    messages.add("Rule \"" + rule.getName() + "\" turned "
                            + device.getName() + (turnOn ? " ON" : " OFF") + ".");
                }
            }
        }
        return messages;
    }
}
