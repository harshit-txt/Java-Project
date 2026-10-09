package com.homeauto.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "devices")
public class Device {

    public static final List<String> TYPES =
            List.of("Light", "Fan", "AC", "Thermostat", "Door Lock", "Camera");
    public static final List<String> PROTOCOLS =
            List.of("Wi-Fi", "Zigbee", "Bluetooth", "Z-Wave");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String room;
    private String protocol;

    // PENDING = waiting for admin, APPROVED = can be used, REJECTED = not compatible
    private String status = "PENDING";

    private boolean power = false;
    private int level = 50;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Device() {
    }

    public Device(String name, String type, String room, String protocol, User owner) {
        this.name = name;
        this.type = type;
        this.room = room;
        this.protocol = protocol;
        this.owner = owner;
        this.level = isTemperatureDevice() ? 24 : 50;
    }

    // ---- helper methods used by the HTML pages (not saved in the database) ----

    private boolean isTemperatureDevice() {
        return "AC".equals(type) || "Thermostat".equals(type);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isAdjustable() {
        return !("Door Lock".equals(type) || "Camera".equals(type));
    }

    public int getMin() {
        return isTemperatureDevice() ? 16 : 0;
    }

    public int getMax() {
        return isTemperatureDevice() ? 30 : 100;
    }

    public String getSettingLabel() {
        if ("Light".equals(type)) return "Brightness (%)";
        if ("Fan".equals(type)) return "Speed (%)";
        if (isTemperatureDevice()) return "Temperature (\u00B0C)";
        return "Level";
    }

    public String getStateText() {
        if ("Door Lock".equals(type)) return power ? "Locked" : "Unlocked";
        if ("Camera".equals(type)) return power ? "Recording" : "Off";
        return power ? "On" : "Off";
    }

    public String getActionText() {
        if ("Door Lock".equals(type)) return power ? "Unlock" : "Lock";
        return power ? "Turn off" : "Turn on";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPower() { return power; }
    public void setPower(boolean power) { this.power = power; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
