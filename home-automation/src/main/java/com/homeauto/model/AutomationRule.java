package com.homeauto.model;

import jakarta.persistence.*;

@Entity
@Table(name = "automation_rules")
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // TEMP_ABOVE, TEMP_BELOW or HUMIDITY_ABOVE
    private String conditionType;
    private double conditionValue;

    // ON or OFF
    private String action;

    private boolean enabled = true;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public AutomationRule() {
    }

    // Text shown in the rules table, e.g. "When temperature is above 30 C, turn Bedroom Fan ON"
    public String getDescription() {
        String value = (conditionValue == Math.floor(conditionValue))
                ? String.valueOf((long) conditionValue)
                : String.valueOf(conditionValue);
        String when;
        if ("TEMP_ABOVE".equals(conditionType)) {
            when = "temperature is above " + value + "\u00B0C";
        } else if ("TEMP_BELOW".equals(conditionType)) {
            when = "temperature is below " + value + "\u00B0C";
        } else {
            when = "humidity is above " + value + "%";
        }
        String deviceName = (device == null) ? "?" : device.getName();
        return "When " + when + ", turn " + deviceName + " " + action;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public double getConditionValue() { return conditionValue; }
    public void setConditionValue(double conditionValue) { this.conditionValue = conditionValue; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
