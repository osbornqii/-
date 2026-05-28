<template>
  <el-card class="compact-card">
    <div style="display:flex; align-items:center; gap:12px; margin-bottom:12px; flex-wrap:wrap">
      <el-icon color="#36a3e0"><List /></el-icon>
      <span style="font-weight:600">订单管理</span>
      
      <div style="display:flex; align-items:center; gap:8px; margin-left:24px">
        <span style="font-size:14px; color:#606266">预计配送日期:</span>
        <el-date-picker
          v-model="queryDate"
          type="date"
          placeholder="选择日期"
          size="small"
          style="width:140px"
          @change="load"
        />
      </div>

      <el-button type="primary" style="margin-left:auto" @click="load">刷新列表</el-button>
    </div>

    <div v-for="(group, slot) in groupedOrders" :key="slot" style="margin-bottom:24px">
      <div style="background:#f5f7fa; padding:8px 12px; border-left:4px solid #36a3e0; margin-bottom:8px; display:flex; align-items:center; justify-content:space-between">
        <span style="font-weight:700; color:#303133">配送时段：{{ slot }}</span>
        <el-tag size="small" type="info">{{ group.length }} 个订单</el-tag>
      </div>
      
      <el-table :data="group" size="small" border>
        <el-table-column prop="id" label="ID" width="50" />
        <el-table-column prop="orderNumber" label="订单号" width="110" show-overflow-tooltip />
        <el-table-column label="下单时间" width="110">
          <template #default="{ row }">
            {{ formatDateTime(row.orderTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="user" label="收件人" width="80" show-overflow-tooltip />
        <el-table-column prop="userContact" label="手机号" width="110" show-overflow-tooltip />
        <el-table-column prop="destination" label="目的地" min-width="100" show-overflow-tooltip />
        <el-table-column label="分配车辆" width="120">
          <template #default="{ row }">
            <el-select
              v-model="assignMap[row.id].vehicleNumber"
              filterable
              clearable
              placeholder="选择车辆"
              size="small"
              style="width:100%"
              :loading="assignMap[row.id].loading"
              :disabled="row.orderStatus === 'CANCELLED' || row.orderStatus === 'DELIVERED'"
              @change="assignVehicleRow(row.id)"
            >
              <el-option
                v-for="v in vehicles"
                :key="v.id"
                :label="`${v.vehicleNumber}`"
                :value="v.vehicleNumber"
                :disabled="v.vehicleStatus === 'OFFLINE'"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="包裹重量 & 尺寸(L*W*H)" width="280">
          <template #default="{ row }">
            <div style="display:flex; gap:4px; align-items:center">
              <el-input
                v-model="packageMap[row.id].weightKg"
                size="small"
                placeholder="重"
                inputmode="decimal"
                style="width:55px"
                :disabled="row.orderStatus === 'CANCELLED' || row.orderStatus === 'DISPATCHED' || row.orderStatus === 'DELIVERED'"
                @input="markPackageDirty(row.id)"
              />
              <span style="color:#909399; scale:0.8">kg |</span>
              <el-input
                v-model="packageMap[row.id].lengthCm"
                size="small"
                placeholder="长"
                inputmode="decimal"
                style="width:45px"
                :disabled="row.orderStatus === 'CANCELLED' || row.orderStatus === 'DISPATCHED' || row.orderStatus === 'DELIVERED'"
                @input="markPackageDirty(row.id)"
              />
              <span style="color:#909399">*</span>
              <el-input
                v-model="packageMap[row.id].widthCm"
                size="small"
                placeholder="宽"
                inputmode="decimal"
                style="width:45px"
                :disabled="row.orderStatus === 'CANCELLED' || row.orderStatus === 'DISPATCHED' || row.orderStatus === 'DELIVERED'"
                @input="markPackageDirty(row.id)"
              />
              <span style="color:#909399">*</span>
              <el-input
                v-model="packageMap[row.id].heightCm"
                size="small"
                placeholder="高"
                inputmode="decimal"
                style="width:45px"
                :disabled="row.orderStatus === 'CANCELLED' || row.orderStatus === 'DISPATCHED' || row.orderStatus === 'DELIVERED'"
                @input="markPackageDirty(row.id)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="orderStatusTagType(row.orderStatus)" size="small">{{ orderStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <div style="display: flex; gap: 4px;">
              <el-button
                type="primary"
                size="small"
                plain
                :loading="packageMap[row.id].saving"
                :disabled="!packageMap[row.id].dirty || row.orderStatus === 'CANCELLED' || row.orderStatus === 'DISPATCHED' || row.orderStatus === 'DELIVERED'"
                @click="savePackageRow(row.id)"
              >
                保存
              </el-button>
              <el-button
                v-if="row.orderStatus === 'DELIVERED' || row.orderStatus === 'CANCELLED'"
                type="danger"
                size="small"
                plain
                @click="deleteOrder(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="Object.keys(groupedOrders).length === 0" style="padding:40px; text-align:center; color:#909399">
      暂无订单数据
    </div>

    <div style="margin-top:12px; color:#606266; font-size:14px">{{ message }}</div>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { List } from "@element-plus/icons-vue";
import http from "../api/http";
import { orderStatusTagType, orderStatusText, vehicleStatusText } from "../utils/status";

const orders = ref([]);
const vehicles = ref([]);
const statusMap = reactive({});
const message = ref("");
const queryDate = ref(new Date());

const groupedOrders = computed(() => {
  const groups = {};
  
  // 过滤逻辑
  let filtered = [];
  if (queryDate.value) {
    // 如果选择了具体日期，严格只显示该日期的订单
    const qd = new Date(queryDate.value);
    const dateStr = `${qd.getFullYear()}-${String(qd.getMonth() + 1).padStart(2, "0")}-${String(qd.getDate()).padStart(2, "0")}`;
    filtered = orders.value.filter(o => formatOnlyDate(o.expectedDeliveryTime) === dateStr);
  } else {
    // 如果没有选择具体日期，则只显示活跃订单（排除已送达和已取消）
    filtered = orders.value.filter(o => o.orderStatus !== 'DELIVERED' && o.orderStatus !== 'CANCELLED');
  }

  const sorted = filtered.sort((a, b) => {
    const timeA = a.expectedDeliveryTime || "";
    const timeB = b.expectedDeliveryTime || "";
    return timeA.localeCompare(timeB);
  });

  sorted.forEach(order => {
    // 格式化分组名称：如果没有选择具体日期，显示日期+时段；如果已选日期，仅显示时段。
    const dStr = formatOnlyDate(order.expectedDeliveryTime);
    const timeStr = formatTime(order.expectedDeliveryTime);
    const slotKey = queryDate.value ? timeStr : `${dStr} ${timeStr}`;

    if (!groups[slotKey]) {
      groups[slotKey] = [];
    }
    groups[slotKey].push(order);
  });
  return groups;
});

function formatOnlyDate(val) {
  if (!val) return "-";
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return val;
  const pad = n => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function formatDateTime(val) {
  if (!val) return "-";
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return val;
  const pad = n => String(n).padStart(2, "0");
  const m = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const h = pad(d.getHours());
  const min = pad(d.getMinutes());
  return `${m}-${day} ${h}:${min}`;
}

function formatTime(val) {
  if (!val) return "-";
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) return val;
  const pad = n => String(n).padStart(2, "0");
  const start = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const d2 = new Date(d.getTime() + 60 * 60 * 1000);
  const end = `${pad(d2.getHours())}:${pad(d2.getMinutes())}`;
  return `${start}~${end}`;
}

const packageMap = reactive({});
const assignMap = reactive({});

function ensurePackageRow(order) {
  const id = order?.id;
  if (id == null) return;
  if (!packageMap[id]) {
    packageMap[id] = reactive({
      weightKg: "",
      lengthCm: "",
      widthCm: "",
      heightCm: "",
      dirty: false,
      saving: false
    });
  }
  packageMap[id].weightKg = order?.weightKg == null ? "" : String(order.weightKg);
  packageMap[id].lengthCm = order?.lengthCm == null ? "" : String(order.lengthCm);
  packageMap[id].widthCm = order?.widthCm == null ? "" : String(order.widthCm);
  packageMap[id].heightCm = order?.heightCm == null ? "" : String(order.heightCm);
  packageMap[id].dirty = false;
  packageMap[id].saving = false;
}

function ensureAssignRow(order) {
  const id = order?.id;
  if (id == null) return;
  if (!assignMap[id]) {
    assignMap[id] = reactive({
      vehicleNumber: "",
      loading: false
    });
  }
  assignMap[id].vehicleNumber = order?.assignedVehicleNumber ?? "";
  assignMap[id].loading = false;
}

async function loadVehicles() {
  try {
    const res = await http.get("/api/vehicle/list");
    vehicles.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    vehicles.value = [];
  }
}

function markPackageDirty(id) {
  if (!packageMap[id]) return;
  packageMap[id].dirty = true;
}

async function load() {
  message.value = "加载中...";
  try {
    const params = {};
    if (queryDate.value) {
      const d = new Date(queryDate.value);
      // 设置为当天的 00:00:00 到 23:59:59
      const start = new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0);
      const end = new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59);
      
      const pad = n => String(n).padStart(2, "0");
      const formatIso = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
      
      params.start = formatIso(start);
      params.end = formatIso(end);
    }

    const [ordersRes] = await Promise.all([
      http.get("/api/orders", { params }),
      loadVehicles()
    ]);
    orders.value = Array.isArray(ordersRes.data) ? ordersRes.data : [];
    orders.value.forEach(o => (statusMap[o.id] = o.orderStatus));
    orders.value.forEach(o => ensurePackageRow(o));
    orders.value.forEach(o => ensureAssignRow(o));
    message.value = `共 ${orders.value.length} 条`;
  } catch (e) {
    message.value = e?.response?.data?.message || e?.message || "加载失败";
  }
}

async function updateStatus(id) {
  try {
    const payload = { orderStatus: statusMap[id] || "PENDING" };
    const res = await http.patch(`/api/orders/${id}/status`, payload);
    const idx = orders.value.findIndex(o => o.id === id);
    if (idx >= 0) {
      orders.value[idx] = res.data;
      statusMap[id] = res.data.orderStatus;
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "更新失败");
  }
}

async function savePackageRow(id) {
  if (!packageMap[id] || packageMap[id].saving) return;

  const toNumberOrNull = v => {
    const s = String(v ?? "").trim();
    if (!s) return null;
    const n = Number(s);
    if (!Number.isFinite(n)) return NaN;
    return n;
  };

  const payload = {
    weightKg: toNumberOrNull(packageMap[id].weightKg),
    lengthCm: toNumberOrNull(packageMap[id].lengthCm),
    widthCm: toNumberOrNull(packageMap[id].widthCm),
    heightCm: toNumberOrNull(packageMap[id].heightCm)
  };
  if (Object.values(payload).some(v => Number.isNaN(v))) {
    ElMessage.warning("请输入合法数字");
    return;
  }
  const hasAny = Object.values(payload).some(v => v != null);
  if (!hasAny) {
    ElMessage.warning("请至少填写一个字段");
    return;
  }

  packageMap[id].saving = true;
  try {
    const res = await http.patch(`/api/orders/${id}/package`, payload);
    const idx = orders.value.findIndex(o => o.id === id);
    if (idx >= 0) orders.value[idx] = res.data;
    ensurePackageRow(res.data);
    ElMessage.success("保存成功");
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "保存失败");
  } finally {
    packageMap[id].saving = false;
  }
}

