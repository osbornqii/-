package com.campusdelivery.controller;

import com.campusdelivery.dto.CalculatePathRequest;
import com.campusdelivery.entity.DeliveryPath;
import com.campusdelivery.service.DeliveryPathService;
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
@RequestMapping("/api/path")
public class CompatPathController {
    private final DeliveryPathService service;

    public CompatPathController(DeliveryPathService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPath calculate(@Valid @RequestBody CalculatePathRequest request) {
        return service.calculate(request);
    }

    @PostMapping("/assigned-calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPath calculateAssigned(@Valid @RequestBody CalculatePathRequest request) {
        return service.calculateAssigned(request);
    }

    @GetMapping("/get")
    public List<DeliveryPath> get(@RequestParam("from") LocalDateTime from, @RequestParam("to") LocalDateTime to) {
        return service.listByExpectedDeliveryTime(from, to);
    }
}
