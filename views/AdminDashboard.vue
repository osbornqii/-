<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="card" @click="go('/admin/orders')">
          <div class="card-title">
            <el-icon color="#36a3e0" size="22"><List /></el-icon>
            <span>订单管理</span>
          </div>
          <div class="card-sub">查看订单列表、更新状态</div>
          <div class="stats">
            <div class="stat">
              <div class="stat-label">今日订单数</div>
              <div class="stat-value">{{ stats.todayOrders }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">待配送数</div>
              <div class="stat-value">{{ stats.pendingOrders }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="card" @click="go('/admin/vehicles')">
          <div class="card-title">
            <el-icon color="#ffce54" size="22"><Van /></el-icon>
            <span>车辆管理</span>
          </div>
          <div class="card-sub">查看车辆状态、更新调度</div>
          <div class="stats">
            <div class="stat">
              <div class="stat-label">可用车辆数</div>
              <div class="stat-value">{{ stats.idleVehicles }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">配送中车辆数</div>
              <div class="stat-value">{{ stats.busyVehicles }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="card" @click="go('/admin/path')">
          <div class="card-title">
            <el-icon color="#34c38f" size="22"><MapLocation /></el-icon>
            <span>路径规划</span>
          </div>
          <div class="card-sub">计算配送路线并可视化</div>
          <div class="stats">
            <div class="stat">
              <div class="stat-label">已规划路线数</div>
              <div class="stat-value">{{ stats.plannedRoutes }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="hint">
      <el-button text type="primary" :loading="loading" @click="loadStats">刷新统计</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { List, MapLocation, Van } from "@element-plus/icons-vue";
import http from "../api/http";

const router = useRouter();
const loading = ref(false);
const stats = reactive({
  todayOrders: 0,
  pendingOrders: 0,
  idleVehicles: 0,
  busyVehicles: 0,
  plannedRoutes: 0
});

function go(path) {
  router.push(path);
}

function isToday(value) {
  if (!value) return false;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return false;
  const now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
}

async function loadStats() {
  if (loading.value) return;
  loading.value = true;
  try {
    const [ordersRes, vehiclesRes, pathsRes] = await Promise.all([
      http.get("/api/order/list"),
      http.get("/api/vehicle/list"),
      http.get("/api/paths")
    ]);

    const orders = Array.isArray(ordersRes.data) ? ordersRes.data : [];
    const vehicles = Array.isArray(vehiclesRes.data) ? vehiclesRes.data : [];
    const paths = Array.isArray(pathsRes.data) ? pathsRes.data : [];

    stats.todayOrders = orders.filter(o => isToday(o.orderTime)).length;
    stats.pendingOrders = orders.filter(o => o.orderStatus === "NEW").length;
    stats.idleVehicles = vehicles.filter(v => v.vehicleStatus === "IDLE").length;
    stats.busyVehicles = vehicles.filter(v => v.vehicleStatus === "BUSY").length;
    stats.plannedRoutes = paths.length;
  } catch {
    stats.todayOrders = 0;
    stats.pendingOrders = 0;
    stats.idleVehicles = 0;
    stats.busyVehicles = 0;
    stats.plannedRoutes = 0;
  } finally {
    loading.value = false;
  }
}

onMounted(loadStats);
</script>

<style scoped>
.page {
  background: #f3f5f7;
  padding: 16px;
  border-radius: 12px;
}

.card {
  cursor: pointer;
  background: #fff;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.card:hover {
  transform: scale(1.03);
  box-shadow: 0 14px 40px rgba(31, 45, 61, 0.14);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
}

.card-sub {
  color: #667;
  margin-top: 6px;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
}

.stat {
  padding: 10px 12px;
  background: rgba(54, 163, 224, 0.08);
  border-radius: 10px;
}

.stat-label {
  color: #667;
  font-size: 12px;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #1f2d3d;
  margin-top: 4px;
}

.hint {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .stats {
    grid-template-columns: 1fr;
  }
}
</style>
