<template>
  <div :style="pageStyle">
    <div style="width:100%; max-width:560px">
      <el-card style="box-shadow: 0 12px 40px rgba(31,45,61,0.12); background: rgba(255,255,255,0.92)">
        <div style="display:flex; align-items:center; gap:8px; margin-bottom:14px">
          <el-icon color="#36a3e0"><Promotion /></el-icon>
          <span style="font-weight:700">校园无人配送下单</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" label-position="left">
          <el-form-item label="姓名" prop="user">
            <el-input v-model="form.user" placeholder="请输入姓名" clearable />
          </el-form-item>
          <el-form-item label="手机号" prop="userContact">
            <el-input v-model="form.userContact" placeholder="请输入11位手机号" maxlength="11" disabled />
          </el-form-item>
          <el-form-item label="取件码" prop="pickupCode">
            <el-input v-model="form.pickupCode" placeholder="请输入取件码" clearable />
          </el-form-item>
          <el-form-item label="配送点" prop="destination">
            <el-select v-model="form.destination" placeholder="请选择配送点" filterable style="width:100%">
              <el-option v-for="b in buildings" :key="b" :label="b" :value="b" />
            </el-select>
          </el-form-item>
          <el-form-item label="配送时段" prop="deliverySlot">
            <el-select v-model="form.deliverySlot" placeholder="请选择时段" style="width:100%">
              <el-option
                v-for="t in timeSlots"
                :key="t"
                :label="t"
                :value="t"
                :disabled="isSlotDisabled(t)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="配送费用">
            <span style="font-weight:700; color:#f56c6c; font-size:18px">￥1.50</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">提交并支付</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>

        <div v-if="result" style="margin-top:12px; color:#2c3e50">{{ result }}</div>
      </el-card>
    </div>

    <!-- 模拟支付弹窗 -->
    <el-dialog v-model="paymentVisible" title="订单支付" width="360px" center>
      <div style="text-align:center">
        <div style="font-size:14px; color:#606266; margin-bottom:8px">支付金额</div>
        <div style="font-size:32px; font-weight:700; color:#303133; margin-bottom:24px">￥1.50</div>
        
        <div style="display:flex; flex-direction:column; gap:12px; margin-bottom:20px">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px; border:1px solid #e4e7ed; border-radius:8px; cursor:pointer" @click="paymentMethod = 'wechat'">
            <div style="display:flex; align-items:center; gap:8px">
              <el-icon color="#67c23a" size="20"><CircleCheck v-if="paymentMethod === 'wechat'" /><ChatDotRound v-else /></el-icon>
              <span>微信支付</span>
            </div>
            <el-radio v-model="paymentMethod" label="wechat"><span></span></el-radio>
          </div>
          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px; border:1px solid #e4e7ed; border-radius:8px; cursor:pointer" @click="paymentMethod = 'alipay'">
            <div style="display:flex; align-items:center; gap:8px">
              <el-icon color="#409eff" size="20"><CircleCheck v-if="paymentMethod === 'alipay'" /><Wallet v-else /></el-icon>
              <span>支付宝支付</span>
            </div>
            <el-radio v-model="paymentMethod" label="alipay"><span></span></el-radio>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="paymentVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmPayment">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessageBox, ElMessage } from "element-plus";
import { Promotion, Wallet, ChatDotRound, CircleCheck } from "@element-plus/icons-vue";
import http from "../api/http";
import { useAuthStore } from "../stores/auth";
import { orderStatusText } from "../utils/status";

const auth = useAuthStore();
const buildings = [
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
];

const timeSlots = [
  "09:00~10:00", "10:00~11:00", "11:00~12:00", "12:00~13:00",
  "13:00~14:00", "14:00~15:00", "15:00~16:00", "16:00~17:00",
  "17:00~18:00", "18:00~19:00", "19:00~20:00", "20:00~21:00"
];

const formRef = ref();
const submitting = ref(false);
const paymentVisible = ref(false);
const paymentMethod = ref("wechat");
const form = reactive({
  user: auth.session?.name || "",
  userContact: auth.session?.phone || "",
  pickupCode: "",
  destination: "",
  deliverySlot: ""
});

onMounted(() => {
  if (auth.session) {
    form.user = auth.session.name || "";
    form.userContact = auth.session.phone || "";
  }
});
const result = ref("");

const rules = {
  user: [{ required: true, message: "姓名不能为空", trigger: "blur" }],
  userContact: [
    { required: true, message: "手机号不能为空", trigger: "blur" },
    { pattern: /^\d{11}$/, message: "手机号必须为11位数字", trigger: "blur" }
  ],
  pickupCode: [{ required: true, message: "取件码不能为空", trigger: "blur" }],
  destination: [{ required: true, message: "请选择配送点", trigger: "change" }],
  deliverySlot: [{ required: true, message: "请选择配送时段", trigger: "change" }]
};

const pageStyle = {
  minHeight: "calc(100vh - 32px)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "16px",
  backgroundImage: "linear-gradient(180deg, rgba(232,245,255,0.9) 0%, rgba(245,255,244,0.9) 100%)"
};

function toLocalDateTimeForSlot(slot) {
  if (!slot) return null;
  const startPart = String(slot).split("~")[0];
  const [h, m] = startPart.split(":").map(v => Number(v));
  const d = new Date();
  d.setHours(Number.isFinite(h) ? h : 0, Number.isFinite(m) ? m : 0, 0, 0);
  const pad = n => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function isSlotDisabled(slot) {
  if (!slot) return false;
  // 提取时段开始时间，例如 "09:00~10:00" 提取 "09:00"
  const startPart = String(slot).split("~")[0];
  const [h, m] = startPart.split(":").map(v => Number(v));
  
  const now = new Date();
  const slotStart = new Date();
  slotStart.setHours(h, m, 0, 0);
  
  // 如果当前时间已经超过了时段的开始时间，则禁用
  return now > slotStart;
}

function reset() {
  form.pickupCode = "";
  form.destination = "";
  form.deliverySlot = "";
  result.value = "";
  formRef.value?.clearValidate?.();
}

async function submit() {
  if (submitting.value) return;
  const ok = await formRef.value?.validate?.().catch(() => false);
  if (!ok) return;
  
  // 显示支付弹窗
  paymentVisible.value = true;
}

async function confirmPayment() {
  submitting.value = true;
  try {
    const payload = {
      user: form.user,
      userContact: form.userContact,
      pickupCode: form.pickupCode,
      destination: form.destination,
      expectedDeliveryTime: toLocalDateTimeForSlot(form.deliverySlot)
    };
    
    // 模拟支付延迟
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    const res = await http.post("/api/order/submit", payload);
    const statusCode = res.data?.orderStatus ?? "";
    const orderNumber = res.data?.orderNumber ?? "";
    const statusText = orderStatusText(statusCode);
    
    paymentVisible.value = false;
    result.value = `订单号：${orderNumber}，状态：${statusText}`;
    
    ElMessage.success("支付成功！");
    await ElMessageBox.alert(`订单提交成功并已支付\n订单号：${orderNumber}\n状态：${statusText}`, "支付成功", {
      confirmButtonText: "好的",
      type: "success"
    });
    reset();
  } catch (e) {
    result.value = e?.response?.data?.message || e?.message || "提交失败";
    await ElMessageBox.alert(String(result.value), "提交失败", { confirmButtonText: "知道了", type: "error" });
  } finally {
    submitting.value = false;
  }
}
</script>
