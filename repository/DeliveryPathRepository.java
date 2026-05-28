package com.campusdelivery.repository;

import com.campusdelivery.entity.DeliveryPath;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPathRepository extends JpaRepository<DeliveryPath, Long> {
    List<DeliveryPath> findAllByExpectedDeliveryTimeBetween(LocalDateTime from, LocalDateTime to);
}
