package com.campusdelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "paths")
public class DeliveryPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @Column(name = "expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    @Lob
    @Column(name = "planning_result", columnDefinition = "TEXT")
    private String planningResult;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public LocalDateTime getExpectedDeliveryTime() {
        return expectedDeliveryTime;
    }

    public void setExpectedDeliveryTime(LocalDateTime expectedDeliveryTime) {
        this.expectedDeliveryTime = expectedDeliveryTime;
    }

    public String getPlanningResult() {
        return planningResult;
    }

    public void setPlanningResult(String planningResult) {
        this.planningResult = planningResult;
    }
}
