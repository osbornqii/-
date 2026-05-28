package com.campusdelivery.controller;

import com.campusdelivery.dto.UpdateVehicleStatusByNumberRequest;
import com.campusdelivery.entity.Vehicle;
import com.campusdelivery.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicle")
public class CompatVehicleController {
    private final VehicleService service;

    public CompatVehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public List<Vehicle> list() {
        return service.list();
    }

    @PutMapping("/status")
    public Vehicle updateStatus(@Valid @RequestBody UpdateVehicleStatusByNumberRequest request) {
        return service.updateStatusByVehicleNumber(request.vehicleNumber(), request.vehicleStatus());
    }
}

