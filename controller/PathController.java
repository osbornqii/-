package com.campusdelivery.controller;

import com.campusdelivery.dto.CreatePathRequest;
import com.campusdelivery.dto.UpdatePathRequest;
import com.campusdelivery.entity.DeliveryPath;
import com.campusdelivery.service.DeliveryPathService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/paths")
public class PathController {
    private final DeliveryPathService service;

    public PathController(DeliveryPathService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryPath create(@Valid @RequestBody CreatePathRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<DeliveryPath> list(@RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                   @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (start != null && end != null) {
            return service.listByExpectedDeliveryTime(start, end);
        }
        return service.list();
    }

    @GetMapping("/{id}")
    public DeliveryPath get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public DeliveryPath update(@PathVariable Long id, @Valid @RequestBody UpdatePathRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

