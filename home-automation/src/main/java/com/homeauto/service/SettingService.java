package com.homeauto.service;

import com.homeauto.model.SystemSetting;
import com.homeauto.repository.SettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

    public static final String SITE_NAME = "siteName";
    public static final String MAX_DEVICES = "maxDevicesPerUser";
    public static final String ALLOW_REGISTRATION = "allowRegistration";

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String get(String key, String defaultValue) {
        return settingRepository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public void set(String key, String value) {
        settingRepository.save(new SystemSetting(key, value));
    }

    // Called once at startup: only fills in settings that do not exist yet
    public void createDefaults() {
        if (!settingRepository.existsById(SITE_NAME)) set(SITE_NAME, "Smart Home");
        if (!settingRepository.existsById(MAX_DEVICES)) set(MAX_DEVICES, "10");
        if (!settingRepository.existsById(ALLOW_REGISTRATION)) set(ALLOW_REGISTRATION, "true");
    }

    public String getSiteName() {
        return get(SITE_NAME, "Smart Home");
    }

    public int getMaxDevices() {
        try {
            return Integer.parseInt(get(MAX_DEVICES, "10"));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public boolean isRegistrationAllowed() {
        return Boolean.parseBoolean(get(ALLOW_REGISTRATION, "true"));
    }
}
