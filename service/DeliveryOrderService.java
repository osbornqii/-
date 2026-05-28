package com.campusdelivery.service;

import com.campusdelivery.dto.CreateOrderRequest;
import com.campusdelivery.dto.UpdateOrderRequest;
import com.campusdelivery.entity.DeliveryOrder;
import com.campusdelivery.repository.DeliveryOrderRepository;
import com.campusdelivery.repository.VehicleRepository;
import com.campusdelivery.entity.Vehicle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryOrderService {
    private static final DateTimeFormatter ORDER_NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final double VEHICLE_CAPACITY_KG = 15.0;
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "DISPATCHED");

    private final DeliveryOrderRepository repository;
    private final VehicleRepository vehicleRepository;

    public DeliveryOrderService(DeliveryOrderRepository repository, VehicleRepository vehicleRepository) {
        this.repository = repository;
        this.vehicleRepository = vehicleRepository;
    }

    public DeliveryOrder create(CreateOrderRequest request) {
        String orderNumber = request.orderNumber();
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = generateOrderNumber();
        }

        if (repository.findByOrderNumber(orderNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "orderNumber already exists");
        }

        if (request.expectedDeliveryTime() != null && request.expectedDeliveryTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配送时段不可选过去的时间");
        }

        DeliveryOrder order = new DeliveryOrder();
        order.setUser(request.user());
        order.setUserContact(request.userContact());
        order.setPickupCode(request.pickupCode());
        order.setDestination(request.destination());
        order.setExpectedDeliveryTime(request.expectedDeliveryTime());
        order.setOrderTime(request.orderTime() != null ? request.orderTime() : LocalDateTime.now());
        order.setOrderNumber(orderNumber);
        order.setOrderStatus(request.orderStatus() != null && !request.orderStatus().isBlank() ? request.orderStatus() : "PENDING");
        order.setWeightKg(request.weightKg());
        order.setLengthCm(request.lengthCm());
        order.setWidthCm(request.widthCm());
        order.setHeightCm(request.heightCm());

        try {
            return repository.save(order);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "orderNumber already exists");
        }
    }

    public List<DeliveryOrder> list() {
        return repository.findAll();
    }

    public List<DeliveryOrder> listByExpectedDeliveryTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return list();
        }
        return repository.findAllByExpectedDeliveryTimeBetween(start, end);
    }

    public List<DeliveryOrder> listByUserContact(String userContact) {
        if (userContact == null || userContact.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userContact is required");
        }
        return repository.findAllByUserContact(userContact);
    }

    public DeliveryOrder get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    public DeliveryOrder update(Long id, UpdateOrderRequest request) {
        DeliveryOrder order = get(id);

        String newOrderNumber = request.orderNumber();
        if (!order.getOrderNumber().equals(newOrderNumber) && repository.findByOrderNumber(newOrderNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "orderNumber already exists");
        }

        if ("DISPATCHED".equals(order.getOrderStatus()) || "DELIVERED".equals(order.getOrderStatus())) {
            if (!Objects.equals(order.getWeightKg(), request.weightKg())
                    || !Objects.equals(order.getLengthCm(), request.lengthCm())
                    || !Objects.equals(order.getWidthCm(), request.widthCm())
                    || !Objects.equals(order.getHeightCm(), request.heightCm())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "配送中/已送达订单不可更改包裹信息");
            }
        }

        order.setUser(request.user());
        order.setUserContact(request.userContact());
        order.setPickupCode(request.pickupCode());
        order.setDestination(request.destination());
        order.setExpectedDeliveryTime(request.expectedDeliveryTime());
        order.setOrderTime(request.orderTime());
        order.setOrderNumber(newOrderNumber);
        order.setOrderStatus(request.orderStatus());
        order.setWeightKg(request.weightKg());
        order.setLengthCm(request.lengthCm());
        order.setWidthCm(request.widthCm());
        order.setHeightCm(request.heightCm());

        try {
            return repository.save(order);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "orderNumber already exists");
        }
    }

    public DeliveryOrder updateStatus(Long id, String orderStatus) {
        DeliveryOrder order = get(id);
        order.setOrderStatus(orderStatus);
        return repository.save(order);
    }

    public DeliveryOrder updatePackage(Long id, Double weightKg, Double lengthCm, Double widthCm, Double heightCm) {
        DeliveryOrder order = get(id);

        if ("DISPATCHED".equals(order.getOrderStatus()) || "DELIVERED".equals(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "配送中/已送达订单不可更改包裹信息");
        }

        boolean hasUpdate = false;

        if (weightKg != null) {
            if (weightKg < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weightKg must be >= 0");
            }
            order.setWeightKg(weightKg);
            hasUpdate = true;
        }
        if (lengthCm != null) {
            if (lengthCm < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lengthCm must be >= 0");
            }
            order.setLengthCm(lengthCm);
            hasUpdate = true;
        }
        if (widthCm != null) {
            if (widthCm < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "widthCm must be >= 0");
            }
            order.setWidthCm(widthCm);
            hasUpdate = true;
        }
        if (heightCm != null) {
            if (heightCm < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heightCm must be >= 0");
            }
            order.setHeightCm(heightCm);
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no changes");
        }

        return repository.save(order);
    }

    public DeliveryOrder assignVehicle(Long id, String vehicleNumber) {
        DeliveryOrder order = get(id);
        String prev = order.getAssignedVehicleNumber();

        String normalized = vehicleNumber == null ? null : vehicleNumber.trim();
        if (normalized != null && normalized.isBlank()) normalized = null;

        if (normalized != null) {
            Vehicle vehicle = vehicleRepository.findByVehicleNumber(normalized)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vehicle not found"));
            if ("OFFLINE".equals(vehicle.getVehicleStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "vehicle is offline");
            }
            if (order.getWeightKg() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weightKg is required before assignment");
            }
            if (order.getWeightKg() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weightKg must be >= 0");
            }

            double occupied = repository.findAllByAssignedVehicleNumberAndOrderStatusIn(normalized, ACTIVE_STATUSES.stream().toList()).stream()
                    .filter(o -> o.getId() != null && !o.getId().equals(order.getId()))
                    .filter(o -> sameSlot(o.getExpectedDeliveryTime(), order.getExpectedDeliveryTime()))
                    .map(DeliveryOrder::getWeightKg)
                    .filter(w -> w != null && w > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            double remaining = Math.max(0, VEHICLE_CAPACITY_KG - occupied);
            if (order.getWeightKg() > remaining) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该车已满");
            }
        }

        order.setAssignedVehicleNumber(normalized);
        if (normalized != null) {
            order.setOrderStatus("DISPATCHED");
        }
        DeliveryOrder saved = repository.save(order);

        if (prev != null && !prev.isBlank() && (normalized == null || !prev.equals(normalized))) {
            refreshVehicleStatus(prev);
        }
        if (normalized != null) {
            refreshVehicleStatus(normalized);
        }

        return saved;
    }

    private boolean sameSlot(LocalDateTime value, LocalDateTime expected) {
        if (value == null || expected == null) return false;
        return value.getYear() == expected.getYear()
                && value.getMonthValue() == expected.getMonthValue()
                && value.getDayOfMonth() == expected.getDayOfMonth()
                && value.getHour() == expected.getHour();
    }

    private void refreshVehicleStatus(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) return;
        Vehicle vehicle = vehicleRepository.findByVehicleNumber(vehicleNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vehicle not found"));

        if ("OFFLINE".equals(vehicle.getVehicleStatus())) return;

        double total = repository.findAllByAssignedVehicleNumberAndOrderStatusIn(vehicleNumber, ACTIVE_STATUSES.stream().toList()).stream()
                .map(DeliveryOrder::getWeightKg)
                .filter(w -> w != null && w > 0)
                .mapToDouble(Double::doubleValue)
                .sum();

        String nextStatus = total > 0 ? "BUSY" : "IDLE";
        if (!nextStatus.equals(vehicle.getVehicleStatus())) {
            vehicle.setVehicleStatus(nextStatus);
            vehicleRepository.save(vehicle);
        }

        if (total > VEHICLE_CAPACITY_KG) {
            vehicle.setVehicleStatus("BUSY");
            vehicleRepository.save(vehicle);
        }
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        repository.deleteById(id);
    }

    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        int rand = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD-" + now.format(ORDER_NUMBER_TIME) + "-" + rand;
    }
}
