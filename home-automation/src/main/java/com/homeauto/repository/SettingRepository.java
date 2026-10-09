package com.homeauto.repository;

import com.homeauto.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SystemSetting, String> {
}
