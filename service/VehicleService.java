package com.campusdelivery.service;

import com.campusdelivery.dto.CreateVehicleRequest;
import com.campusdelivery.dto.UpdateVehicleRequest;
import com.campusdelivery.entity.DeliveryOrder;
import com.campusdelivery.entity.Vehicle;
import com.campusdelivery.repository.DeliveryOrderRepository;
import com.campusdelivery.repository.VehicleRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleService {
    private final VehicleRepository repository;
    private final DeliveryOrderRepository orderRepository;

    public VehicleService(VehicleRepository repository, DeliveryOrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    public Vehicle create(CreateVehicleRequest request) {
        if (repository.findByVehicleNumber(request.vehicleNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "vehicleNumber already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(request.vehicleNumber());
        vehicle.setVehicleStatus(request.vehicleStatus());
        vehicle.setHistory(request.history());

        try {
            return repository.save(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "vehicleNumber already exists");
        }
    }

    public List<Vehicle> list() {
        return repository.findAll();
    }

    public Vehicle get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vehicle not found"));
    }

    public Vehicle update(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = get(id);

        String newNumber = request.vehicleNumber();
        if (!vehicle.getVehicleNumber().equals(newNumber) && repository.findByVehicleNumber(newNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "vehicleNumber already exists");
        }

        vehicle.setVehicleNumber(newNumber);
        vehicle.setVehicleStatus(request.vehicleStatus());
        vehicle.setHistory(request.history());

        try {
            return repository.save(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "vehicleNumber already exists");
        }
    }

    public Vehicle updateStatus(Long id, String vehicleStatus) {
        Vehicle vehicle = get(id);
        vehicle.setVehicleStatus(vehicleStatus);
        Vehicle saved = repository.save(vehicle);
        
        if ("IDLE".equals(vehicleStatus)) {
            List<DeliveryOrder> activeOrders = orderRepository.findAllByAssignedVehicleNumberAndOrderStatusIn(
                saved.getVehicleNumber(), List.of("DISPATCHED"));
            for (DeliveryOrder order : activeOrders) {
                order.setOrderStatus("DELIVERED");
                orderRepository.save(order);
            }
        }
        
        return saved;
    }

    public Vehicle updateStatusByVehicleNumber(String vehicleNumber, String vehicleStatus) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleNumber is required");
        }
        Vehicle vehicle = repository.findByVehicleNumber(vehicleNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vehicle not found"));
        vehicle.setVehicleStatus(vehicleStatus);
        Vehicle saved = repository.save(vehicle);

        if ("IDLE".equals(vehicleStatus)) {
            List<DeliveryOrder> activeOrders = orderRepository.findAllByAssignedVehicleNumberAndOrderStatusIn(
                saved.getVehicleNumber(), List.of("DISPATCHED"));
            for (DeliveryOrder order : activeOrders) {
                order.setOrderStatus("DELIVERED");
                orderRepository.save(order);
            }
        }

        return saved;
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "vehicle not found");
        }
        repository.deleteById(id);
    }
}
