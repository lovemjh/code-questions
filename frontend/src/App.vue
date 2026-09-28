<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">企业内部人员管理系统</span>
    </el-header>
    <el-container class="body">
      <el-aside width="200px" class="aside">
        <el-menu router :default-active="route.path" class="menu">
          <el-menu-item index="/persons">人员管理</el-menu-item>
          <el-menu-item index="/org">组织架构</el-menu-item>
          <el-menu-item index="/tags">标签管理</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.layout {
  height: 100%;
}

.header {
  display: flex;
  align-items: center;
  background: #1f2d3d;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.body {
  height: calc(100% - 60px);
}

.aside {
  background: #fff;
  border-right: 1px solid #e6e6e6;
}

.menu {
  border-right: none;
}

.main {
  background: #f5f7fa;
}
</style>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei',
    Arial, sans-serif;
}
</style>
