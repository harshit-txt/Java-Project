package com.homeauto.service;

import com.homeauto.model.Device;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.RuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;

    public DeviceService(DeviceRepository deviceRepository, RuleRepository ruleRepository) {
        this.deviceRepository = deviceRepository;
        this.ruleRepository = ruleRepository;
    }

    // Rules that use the device must go first, otherwise the database complains
    @Transactional
    public void deleteDevice(Device device) {
        ruleRepository.deleteAll(ruleRepository.findByDevice(device));
        deviceRepository.delete(device);
    }
}
