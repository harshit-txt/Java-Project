package com.homeauto.repository;

import com.homeauto.model.AutomationRule;
import com.homeauto.model.Device;
import com.homeauto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByOwnerOrderByIdAsc(User owner);

    List<AutomationRule> findByDevice(Device device);
}
