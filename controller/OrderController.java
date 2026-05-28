package com.campusdelivery.controller;

import com.campusdelivery.dto.CreateOrderRequest;
import com.campusdelivery.dto.UpdateOrderRequest;
import com.campusdelivery.dto.UpdateOrderAssignVehicleRequest;
import com.campusdelivery.dto.UpdateOrderPackageRequest;
import com.campusdelivery.dto.UpdateOrderStatusRequest;
import com.campusdelivery.entity.DeliveryOrder;
import com.campusdelivery.service.DeliveryOrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final DeliveryOrderService service;

    public OrderController(DeliveryOrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryOrder create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<DeliveryOrder> list(@RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                   @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (start != null || end != null) {
            return service.listByExpectedDeliveryTime(start, end);
        }
        return service.list();
    }

    @GetMapping("/{id}")
    public DeliveryOrder get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public DeliveryOrder update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public DeliveryOrder updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateStatus(id, request.orderStatus());
    }

    @PatchMapping("/{id}/package")
    public DeliveryOrder updatePackage(@PathVariable Long id, @RequestBody UpdateOrderPackageRequest request) {
        return service.updatePackage(id, request.weightKg(), request.lengthCm(), request.widthCm(), request.heightCm());
    }

    @PatchMapping("/{id}/vehicle")
    public DeliveryOrder assignVehicle(@PathVariable Long id, @RequestBody UpdateOrderAssignVehicleRequest request) {
        return service.assignVehicle(id, request.vehicleNumber());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
