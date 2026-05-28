<template>
  <el-card>
    <div style="display:flex; align-items:center; gap:8px; margin-bottom:12px">
      <el-icon color="#36a3e0"><Van /></el-icon>
      <span style="font-weight:600">车辆管理</span>
      <el-button type="primary" style="margin-left:auto" @click="load">刷新列表</el-button>
    </div>
    <el-table :data="vehicles" size="small" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="vehicleNumber" label="车辆编号" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="vehicleStatusTagType(row.vehicleStatus)">{{ vehicleStatusText(row.vehicleStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="history" label="历史" />
    </el-table>

    <el-divider />
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-card class="form-card">
          <div class="form-title">新增车辆</div>
          <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
            <el-form-item label="车辆编号" prop="vehicleNumber">
              <el-input v-model="createForm.vehicleNumber" placeholder="例如：V-001" clearable />
            </el-form-item>
            <el-form-item label="车辆状态" prop="vehicleStatus">
              <el-select v-model="createForm.vehicleStatus" placeholder="请选择状态" style="width:200px">
                <el-option label="空闲" value="IDLE" />
                <el-option label="任务中" value="BUSY" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
            <el-form-item label="历史" prop="history">
              <el-input v-model="createForm.history" type="textarea" :rows="3" placeholder="可选：车辆维护/任务记录" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="creating" @click="createVehicle">添加</el-button>
              <el-button @click="resetCreate">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card class="form-card">
          <div class="form-title">更新车辆状态</div>
          <el-form ref="updateFormRef" :model="updateForm" :rules="updateRules" label-width="100px" @submit.prevent>
            <el-form-item label="车辆编号" prop="vehicleNumber">
              <el-select v-model="updateForm.vehicleNumber" filterable placeholder="请选择车辆" style="width:100%">
                <el-option v-for="v in vehicles" :key="v.id" :label="v.vehicleNumber" :value="v.vehicleNumber" />
              </el-select>
            </el-form-item>
            <el-form-item label="车辆状态" prop="vehicleStatus">
              <el-select v-model="updateForm.vehicleStatus" placeholder="请选择状态" style="width:200px">
                <el-option label="空闲" value="IDLE" />
                <el-option label="任务中" value="BUSY" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="updating" @click="updateStatus">更新</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    <div style="margin-top:12px">{{ message }}</div>
  </el-card>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Van } from "@element-plus/icons-vue";
import http from "../api/http";
import { vehicleStatusTagType, vehicleStatusText } from "../utils/status";

const vehicles = ref([]);
const message = ref("");

const createFormRef = ref();
const updateFormRef = ref();
const creating = ref(false);
const updating = ref(false);

const createForm = reactive({
  vehicleNumber: "",
  vehicleStatus: "IDLE",
  history: ""
});

const updateForm = reactive({
  vehicleNumber: "",
  vehicleStatus: "IDLE"
});

const createRules = {
  vehicleNumber: [{ required: true, message: "车辆编号不能为空", trigger: "blur" }],
  vehicleStatus: [{ required: true, message: "请选择车辆状态", trigger: "change" }]
};

const updateRules = {
  vehicleNumber: [{ required: true, message: "车辆编号不能为空", trigger: "blur" }],
  vehicleStatus: [{ required: true, message: "请选择车辆状态", trigger: "change" }]
};

async function load() {
  message.value = "加载中...";
  try {
    const res = await http.get("/api/vehicle/list");
    vehicles.value = Array.isArray(res.data) ? res.data : [];
    message.value = `共 ${vehicles.value.length} 辆`;
    if (!updateForm.vehicleNumber && vehicles.value.length) {
      updateForm.vehicleNumber = vehicles.value[0].vehicleNumber;
    }
  } catch (e) {
    message.value = e?.response?.data?.message || e?.message || "加载失败";
  }
}

function resetCreate() {
  createForm.vehicleNumber = "";
  createForm.vehicleStatus = "IDLE";
  createForm.history = "";
  createFormRef.value?.clearValidate?.();
}

async function createVehicle() {
  if (creating.value) return;
  const ok = await createFormRef.value?.validate?.().catch(() => false);
  if (!ok) return;
  creating.value = true;
  try {
    await http.post("/api/vehicles", {
      vehicleNumber: createForm.vehicleNumber,
      vehicleStatus: createForm.vehicleStatus,
      history: createForm.history || null
    });
    ElMessage.success("添加成功");
    resetCreate();
    await load();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "添加失败");
  } finally {
    creating.value = false;
  }
}

async function updateStatus() {
  if (updating.value) return;
  const ok = await updateFormRef.value?.validate?.().catch(() => false);
  if (!ok) return;
  updating.value = true;
  try {
    const payload = { vehicleNumber: updateForm.vehicleNumber, vehicleStatus: updateForm.vehicleStatus };
    await http.put("/api/vehicle/status", payload);
    await load();
    ElMessage.success("更新成功");
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "更新失败");
  } finally {
    updating.value = false;
  }
}
</script>

<style scoped>
.form-card {
  height: 100%;
  box-shadow: 0 10px 30px rgba(31, 45, 61, 0.08);
}

.form-title {
  font-weight: 700;
  margin-bottom: 10px;
  color: #1f2d3d;
}
</style>
