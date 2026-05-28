package com.campusdelivery.controller;

import com.campusdelivery.dto.CreateOrderRequest;
import com.campusdelivery.dto.SubmitOrderRequest;
import com.campusdelivery.entity.DeliveryOrder;
import com.campusdelivery.service.DeliveryOrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class CompatOrderController {
    private final DeliveryOrderService service;

    public CompatOrderController(DeliveryOrderService service) {
        this.service = service;
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryOrder submit(@Valid @RequestBody SubmitOrderRequest request) {
        CreateOrderRequest create = new CreateOrderRequest(
                request.user(),
                request.userContact(),
                request.pickupCode(),
                request.destination(),
                request.expectedDeliveryTime(),
                LocalDateTime.now(),
                null,
                "PENDING",
                null,
                null,
                null,
                null
        );
        return service.create(create);
    }

    @GetMapping("/query")
    public List<DeliveryOrder> query(@RequestParam(name = "phone", required = false) String phone,
                                    @RequestParam(name = "userContact", required = false) String userContact) {
        String contact = (phone != null && !phone.isBlank()) ? phone : userContact;
        return service.listByUserContact(contact);
    }

    @GetMapping("/list")
    public List<DeliveryOrder> list() {
        return service.list();
    }
}
