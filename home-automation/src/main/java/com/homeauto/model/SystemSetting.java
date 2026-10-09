package com.homeauto.model;

import jakarta.persistence.*;

// Simple name/value table for the admin's system settings
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    private String settingKey;

    private String settingValue;

    public SystemSetting() {
    }

    public SystemSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}
