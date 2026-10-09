package com.homeauto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// One row per homeowner: latest temperature, humidity and security status of their home
@Entity
@Table(name = "environment_status")
public class EnvironmentStatus {

    @Id
    private Long userId;

    private double temperature = 26.0;
    private double humidity = 55.0;
    private String securityStatus = "Secure";   // "Secure" or "Alert"
    private LocalDateTime updatedAt = LocalDateTime.now();

    public EnvironmentStatus() {
    }

    public EnvironmentStatus(Long userId) {
        this.userId = userId;
    }

    public String getUpdatedText() {
        if (updatedAt == null) return "-";
        return updatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public String getSecurityStatus() { return securityStatus; }
    public void setSecurityStatus(String securityStatus) { this.securityStatus = securityStatus; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