async function assignVehicleRow(id) {
  if (!assignMap[id] || assignMap[id].loading) return;

  const order = orders.value.find(o => o.id === id);
  if (!order) return;

  // 校验包裹信息是否已录入
  if (!order.weightKg || !order.lengthCm || !order.widthCm || !order.heightCm) {
    ElMessage.warning("请先录入完整的包裹信息（重量、长、宽、高）并保存");
    ensureAssignRow(order);
    return;
  }

  const vehicleNumber = assignMap[id].vehicleNumber || null;
  if (vehicleNumber) {
    // 前端预校验：检查该时段该车是否已满 (15kg)
    const slot = order.expectedDeliveryTime;
    const occupied = orders.value
      .filter(o => o.id !== id && o.assignedVehicleNumber === vehicleNumber && o.expectedDeliveryTime === slot)
      .reduce((sum, o) => sum + (Number(o.weightKg) || 0), 0);
    
    if (occupied + Number(order.weightKg) > 15) {
      ElMessage.warning("该车已满");
      ensureAssignRow(order);
      return;
    }
  }

  assignMap[id].loading = true;
  try {
    const res = await http.patch(`/api/orders/${id}/vehicle`, { vehicleNumber });
    const idx = orders.value.findIndex(o => o.id === id);
    if (idx >= 0) orders.value[idx] = res.data;
    ensureAssignRow(res.data);
    await loadVehicles();
    ElMessage.success("分配成功");
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "分配失败");
    const current = orders.value.find(o => o.id === id);
    if (current) ensureAssignRow(current);
  } finally {
    assignMap[id].loading = false;
  }
}

async function deleteOrder(id) {
  try {
    await ElMessageBox.confirm("确定要删除该订单记录吗？删除后将无法恢复。", "删除提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    });
    await http.delete(`/api/orders/${id}`);
    ElMessage.success("订单已删除");
    await load();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.response?.data?.message || e?.message || "删除失败");
    }
  }
}

onMounted(load);
</script>

<style scoped>
.compact-card :deep(.el-card__body) {
  padding: 10px;
}

:deep(.el-table .cell) {
  padding-left: 4px;
  padding-right: 4px;
}

:deep(.el-table--small .cell) {
  font-size: 12px;
}
</style>
