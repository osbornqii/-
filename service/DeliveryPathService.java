package com.campusdelivery.service;

import com.campusdelivery.dto.CalculatePathRequest;
import com.campusdelivery.dto.CreatePathRequest;
import com.campusdelivery.dto.UpdatePathRequest;
import com.campusdelivery.entity.DeliveryOrder;
import com.campusdelivery.entity.DeliveryPath;
import com.campusdelivery.repository.DeliveryOrderRepository;
import com.campusdelivery.repository.DeliveryPathRepository;
import com.campusdelivery.algorithm.GeneticAlgorithmWithDB;
import com.campusdelivery.repository.VehicleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryPathService {
    private final DeliveryPathRepository repository;
    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double[][] COORDINATES = {
            {110.300710, 25.106313},
            {110.298548, 25.101785},
            {110.290096, 25.097971},
            {110.293150, 25.097369},
            {110.289969, 25.098731},
            {110.292174, 25.097044},
            {110.300053, 25.105328},
            {110.294803, 25.098692},
            {110.297748, 25.105215}
    };

    private static final String[] NAMES = {
            "快递站(仓库)",
            "图书馆",
            "研究生公寓",
            "新5期大门",
            "容园",
            "5期宿舍",
            "雅园",
            "恬园",
            "一期学生公寓"
    };

    private static final Map<String, Integer> DEST_TO_ID = Map.of(
            "图书馆", 1,
            "研究生公寓", 2,
            "新5期大门", 3,
            "容园", 4,
            "5期宿舍", 5,
            "雅园", 6,
            "恬园", 7,
            "一期学生公寓", 8,
            "快递站(仓库)", 0
    );

    private static final List<String> DEFAULT_VEHICLES = List.of("V-001", "V-002", "V-003");
    private static final List<String> ALL_STATUSES = List.of("PENDING", "DISPATCHED", "DELIVERED");
    private static final double VEHICLE_CAPACITY_KG = 15.0;
    private static final Pattern ROUTE_PATTERN = Pattern.compile("0-\\d+(?:-\\d+)*-0");
    private final double[][] distanceMatrix = buildDistanceMatrix();

    public DeliveryPathService(DeliveryPathRepository repository, DeliveryOrderRepository orderRepository, VehicleRepository vehicleRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public DeliveryPath create(CreatePathRequest request) {
        DeliveryPath path = new DeliveryPath();
        path.setComputedAt(request.computedAt());
        path.setExpectedDeliveryTime(request.expectedDeliveryTime());
        path.setPlanningResult(request.planningResult());
        return repository.save(path);
    }

    public DeliveryPath calculate(CalculatePathRequest request) {
        long count = vehicleRepository.count();
        GeneticAlgorithmWithDB ga = new GeneticAlgorithmWithDB((int) count);
        Map<String, Object> result = ga.run();
        ga.close();
        Object total = result.get("totalDistance");
        Object pathsObj = result.get("paths");
        String paths = "";
        if (pathsObj instanceof List<?> list) {
            paths = list.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        String json = "{\"totalDistance\":" + (total == null ? 0 : total.toString()) + ",\"paths\":[" + paths + "]}";
        DeliveryPath path = new DeliveryPath();
        path.setComputedAt(LocalDateTime.now());
        path.setExpectedDeliveryTime(request.expectedDeliveryTime());
        path.setPlanningResult(json);
        return repository.save(path);
    }

    public DeliveryPath calculateAssigned(CalculatePathRequest request) {
        LocalDateTime expected = request.expectedDeliveryTime();
        if (expected == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedDeliveryTime is required");
        }

        // 1. 获取全量订单进行匹配，彻底解决数据库查询兼容性问题
        List<DeliveryOrder> allOrders = orderRepository.findAll();
        
        // 调试用：记录匹配过程
        long countWithExpectedDate = allOrders.stream()
                .filter(o -> o.getExpectedDeliveryTime() != null 
                    && o.getExpectedDeliveryTime().getYear() == expected.getYear()
                    && o.getExpectedDeliveryTime().getMonthValue() == expected.getMonthValue()
                    && o.getExpectedDeliveryTime().getDayOfMonth() == expected.getDayOfMonth())
                .count();

        List<DeliveryOrder> slotOrders = allOrders.stream()
                .filter(o -> {
                    LocalDateTime val = o.getExpectedDeliveryTime();
                    if (val == null) return false;
                    LocalDateTime slotStart = expected.withMinute(0).withSecond(0).withNano(0);
                    LocalDateTime slotEnd = slotStart.plusHours(1);
                    return !val.isBefore(slotStart) && val.isBefore(slotEnd);
                })
                .toList();

        // 2. 订单过滤：只排除取消类，其余（含已送达/未分配车辆）都参与路径计算
        List<DeliveryOrder> candidateOrders = slotOrders.stream()
                .filter(o -> {
                    String s = o.getOrderStatus();
                    if (s == null) return true;
                    String upper = s.toUpperCase();
                    return !(upper.contains("CANCEL") || s.contains("取消"));
                })
                .toList();

        List<DeliveryOrder> assignedOrders = candidateOrders.stream()
                .filter(o -> o.getAssignedVehicleNumber() != null && !o.getAssignedVehicleNumber().trim().isEmpty())
                .toList();

        Map<String, List<DeliveryOrder>> ordersByVehicle = new HashMap<>(
                assignedOrders.stream().collect(Collectors.groupingBy(o -> o.getAssignedVehicleNumber().trim()))
        );

        // 3. 准备车辆集合
        List<com.campusdelivery.entity.Vehicle> allVehiclesFromDb = vehicleRepository.findAll();
        Set<String> vehicleNumberSet = new TreeSet<>(); // 使用 TreeSet 自动排序
        
        // 加入当前活跃车辆
        allVehiclesFromDb.forEach(v -> vehicleNumberSet.add(v.getVehicleNumber().trim()));
        // 加入历史订单中出现的车辆（确保回溯时能看到路线）
        vehicleNumberSet.addAll(ordersByVehicle.keySet());
        
        if (vehicleNumberSet.isEmpty()) {
            vehicleNumberSet.addAll(DEFAULT_VEHICLES);
        }

        // 4. 未分配车辆订单也参与规划：按车辆列表轮询分配（不写回数据库）
        List<DeliveryOrder> unassignedOrders = candidateOrders.stream()
                .filter(o -> o.getAssignedVehicleNumber() == null || o.getAssignedVehicleNumber().trim().isEmpty())
                .toList();
        if (!unassignedOrders.isEmpty()) {
            List<String> vehicleNumbers = new ArrayList<>(vehicleNumberSet);
            int n = vehicleNumbers.size();
            for (int i = 0; i < unassignedOrders.size(); i++) {
                String vn = vehicleNumbers.get(i % n);
                ordersByVehicle.computeIfAbsent(vn, k -> new ArrayList<>()).add(unassignedOrders.get(i));
            }
        }

        Map<String, Object> root = new HashMap<>();
        List<Map<String, Object>> vehiclesResult = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        double totalDistanceKm = 0;
        GeneticAlgorithmWithDB ga = new GeneticAlgorithmWithDB();

        try {
            for (String vehicleNumber : vehicleNumberSet) {
                List<DeliveryOrder> orders = ordersByVehicle.getOrDefault(vehicleNumber, List.of());
                List<Integer> destIds = orders.stream()
                        .map(DeliveryOrder::getDestination)
                        .filter(Objects::nonNull)
                        .map(DeliveryPathService::destinationToId)
                        .filter(id -> id != null && id != 0)
                        .distinct()
                        .toList();

                boolean hasTask = !destIds.isEmpty();
                List<Integer> route = hasTask ? ga.solveTspRoute(destIds) : List.of();
                String pathStr = hasTask ? routeToString(route) : "";
                double distanceKm = hasTask ? ga.routeDistanceKm(route) : 0;
                totalDistanceKm += distanceKm;

                Map<String, Object> v = new HashMap<>();
                v.put("vehicleNumber", vehicleNumber);
                v.put("capacityKg", VEHICLE_CAPACITY_KG);
                v.put("loadKg", loadKg(orders));
                v.put("distanceKm", distanceKm);
                v.put("path", pathStr);
                v.put("stops", hasTask ? route.stream().map(i -> NAMES[i]).toList() : List.of());
                v.put("hasTask", hasTask);
                v.put("orderCount", orders.size());
                
                String status = vehicleRepository.findByVehicleNumber(vehicleNumber)
                        .map(com.campusdelivery.entity.Vehicle::getVehicleStatus)
                        .orElse("UNKNOWN");
                v.put("vehicleStatus", status);
                
                vehiclesResult.add(v);
                paths.add(pathStr);
            }
        } finally {
            ga.close();
        }

        boolean hasAnyRoute = vehiclesResult.stream().anyMatch(v -> {
            Object pathObj = v.get("path");
            String pathStr = pathObj == null ? "" : String.valueOf(pathObj);
            if (!pathStr.isBlank()) return true;
            Object hasTaskObj = v.get("hasTask");
            if (Boolean.TRUE.equals(hasTaskObj)) return true;
            Object orderCountObj = v.get("orderCount");
            if (orderCountObj instanceof Number n) return n.intValue() > 0;
            try {
                return Integer.parseInt(String.valueOf(orderCountObj)) > 0;
            } catch (Exception ignored) {
                return false;
            }
        });

        boolean force = Boolean.TRUE.equals(request.force());
        if (!force && !hasAnyRoute) {
            DeliveryPath existing = findLatestNonEmptyPathForSlot(expected);
            if (existing != null) {
                return existing;
            }
        }

        root.put("totalOrdersFound", candidateOrders.size());
        root.put("debug_totalOrdersInDb", allOrders.size());
        root.put("debug_countWithExpectedDate", countWithExpectedDate);
        root.put("debug_countInSlotBeforeFilter", slotOrders.size());
        root.put("debug_planner", "assigned-calculate-v2");
        root.put("debug_slotStart", expected.withMinute(0).withSecond(0).withNano(0).toString());
        root.put("debug_slotEnd", expected.withMinute(0).withSecond(0).withNano(0).plusHours(1).toString());
        if (!allOrders.isEmpty()) {
            root.put("debug_sampleDates", allOrders.stream()
                .map(o -> String.valueOf(o.getExpectedDeliveryTime()))
                .limit(5)
                .toList());
        }
        root.put("debug_slotOrders", candidateOrders.stream()
                .limit(30)
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", o.getId());
                    m.put("expectedDeliveryTime", String.valueOf(o.getExpectedDeliveryTime()));
                    m.put("destination", o.getDestination());
                    m.put("mappedId", destinationToId(o.getDestination()));
                    m.put("assignedVehicleNumber", o.getAssignedVehicleNumber());
                    m.put("orderStatus", o.getOrderStatus());
                    return m;
                })
                .toList());
        root.put("debug_mappedNullCount", candidateOrders.stream()
                .map(DeliveryOrder::getDestination)
                .map(DeliveryPathService::destinationToId)
                .filter(Objects::isNull)
                .count());
        root.put("totalDistanceKm", totalDistanceKm);
        root.put("expectedDeliveryTime", expected.toString());
        root.put("vehicles", vehiclesResult);
        root.put("paths", paths);

        String json;
        try {
            json = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize planning result failed", e);
        }

        DeliveryPath path = new DeliveryPath();
        path.setComputedAt(LocalDateTime.now());
        path.setExpectedDeliveryTime(expected);
        path.setPlanningResult(json);
        return repository.save(path);
    }

    private DeliveryPath findLatestNonEmptyPathForSlot(LocalDateTime expected) {
        if (expected == null) return null;
        LocalDateTime slotStart = expected.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slotEnd = slotStart.plusHours(1);
        return repository.findAll().stream()
                .filter(p -> {
                    LocalDateTime t = p.getExpectedDeliveryTime();
                    if (t == null) return false;
                    return !t.isBefore(slotStart) && (t.isBefore(slotEnd) || t.isEqual(slotEnd));
                })
                .filter(p -> {
                    String pr = p.getPlanningResult();
                    if (pr == null || pr.isBlank()) return false;
                    return ROUTE_PATTERN.matcher(pr).find();
                })
                .max((a, b) -> {
                    LocalDateTime ta = a.getComputedAt();
                    LocalDateTime tb = b.getComputedAt();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return -1;
                    if (tb == null) return 1;
                    return ta.compareTo(tb);
                })
                .orElse(null);
    }

    private double loadKg(List<DeliveryOrder> orders) {
        if (orders == null) return 0;
        return orders.stream()
                .map(DeliveryOrder::getWeightKg)
                .filter(w -> w != null && w > 0)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private boolean sameSlot(LocalDateTime value, LocalDateTime expected) {
        if (value == null || expected == null) return false;
        return value.getYear() == expected.getYear()
                && value.getMonthValue() == expected.getMonthValue()
                && value.getDayOfMonth() == expected.getDayOfMonth()
                && value.getHour() == expected.getHour();
    }

    private static Integer destinationToId(String destination) {
        if (destination == null) return null;
        String raw = destination.trim();
        if (raw.isEmpty()) return null;
        Integer direct = DEST_TO_ID.get(raw);
        if (direct != null) return direct;

        String s = normalizeDestination(raw);
        if (s.isEmpty()) return null;
        direct = DEST_TO_ID.get(s);
        if (direct != null) return direct;

        if (s.contains("快递站") || s.contains("仓库")) return 0;
        if (s.contains("图书馆")) return 1;
        if (s.contains("研究生")) return 2;
        if (s.contains("新5期") || s.contains("新五期")) return 3;
        if (s.contains("容园")) return 4;
        if (s.contains("5期宿舍") || s.contains("五期宿舍") || (s.contains("5期") && s.contains("宿舍")) || (s.contains("五期") && s.contains("宿舍"))) return 5;
        if (s.contains("雅园")) return 6;
        if (s.contains("恬园")) return 7;
        if (s.contains("一期学生公寓") || s.contains("1期学生公寓") || ((s.contains("一期") || s.contains("1期")) && (s.contains("公寓") || s.contains("宿舍")))) return 8;
        return null;
    }

    private static String normalizeDestination(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '\u3000') continue;
            if (c >= '\uFF10' && c <= '\uFF19') {
                sb.append((char) (c - '\uFF10' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private double[][] buildDistanceMatrix() {
        int n = COORDINATES.length;
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    m[i][j] = 0;
                } else {
                    m[i][j] = haversineMeters(COORDINATES[i][0], COORDINATES[i][1], COORDINATES[j][0], COORDINATES[j][1]);
                }
            }
        }
        return m;
    }

    private double haversineMeters(double lng1, double lat1, double lng2, double lat2) {
        double rlat1 = Math.toRadians(lat1);
        double rlon1 = Math.toRadians(lng1);
        double rlat2 = Math.toRadians(lat2);
        double rlon2 = Math.toRadians(lng2);
        double dlon = rlon2 - rlon1;
        double dlat = rlat2 - rlat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(rlat1) * Math.cos(rlat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6371000 * c;
    }

    private List<Integer> planRouteNearestNeighbor(List<Integer> destIds) {
        if (destIds == null || destIds.isEmpty()) {
            return List.of(0);
        }
        List<Integer> remaining = new ArrayList<>(destIds);
        List<Integer> route = new ArrayList<>();
        route.add(0);
        int current = 0;
        while (!remaining.isEmpty()) {
            int bestIdx = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                int candidate = remaining.get(i);
                double d = distanceMatrix[current][candidate];
                if (d < bestDist) {
                    bestDist = d;
                    bestIdx = i;
                }
            }
            int next = remaining.remove(bestIdx);
            route.add(next);
            current = next;
        }
        route.add(0);
        twoOpt(route);
        return route;
    }

    private void twoOpt(List<Integer> route) {
        if (route == null || route.size() < 5) return;
        boolean improved = true;
        int iterations = 0;
        while (improved && iterations < 30) {
            improved = false;
            iterations++;
            for (int i = 1; i < route.size() - 2; i++) {
                for (int j = i + 1; j < route.size() - 1; j++) {
                    double before = distanceMatrix[route.get(i - 1)][route.get(i)] + distanceMatrix[route.get(j)][route.get(j + 1)];
                    double after = distanceMatrix[route.get(i - 1)][route.get(j)] + distanceMatrix[route.get(i)][route.get(j + 1)];
                    if (after + 1e-6 < before) {
                        reverse(route, i, j);
                        improved = true;
                    }
                }
            }
        }
    }

    private void reverse(List<Integer> route, int i, int j) {
        while (i < j) {
            int tmp = route.get(i);
            route.set(i, route.get(j));
            route.set(j, tmp);
            i++;
            j--;
        }
    }

    private double routeDistanceKm(List<Integer> route) {
        if (route == null || route.size() < 2) return 0;
        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            total += distanceMatrix[route.get(i)][route.get(i + 1)];
        }
        return total / 1000.0;
    }

    private String routeToString(List<Integer> route) {
        if (route == null || route.isEmpty()) return "";
        if (route.size() == 1 && route.get(0) == 0) return "";
        return route.stream().map(String::valueOf).collect(Collectors.joining("-"));
    }

    public List<DeliveryPath> list() {
        return repository.findAll();
    }

    public List<DeliveryPath> listByExpectedDeliveryTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return list();
        }
        // 使用全量内存过滤，规避 SQLite 对 LocalDateTime 的查询兼容性问题
        return repository.findAll().stream()
                .filter(p -> {
                    LocalDateTime time = p.getExpectedDeliveryTime();
                    return time != null && !time.isBefore(start) && !time.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    public DeliveryPath get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "path not found"));
    }

    public DeliveryPath update(Long id, UpdatePathRequest request) {
        DeliveryPath path = get(id);
        path.setComputedAt(request.computedAt());
        path.setExpectedDeliveryTime(request.expectedDeliveryTime());
        path.setPlanningResult(request.planningResult());
        return repository.save(path);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "path not found");
        }
        repository.deleteById(id);
    }

    
}
