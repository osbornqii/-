<template>
  <el-card>
    <div style="display:flex; align-items:center; gap:8px; margin-bottom:12px">
      <el-icon color="#36a3e0"><Search /></el-icon>
      <span style="font-weight:600">订单查询</span>
    </div>
    <div style="display:flex; gap:8px; margin-bottom:12px; flex-wrap:wrap">
      <el-input v-model="phone" placeholder="请输入手机号" style="max-width:260px" disabled />
      <el-button type="primary" @click="query">查询我的订单</el-button>
    </div>
    <el-table :data="orders" size="small" border>
      <el-table-column prop="orderNumber" label="订单号" />
      <el-table-column prop="user" label="收件人" />
      <el-table-column prop="userContact" label="手机号" />
      <el-table-column prop="destination" label="目的地" />
      <el-table-column prop="pickupCode" label="取件码" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="orderStatusTagType(row.orderStatus)">{{ orderStatusText(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.orderStatus === 'PENDING'"
            type="danger"
            size="small"
            plain
            @click="cancelOrder(row)"
          >
            取消订单
          </el-button>
          <el-button
            v-if="row.orderStatus === 'DELIVERED' || row.orderStatus === 'CANCELLED'"
            type="danger"
            size="small"
            plain
            @click="deleteOrder(row)"
          >
            删除记录
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="orderTime" label="下单时间" />
      <el-table-column prop="expectedDeliveryTime" label="期望送达" />
    </el-table>
    <div style="margin-top:12px">{{ message }}</div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { Search } from "@element-plus/icons-vue";
import { ElMessageBox, ElMessage } from "element-plus";
import http from "../api/http";
import { useAuthStore } from "../stores/auth";
import { orderStatusTagType, orderStatusText } from "../utils/status";

const auth = useAuthStore();
const phone = ref(auth.session?.phone || "");
const orders = ref([]);
const message = ref("");

onMounted(() => {
  if (phone.value) {
    query();
  }
});

async function query() {
  message.value = "查询中...";
  orders.value = [];
  try {
    const res = await http.get("/api/order/query", { params: { phone: phone.value } });
    orders.value = Array.isArray(res.data) ? res.data : [];
    message.value = `共 ${orders.value.length} 条结果`;
  } catch (e) {
    message.value = e?.response?.data?.message || e?.message || "查询失败";
  }
}

async function cancelOrder(row) {
  try {
    await ElMessageBox.confirm("费用将会退回您的账户", "确定要取消该订单吗？", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    });
    await http.patch(`/api/orders/${row.id}/status`, { orderStatus: "CANCELLED" });
    ElMessage.success("订单已取消");
    await query();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.response?.data?.message || e?.message || "操作失败");
    }
  }
}

async function deleteOrder(row) {
  try {
    await ElMessageBox.confirm("确定要删除该订单记录吗？删除后将无法恢复。", "删除提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    });
    await http.delete(`/api/orders/${row.id}`);
    ElMessage.success("订单记录已删除");
    await query();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error(e?.response?.data?.message || e?.message || "删除失败");
    }
  }
}
</script>
