<template>
  <!-- 登录页等公开页面独立全屏展示，不套用主布局（顶栏 + 侧边菜单） -->
  <router-view v-if="isPublic" />
  <el-container v-else class="layout">
    <el-header class="header">
      <span class="logo">企业内部人员管理系统</span>
      <div class="header-right">
        <span class="user-info">{{ userInfo.displayName || userInfo.username }}</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container class="body">
      <el-aside width="200px" class="aside">
        <el-menu router :default-active="route.path" class="menu">
          <!-- 功能区：日常业务功能 -->
          <el-menu-item-group title="功能">
            <el-menu-item index="/persons">人员管理</el-menu-item>
            <el-menu-item index="/org">组织架构</el-menu-item>
            <el-menu-item index="/tags">标签管理</el-menu-item>
          </el-menu-item-group>
          <!-- 系统管理区：与功能区分开，仅管理员可见 -->
          <el-sub-menu v-if="isAdmin" index="/system" class="system-menu">
            <template #title>系统管理</template>
            <el-menu-item index="/users">用户管理</el-menu-item>
            <el-menu-item index="/roles">角色管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserInfo } from './utils/auth'

const route = useRoute()
const router = useRouter()

// 公开页面（登录页）不显示主布局
const isPublic = computed(() => !!route.meta.public)

function readUserInfo() {
  return getUserInfo()
}

// 用户信息从 localStorage 读取，需在路由变化（登录/退出）后刷新，保证顶栏显示与菜单权限正确
const userInfo = ref(readUserInfo())
watch(() => route.fullPath, () => {
  userInfo.value = readUserInfo()
})

const isAdmin = computed(() => userInfo.value.roles && userInfo.value.roles.includes('ADMIN'))

async function handleLogout() {
  const confirmed = await ElMessageBox.confirm(
    '确定退出登录吗？',
    '退出确认',
    { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1f2d3d;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  font-size: 14px;
  font-weight: normal;
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

/* 功能区与系统管理区的分隔 */
.system-menu {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid #ebeef5;
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
