<template>
  <div :style="pageStyle">
    <div style="width:100%; max-width:520px">
      <div style="display:flex; align-items:center; justify-content:center; gap:12px; margin-bottom:14px">
        <img :src="logoUrl" alt="logo" style="width:64px; height:64px; object-fit:contain; border-radius:12px; background: rgba(255,255,255,0.6); padding:6px" />
        <div style="font-size:20px; font-weight:700; color:#1f2d3d; text-shadow: 0 2px 10px rgba(255,255,255,0.9)">校园无人车路径规划系统</div>
      </div>

      <el-card style="background: rgba(255,255,255,0.9)">
        <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px">
          <el-icon color="#36a3e0" size="22"><Van /></el-icon>
          <div style="font-weight:600; font-size:16px">登录 / 注册</div>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="用户" name="user">
            <el-segmented v-model="userMode" :options="modeOptions" style="margin-bottom:12px" />
            <el-form label-width="90px" :rules="userRules" ref="userForm" :model="userFormData">
              <el-form-item v-if="userMode === 'register'" label="昵称" prop="username">
                <el-input v-model="userFormData.username" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userFormData.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="userFormData.password" type="password" placeholder="请输入密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitUser">{{ userMode === 'register' ? '注册并登录' : '登录' }}</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="管理员" name="admin">
            <el-segmented v-model="adminMode" :options="modeOptions" style="margin-bottom:12px" />
            <el-form label-width="90px" :rules="adminRules" ref="adminForm" :model="adminFormData">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="adminFormData.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="adminFormData.password" type="password" placeholder="请输入密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitAdmin">{{ adminMode === 'register' ? '注册并登录' : '登录' }}</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <el-alert v-if="message" :title="message" type="info" show-icon :closable="false" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import http from "../api/http";
import { useAuthStore } from "../stores/auth";
import { Van } from "@element-plus/icons-vue";
import logoUrl from "../assets/logo.png";
import bgUrl from "../assets/login-bg.png";

const router = useRouter();
const auth = useAuthStore();

const pageStyle = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "16px",
  backgroundColor: "rgba(232, 245, 255, 0.4)",
  backdropFilter: "blur(4px)"
};

const activeTab = ref("user");
const modeOptions = [
  { label: "登录", value: "login" },
  { label: "注册", value: "register" }
];

const userMode = ref("login");
const adminMode = ref("login");
const message = ref("");

// 表单引用
const userForm = ref(null);
const adminForm = ref(null);

// 表单数据
const userFormData = reactive({
  username: "",
  phone: "",
  password: ""
});

const adminFormData = reactive({
  username: "",
  password: ""
});

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
};

const adminRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
};

async function submitUser() {
  try {
    // 验证表单
    await userForm.value.validate();
    
    message.value = "处理中...";
    
    if (userMode.value === "register") {
      const regRes = await http.post("/api/user/register", {
        username: userFormData.username || "用户",
        phone: userFormData.phone,
        password: userFormData.password
      });
      auth.setSession({ role: "user", id: regRes.data?.id ?? null, name: regRes.data?.username || "", phone: regRes.data?.phone || "" });
      message.value = "注册成功";
      await router.push("/home");
      return;
    }

    const res = await http.post("/api/user/login", { phone: userFormData.phone, password: userFormData.password });
    auth.setSession({ role: "user", id: res.data?.id ?? null, name: res.data?.username || "", phone: res.data?.phone || "" });
    message.value = "登录成功";
    await router.push("/home");
  } catch (e) {
    if (e.name === 'Error') {
      // 表单验证失败
      return;
    }
    if (userMode.value === 'register' && e?.response?.status === 409) {
      message.value = "该号码已注册";
    } else if (e?.response?.status === 401) {
      message.value = "手机号或密码错误";
    } else if (e?.response?.status === 400) {
      message.value = "请输入用户信息";
    } else {
      message.value = e?.response?.data?.message || e?.message || "操作失败";
    }
  }
}

async function submitAdmin() {
  try {
    // 验证表单
    await adminForm.value.validate();
    
    message.value = "处理中...";
    
    if (adminMode.value === "register") {
      await http.post("/api/admins", { username: adminFormData.username, password: adminFormData.password, userType: "ADMIN" });
    }
    const res = await http.post("/api/admins/login", { username: adminFormData.username, password: adminFormData.password });
    auth.setSession({ role: "admin", id: res.data?.id ?? null, name: res.data?.username || "" });
    message.value = "登录成功";
    await router.push("/admin");
  } catch (e) {
    if (e.name === 'Error') {
      // 表单验证失败
      return;
    }
    if (adminMode.value === 'register' && e?.response?.status === 409) {
      message.value = "该用户名已存在";
    } else if (e?.response?.status === 401) {
      message.value = "用户名或密码错误";
    } else if (e?.response?.status === 400) {
      message.value = "请输入用户信息";
    } else {
      message.value = e?.response?.data?.message || e?.message || "操作失败";
    }
  }
}
</script>
