<template>
  <div class="home">
    <el-card class="welcome-card">
      <template #header>
        <span>企业内部人员管理系统</span>
      </template>
      <p>项目初始化完成，后续将在此提供以下功能：</p>
      <div class="features">
        <el-tag>人员列表</el-tag>
        <el-tag type="success">组织架构</el-tag>
        <el-tag type="warning">标签管理</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.welcome-card {
  width: 480px;
}

.features {
  display: flex;
  gap: 8px;
}
</style>
