package com.campusdelivery.repository;

import com.campusdelivery.entity.DeliveryOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    Optional<DeliveryOrder> findByOrderNumber(String orderNumber);

    List<DeliveryOrder> findAllByUserContact(String userContact);

    List<DeliveryOrder> findAllByAssignedVehicleNumberAndOrderStatusIn(String assignedVehicleNumber, List<String> orderStatus);

    List<DeliveryOrder> findAllByExpectedDeliveryTimeBetween(LocalDateTime start, LocalDateTime end);
}

