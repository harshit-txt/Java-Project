package com.homeauto.repository;

import com.homeauto.model.Device;
import com.homeauto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByOwnerOrderByIdAsc(User owner);

    long countByOwner(User owner);

    long countByStatus(String status);
}
