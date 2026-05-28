package com.campusdelivery.algorithm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GeneticAlgorithmWithDB {
    private static final double[][] COORDINATES = {
            {110.300710, 25.106313}, // 快递站
            {110.298548, 25.101785}, // 图书馆
            {110.290096, 25.097971}, // 研究生公寓
            {110.293150, 25.097369}, // 新5期大门
            {110.289969, 25.098731}, // 容园
            {110.292174, 25.097044}, // 5期宿舍
            {110.300053, 25.105328}, // 雅园
            {110.294803, 25.098692}, // 恬园
            {110.297748, 25.105215}, // 一期学生公寓
            {110.291542, 25.101342}, // 生命科学学院
            {110.292259, 25.101475}, // 环境与资源学院
            {110.293623, 25.104240}, // 法学院
            {110.293902, 25.104161}, // 马克思主义学院
            {110.294101, 25.104385}, // 政治与行政学院
            {110.294863, 25.103937}, // 经济管理学院
            {110.297023, 25.099948}, // 厚藩楼
            {110.293449, 25.104865}, // 公共体育教研部
            {110.289793, 25.102530}, // 综合体育馆
            {110.293882, 25.103393}, // 文学院
            {110.299679, 25.103821}, // 美术学院
            {110.300410, 25.104019}  // 后勤接待培训中心静园宾馆
    };

    private static final String[] NAMES = {
            "快递站",
            "图书馆",
            "研究生公寓",
            "新5期大门",
            "容园",
            "雅园",
            "恬园",
            "5期宿舍",
            "一期学生公寓",
            "生命科学学院",
            "环境与资源学院",
            "法学院",
            "马克思主义学院",
            "政治与行政学院",
            "经济管理学院",
            "厚藩楼",
            "公共体育教研部",
            "综合体育馆",
            "文学院",
            "美术学院",
            "后勤接待培训中心静园宾馆"
    };

    private static final String DB_URL = "jdbc:sqlite:./campus_delivery.db";
    private Connection connection;

    private int vehicleNum;
    private static final int VEHICLE_CAPACITY = 15;
    private static final int POP_SIZE = 200;
    private static final int GENERATIONS = 400;
    private static final double MUTATION_RATE = 0.15;
    private static final double CROSSOVER_RATE = 0.9;
    private static final int TOURNAMENT_SIZE = 5;
    private static final double ELITE_RATE = 0.1;

    private double[][] distanceMatrix;
    private final Random random = new Random();
    private int[] demands;

    public GeneticAlgorithmWithDB() {
        this(3);
    }

    public GeneticAlgorithmWithDB(int vehicleNum) {
        this.vehicleNum = vehicleNum <= 0 ? 3 : vehicleNum;
        connectToDatabase();
        loadDemandsFromDB();
        calculateDistanceMatrix();
    }

    private void connectToDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTablesIfNotExist();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("database init failed", e);
        }
    }

    private void createTablesIfNotExist() throws SQLException {
        String createOrderTable = """
            CREATE TABLE IF NOT EXISTS tb_order (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_name TEXT,
                user_tel TEXT,
                cargo_id TEXT,
                building_id INTEGER,
                time_slot TEXT,
                order_time TEXT,
                status TEXT
            )
            """;

        String createBuildingTable = """
            CREATE TABLE IF NOT EXISTS tb_building (
                id INTEGER PRIMARY KEY,
                name TEXT,
                lat DOUBLE,
                lng DOUBLE
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createOrderTable);
            stmt.execute(createBuildingTable);
        }

        String checkSql = "SELECT COUNT(*) FROM tb_building";
        String insertSql = "INSERT INTO tb_building (id, name, lat, lng) VALUES (?, ?, ?, ?)";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(checkSql);
            rs.next();
            int count = rs.getInt(1);
            if (count == 0) {
                try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                    for (int i = 0; i < COORDINATES.length; i++) {
                        pstmt.setInt(1, i);
                        pstmt.setString(2, NAMES[i]);
                        pstmt.setDouble(3, COORDINATES[i][1]);
                        pstmt.setDouble(4, COORDINATES[i][0]);
                        pstmt.executeUpdate();
                    }
                }
            }
        }
    }

    private void loadDemandsFromDB() {
        demands = new int[COORDINATES.length];
        demands[0] = 0;
        String sql = """
            SELECT building_id, COUNT(*) as order_count
            FROM tb_order
            WHERE status = '待配送'
            GROUP BY building_id
            """;
        int total = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int buildingId = rs.getInt("building_id");
                int orderCount = rs.getInt("order_count");
                if (buildingId >= 0 && buildingId < demands.length) {
                    demands[buildingId] = orderCount;
                    total += orderCount;
                }
            }
            if (total == 0) {
                setDefaultDemands();
            }
        } catch (SQLException e) {
            setDefaultDemands();
        }
    }

    private void setDefaultDemands() {
        demands = new int[]{
                0,
                4,
                3,
                5,
                2,
                4,
                3,
                5,
                4
        };
    }

    private void calculateDistanceMatrix() {
        int n = COORDINATES.length;
        distanceMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    distanceMatrix[i][j] = calculateDistance(i, j);
                }
            }
        }
    }

    private double calculateDistance(int i, int j) {
        double lat1 = Math.toRadians(COORDINATES[i][1]);
        double lon1 = Math.toRadians(COORDINATES[i][0]);
        double lat2 = Math.toRadians(COORDINATES[j][1]);
        double lon2 = Math.toRadians(COORDINATES[j][0]);
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371000;
        return c * r;
    }

    private class Chromosome implements Comparable<Chromosome> {
        List<Integer> genes;
        double fitness;
        double totalDistance;
        List<List<Integer>> routes;
        List<Integer> vehicleLoads;

        Chromosome(List<Integer> genes) {
            this.genes = new ArrayList<>(genes);
            decode();
            evaluate();
        }

        private void decode() {
            routes = new ArrayList<>();
            vehicleLoads = new ArrayList<>();
            for (int i = 0; i < vehicleNum; i++) {
                routes.add(new ArrayList<>());
                vehicleLoads.add(0);
            }
            int currentVehicle = 0;
            for (int point : genes) {
                int demand = demands[point];
                boolean assigned = false;
                for (int attempt = 0; attempt < vehicleNum; attempt++) {
                    int vehicleIdx = (currentVehicle + attempt) % vehicleNum;
                    if (vehicleLoads.get(vehicleIdx) + demand <= VEHICLE_CAPACITY) {
                        routes.get(vehicleIdx).add(point);
                        vehicleLoads.set(vehicleIdx, vehicleLoads.get(vehicleIdx) + demand);
                        currentVehicle = vehicleIdx;
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) {
                    int minLoadIdx = 0;
                    for (int i = 1; i < vehicleNum; i++) {
                        if (vehicleLoads.get(i) < vehicleLoads.get(minLoadIdx)) {
                            minLoadIdx = i;
                        }
                    }
                    routes.get(minLoadIdx).add(point);
                    vehicleLoads.set(minLoadIdx, vehicleLoads.get(minLoadIdx) + demand);
                }
            }
        }

        private void evaluate() {
            totalDistance = 0;
            for (int v = 0; v < vehicleNum; v++) {
                List<Integer> route = routes.get(v);
                if (route.isEmpty()) continue;
                totalDistance += distanceMatrix[0][route.get(0)];
                for (int i = 0; i < route.size() - 1; i++) {
                    totalDistance += distanceMatrix[route.get(i)][route.get(i + 1)];
                }
                totalDistance += distanceMatrix[route.get(route.size() - 1)][0];
            }
            fitness = 1.0 / (totalDistance + 0.000001);
        }

        public String getPathString(int vehicleIndex) {
            List<Integer> route = routes.get(vehicleIndex);
            if (route.isEmpty()) return "无任务";
            StringBuilder sb = new StringBuilder();
            sb.append("0[").append(NAMES[0]).append("]");
            for (int point : route) {
                sb.append(" → ").append(point).append("[").append(NAMES[point]).append("]");
            }
            sb.append(" → 0[").append(NAMES[0]).append("]");
            sb.append(" (载重:").append(vehicleLoads.get(vehicleIndex)).append("/")
                    .append(VEHICLE_CAPACITY).append("件)");
            return sb.toString();
        }

        public String getSimplePath(int vehicleIndex) {
            List<Integer> route = routes.get(vehicleIndex);
            if (route.isEmpty()) return "";
            StringBuilder sb = new StringBuilder("0");
            for (int point : route) {
                sb.append("-").append(point);
            }
            sb.append("-0");
            return sb.toString();
        }

        @Override
        public int compareTo(Chromosome other) {
            return Double.compare(other.fitness, this.fitness);
        }

        @Override
        public Chromosome clone() {
            return new Chromosome(new ArrayList<>(this.genes));
        }
    }

    private List<Chromosome> initPopulation() {
        List<Chromosome> population = new ArrayList<>();
        List<Integer> allPoints = new ArrayList<>();
        for (int i = 1; i < COORDINATES.length; i++) {
            if (demands[i] > 0) {
                allPoints.add(i);
            }
        }
        for (int i = 0; i < POP_SIZE; i++) {
            List<Integer> genes = new ArrayList<>(allPoints);
            Collections.shuffle(genes);
            population.add(new Chromosome(genes));
        }
        return population;
    }

    private Chromosome tournamentSelect(List<Chromosome> population) {
        Chromosome best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        if (random.nextDouble() > CROSSOVER_RATE) {
            return p1.fitness > p2.fitness ? p1.clone() : p2.clone();
        }
        int size = p1.genes.size();
        int start = random.nextInt(size == 0 ? 1 : size);
        int end = size == 0 ? 0 : random.nextInt(size - start) + start;
        List<Integer> childGenes = new ArrayList<>(Collections.nCopies(size, -1));
        Set<Integer> used = new HashSet<>();
        for (int i = start; i <= end && i < size; i++) {
            childGenes.set(i, p1.genes.get(i));
            used.add(p1.genes.get(i));
        }
        int idx = 0;
        for (int i = 0; i < size; i++) {
            if (childGenes.get(i) == -1) {
                while (idx < size && used.contains(p2.genes.get(idx))) {
                    idx++;
                }
                if (idx < size) {
                    childGenes.set(i, p2.genes.get(idx));
                    used.add(p2.genes.get(idx));
                    idx++;
                }
            }
        }
        return new Chromosome(childGenes);
    }

    private void mutate(Chromosome chromosome) {
        if (random.nextDouble() > MUTATION_RATE) return;
        int size = chromosome.genes.size();
        if (size < 2) return;
        int i = random.nextInt(size);
        int j = random.nextInt(size);
        Collections.swap(chromosome.genes, i, j);
        chromosome.decode();
        chromosome.evaluate();
    }

    private void localOptimize(Chromosome chromosome) {
        boolean improved = true;
        int maxIterations = 30;
        int iteration = 0;
        while (improved && iteration < maxIterations) {
            improved = false;
            iteration++;
            for (int i = 0; i < chromosome.genes.size() - 1; i++) {
                for (int j = i + 1; j < chromosome.genes.size(); j++) {
                    List<Integer> newGenes = new ArrayList<>(chromosome.genes);
                    reverseSublist(newGenes, i, j);
                    Chromosome newChromosome = new Chromosome(newGenes);
                    if (newChromosome.fitness > chromosome.fitness) {
                        chromosome.genes = new ArrayList<>(newGenes);
                        chromosome.decode();
                        chromosome.evaluate();
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }
    }

    private void reverseSublist(List<Integer> list, int start, int end) {
        while (start < end) {
            int temp = list.get(start);
            list.set(start, list.get(end));
            list.set(end, temp);
            start++;
            end--;
        }
    }

    public Map<String, Object> run() {
        List<Chromosome> population = initPopulation();
        Collections.sort(population);
        Chromosome best = population.isEmpty() ? new Chromosome(new ArrayList<>()) : population.get(0);
        for (int generation = 0; generation < GENERATIONS; generation++) {
            List<Chromosome> newPopulation = new ArrayList<>();
            int eliteCount = (int) (POP_SIZE * ELITE_RATE);
            for (int i = 0; i < eliteCount && i < population.size(); i++) {
                newPopulation.add(population.get(i).clone());
            }
            while (newPopulation.size() < POP_SIZE) {
                Chromosome p1 = tournamentSelect(population);
                Chromosome p2 = tournamentSelect(population);
                Chromosome child = crossover(p1, p2);
                mutate(child);
                if (generation % 10 == 0 && random.nextDouble() < 0.1) {
                    localOptimize(child);
                }
                newPopulation.add(child);
            }
            population = newPopulation;
            Collections.sort(population);
            if (!population.isEmpty() && population.get(0).fitness > best.fitness) {
                best = population.get(0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalDistance", best.totalDistance);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < vehicleNum; i++) {
            paths.add(best.getSimplePath(i));
        }
        result.put("paths", paths);
        return result;
    }

    public List<Integer> solveTspRoute(List<Integer> destinationIds) {
        if (destinationIds == null || destinationIds.isEmpty()) return List.of();
        List<Integer> nodes = new ArrayList<>();
        for (Integer id : destinationIds) {
            if (id == null) continue;
            int v = id;
            if (v <= 0) continue;
            if (!nodes.contains(v)) nodes.add(v);
        }
        if (nodes.isEmpty()) return List.of();
        if (nodes.size() == 1) return List.of(0, nodes.get(0), 0);

        int popSize = Math.min(140, Math.max(60, nodes.size() * 25));
        int generations = Math.min(700, 220 + nodes.size() * 60);
        double mutationRate = 0.18;
        double crossoverRate = 0.9;
        int tournamentSize = 5;
        int eliteCount = Math.max(2, (int) (popSize * 0.1));

        List<TspIndividual> population = initTspPopulation(nodes, popSize);
        population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
        TspIndividual best = population.get(0).copy();

        for (int gen = 0; gen < generations; gen++) {
            List<TspIndividual> next = new ArrayList<>();
            for (int i = 0; i < eliteCount && i < population.size(); i++) {
                next.add(population.get(i).copy());
            }

            while (next.size() < popSize) {
                TspIndividual p1 = tspTournamentSelect(population, tournamentSize);
                TspIndividual p2 = tspTournamentSelect(population, tournamentSize);
                TspIndividual child;
                if (random.nextDouble() < crossoverRate) {
                    child = tspCrossoverOx(p1, p2);
                } else {
                    child = p1.fitness >= p2.fitness ? p1.copy() : p2.copy();
                }
                tspMutateSwap(child, mutationRate);
                if (gen % 12 == 0 && random.nextDouble() < 0.2) {
                    tspTwoOpt(child, 30);
                }
                child.evaluate();
                next.add(child);
            }

            population = next;
            population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
            if (!population.isEmpty() && population.get(0).fitness > best.fitness) {
                best = population.get(0).copy();
            }
        }

        List<Integer> route = new ArrayList<>();
        route.add(0);
        route.addAll(best.perm);
        route.add(0);
        return route;
    }

    public double routeDistanceKm(List<Integer> route) {
        if (route == null || route.size() < 2) return 0;
        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            if (a < 0 || b < 0) continue;
            if (a >= distanceMatrix.length || b >= distanceMatrix.length) continue;
            total += distanceMatrix[a][b];
        }
        return total / 1000.0;
    }

    private class TspIndividual {
        List<Integer> perm;
        double distance;
        double fitness;

        TspIndividual(List<Integer> perm) {
            this.perm = perm;
        }

        void evaluate() {
            double dist = 0;
            if (!perm.isEmpty()) {
                dist += distanceMatrix[0][perm.get(0)];
                for (int i = 0; i < perm.size() - 1; i++) {
                    dist += distanceMatrix[perm.get(i)][perm.get(i + 1)];
                }
                dist += distanceMatrix[perm.get(perm.size() - 1)][0];
            }
            this.distance = dist;
            this.fitness = 1.0 / (dist + 0.000001);
        }

        TspIndividual copy() {
            TspIndividual c = new TspIndividual(new ArrayList<>(perm));
            c.distance = distance;
            c.fitness = fitness;
            return c;
        }
    }

    private List<TspIndividual> initTspPopulation(List<Integer> nodes, int popSize) {
        List<TspIndividual> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            List<Integer> perm = new ArrayList<>(nodes);
            Collections.shuffle(perm, random);
            TspIndividual ind = new TspIndividual(perm);
            ind.evaluate();
            population.add(ind);
        }
        return population;
    }

    private TspIndividual tspTournamentSelect(List<TspIndividual> population, int size) {
        TspIndividual best = null;
        for (int i = 0; i < size; i++) {
            TspIndividual candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private TspIndividual tspCrossoverOx(TspIndividual p1, TspIndividual p2) {
        int n = p1.perm.size();
        if (n == 0) return p1.copy();
        int start = random.nextInt(n);
        int end = random.nextInt(n - start) + start;
        List<Integer> child = new ArrayList<>(Collections.nCopies(n, -1));
        Set<Integer> used = new HashSet<>();
        for (int i = start; i <= end; i++) {
            int v = p1.perm.get(i);
            child.set(i, v);
            used.add(v);
        }
        int idx = 0;
        for (int i = 0; i < n; i++) {
            if (child.get(i) != -1) continue;
            while (idx < n && used.contains(p2.perm.get(idx))) {
                idx++;
            }
            if (idx < n) {
                child.set(i, p2.perm.get(idx));
                used.add(p2.perm.get(idx));
                idx++;
            }
        }
        TspIndividual c = new TspIndividual(child);
        return c;
    }

    private void tspMutateSwap(TspIndividual ind, double mutationRate) {
        if (random.nextDouble() > mutationRate) return;
        int n = ind.perm.size();
        if (n < 2) return;
        int i = random.nextInt(n);
        int j = random.nextInt(n);
        Collections.swap(ind.perm, i, j);
    }

    private void tspTwoOpt(TspIndividual ind, int maxIterations) {
        int n = ind.perm.size();
        if (n < 4) return;
        int iter = 0;
        boolean improved = true;
        while (improved && iter < maxIterations) {
            improved = false;
            iter++;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    int a = (i == 0) ? 0 : ind.perm.get(i - 1);
                    int b = ind.perm.get(i);
                    int c = ind.perm.get(j);
                    int d = (j == n - 1) ? 0 : ind.perm.get(j + 1);
                    double before = distanceMatrix[a][b] + distanceMatrix[c][d];
                    double after = distanceMatrix[a][c] + distanceMatrix[b][d];
                    if (after + 1e-6 < before) {
                        reverseSublist(ind.perm, i, j);
                        improved = true;
                    }
                }
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
