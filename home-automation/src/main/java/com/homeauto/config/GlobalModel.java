package com.homeauto.config;

import com.homeauto.controller.AdminController;
import com.homeauto.controller.AuthController;
import com.homeauto.controller.HomeController;
import com.homeauto.service.SettingService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Makes the site name (from System Settings) available on every page
@ControllerAdvice(assignableTypes = {AuthController.class, AdminController.class, HomeController.class})
public class GlobalModel {

    private final SettingService settingService;

    public GlobalModel(SettingService settingService) {
        this.settingService = settingService;
    }

    @ModelAttribute("siteName")
    public String siteName() {
        return settingService.getSiteName();
    }
}
