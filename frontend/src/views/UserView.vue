<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </template>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.username"
        placeholder="用户名"
        clearable
        style="width: 200px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="displayName" label="显示名称" width="150" />
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <el-tag v-for="role in row.roleNames" :key="role" size="small" class="role-tag">{{ role }}</el-tag>
          <span v-if="!row.roleNames || !row.roleNames.length">—</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-sizes="[10, 20, 50]"
      @current-change="load"
      @size-change="search"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑用户' : '新增用户'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" :disabled="!!editing" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="editForm.password" type="password" :placeholder="editing ? '不修改请留空' : '登录密码'" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="editForm.displayName" placeholder="界面显示名称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, deleteUser } from '../api/user'
import { listRoles } from '../api/role'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, username: '' })

const roleOptions = ref([])

async function load() {
  loading.value = true
  try {
    const page = await getUsers({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      username: query.username || undefined
    })
    rows.value = page.records
    total.value = Number(page.total)
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(null)
const editForm = reactive({
  id: null,
  username: '',
  password: '',
  displayName: '',
  roleIds: []
})

async function openCreate() {
  editing.value = null
  Object.assign(editForm, { id: null, username: '', password: '', displayName: '', roleIds: [] })
  dialogVisible.value = true
}

async function openEdit(row) {
  editing.value = row
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    password: '',
    displayName: row.displayName,
    roleIds: row.roleIds || []
  })
  dialogVisible.value = true
}

async function save() {
  // 密码强度：填写了就必须达到最小长度（编辑时留空表示不改密码）
  if (editForm.password && editForm.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editForm.id, {
        username: editForm.username,
        password: editForm.password || undefined,
        displayName: editForm.displayName,
        roleIds: editForm.roleIds
      })
      ElMessage.success('编辑成功')
    } else {
      await createUser({
        username: editForm.username,
        password: editForm.password,
        displayName: editForm.displayName,
        roleIds: editForm.roleIds
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除用户「${row.username}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  listRoles().then(roles => { roleOptions.value = roles })
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.role-tag {
  margin-right: 6px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
