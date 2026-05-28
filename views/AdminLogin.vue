<template>
  <el-card style="max-width:520px; margin:0 auto">
    <div style="display:flex; align-items:center; gap:8px; margin-bottom:12px">
      <el-icon color="#36a3e0"><User /></el-icon>
      <span style="font-weight:600">管理员登录</span>
    </div>
    <el-form @submit.prevent="login" label-width="100px">
      <el-form-item label="用户名">
        <el-input v-model="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="password" type="password" placeholder="请输入密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="login">登录</el-button>
      </el-form-item>
    </el-form>
    <div style="margin-top:12px">结果：{{ result }}</div>
  </el-card>
</template>

<script setup>
import { ref } from "vue";
import { User } from "@element-plus/icons-vue";
import http from "../api/http";
import { useRouter } from "vue-router";

const router = useRouter();
const username = ref("");
const password = ref("");
const result = ref("");

async function login() {
  if (!username.value || !password.value) {
    result.value = "请输入用户信息";
    return;
  }
  result.value = "登录中...";
  try {
    const res = await http.post("/api/admins/login", { username: username.value, password: password.value });
    result.value = `欢迎，${res.data?.username ?? ""}（${res.data?.userType ?? ""}）`;
    setTimeout(() => router.push("/admin"), 800);
  } catch (e) {
    if (e?.response?.status === 400) {
      result.value = "请输入用户信息";
    } else {
      result.value = e?.response?.data?.message || e?.message || "登录失败";
    }
  }
}
</script>
