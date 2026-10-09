package com.homeauto.service;

import com.homeauto.model.Device;
import com.homeauto.model.User;
import com.homeauto.repository.DeviceRepository;
import com.homeauto.repository.EnvironmentRepository;
import com.homeauto.repository.RuleRepository;
import com.homeauto.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final EnvironmentRepository environmentRepository;

    public UserService(UserRepository userRepository,
                       DeviceRepository deviceRepository,
                       RuleRepository ruleRepository,
                       EnvironmentRepository environmentRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.ruleRepository = ruleRepository;
        this.environmentRepository = environmentRepository;
    }

    // Deletes a user together with everything that belongs to them
    @Transactional
    public void deleteUser(User user) {
        ruleRepository.deleteAll(ruleRepository.findByOwnerOrderByIdAsc(user));
        for (Device device : deviceRepository.findByOwnerOrderByIdAsc(user)) {
            deviceRepository.delete(device);
        }
        if (environmentRepository.existsById(user.getId())) {
            environmentRepository.deleteById(user.getId());
        }
        userRepository.delete(user);
    }
}
