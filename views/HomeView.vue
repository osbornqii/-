
<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-card>
          <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px">
            <el-icon color="#34c38f"><Sunny /></el-icon>
            <span style="font-weight:600">欢迎使用校园无人配送系统</span>
          </div>
          <div>为校园快件提供高效、智能、绿色的配送服务。</div>
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { Sunny } from "@element-plus/icons-vue";
</script>
