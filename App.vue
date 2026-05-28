
<template>
  <el-container style="min-height: 100vh; background: transparent">
    <el-header
      v-if="auth.isLoggedIn"
      style="background: rgba(255,255,255,0.7); backdrop-filter: saturate(180%) blur(8px); border-bottom: 1px solid rgba(255,255,255,0.3)"
    >
      <div style="display:flex; align-items:center; justify-content:space-between; gap:12px">
        <div style="display:flex; align-items:center; gap:12px">
          <el-icon color="#36a3e0" size="24"><Van /></el-icon>
          <span style="font-weight:600; font-size:18px">校园无人车路径规划系统</span>
        </div>
        <div v-if="auth.isLoggedIn" style="display:flex; align-items:center; gap:10px">
          <el-tag type="success" effect="light">{{ auth.role === 'admin' ? '管理员' : '用户' }}</el-tag>
          <el-button link type="primary" @click="openProfileDialog">{{ auth.displayName || '个人信息' }}</el-button>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </div>
      <el-menu mode="horizontal" router background-color="transparent" active-text-color="#36a3e0" style="border:none; margin-top:8px">
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item v-if="auth.role === 'user'" index="/order/submit">下单</el-menu-item>
        <el-menu-item v-if="auth.role === 'user'" index="/order/query">查订单</el-menu-item>
        <el-menu-item v-if="auth.role === 'admin'" index="/admin">后台主页</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <div class="cd-container">
        <RouterView />
      </div>
    </el-main>
  </el-container>

  <el-dialog v-model="profileDialogVisible" title="个人信息" width="420px">
    <el-form label-width="90px">
      <el-form-item v-if="auth.role === 'user'" label="手机号">
        <el-input :model-value="auth.session?.phone || ''" disabled />
      </el-form-item>
      <el-form-item v-if="auth.role === 'admin'" label="管理员ID">
        <el-input :model-value="String(auth.session?.id ?? '')" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="profileForm.username" placeholder="请输入昵称" />
      </el-form-item>
      <el-divider />
      <el-form-item label="原密码">
        <el-input v-model="profileForm.oldPassword" type="password" placeholder="修改密码需要填写" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="profileForm.newPassword" type="password" placeholder="留空则不修改" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="profileDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveProfile">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterView } from "vue-router";
import { useRouter } from "vue-router";
import { Van } from "@element-plus/icons-vue";
import { useAuthStore } from "./stores/auth";
import http from "./api/http";

const router = useRouter();
const auth = useAuthStore();

const profileDialogVisible = ref(false);
const profileForm = reactive({
  username: "",
  oldPassword: "",
  newPassword: ""
});

function logout() {
  auth.logout();
  router.push("/login");
}

function openProfileDialog() {
  profileForm.username = auth.session?.name || "";
  profileForm.oldPassword = "";
  profileForm.newPassword = "";
  profileDialogVisible.value = true;
}

async function saveProfile() {
  if (auth.role === "user") {
    const phone = auth.session?.phone || "";
    if (!phone) return;
    const payload = {
      phone,
      username: profileForm.username,
      oldPassword: profileForm.oldPassword,
      newPassword: profileForm.newPassword
    };
    const res = await http.put("/api/user/profile", payload);
    auth.setSession({ ...auth.session, name: res.data?.username || profileForm.username, phone: res.data?.phone || phone });
    profileDialogVisible.value = false;
    return;
  }

  if (auth.role === "admin") {
    const id = auth.session?.id ?? null;
    if (!id) return;
    const payload = {
      id,
      username: profileForm.username,
      oldPassword: profileForm.oldPassword,
      newPassword: profileForm.newPassword
    };
    const res = await http.put("/api/admin/profile", payload);
    auth.setSession({ ...auth.session, name: res.data?.username || profileForm.username });
    profileDialogVisible.value = false;
    return;
  }

  profileDialogVisible.value = false;
}
</script>
