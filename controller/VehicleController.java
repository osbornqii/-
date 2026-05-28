package com.campusdelivery.controller;

import com.campusdelivery.dto.CreateVehicleRequest;
import com.campusdelivery.dto.UpdateVehicleRequest;
import com.campusdelivery.dto.UpdateVehicleStatusRequest;
import com.campusdelivery.entity.Vehicle;
import com.campusdelivery.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody CreateVehicleRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Vehicle> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Vehicle get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable Long id, @Valid @RequestBody UpdateVehicleRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public Vehicle updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateVehicleStatusRequest request) {
        return service.updateStatus(id, request.vehicleStatus());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

