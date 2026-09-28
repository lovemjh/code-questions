<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>全局标签库</span>
        <el-button type="primary" @click="openCreate">新增标签</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="tags" border stripe>
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="标签名称" min-width="180" />
      <el-table-column label="创建时间" width="200">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRename(row)">重命名</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/重命名 标签对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '重命名标签' : '新增标签'"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="tagName"
        maxlength="50"
        show-word-limit
        placeholder="请输入标签名称"
        @keyup.enter="save"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTags, createTag, renameTag, deleteTag } from '../api/tag'

const loading = ref(false)
const tags = ref([])

async function load() {
  loading.value = true
  try {
    tags.value = await listTags()
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

// ---------- 新增 / 重命名 ----------
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(null)
const tagName = ref('')

function openCreate() {
  editing.value = null
  tagName.value = ''
  dialogVisible.value = true
}

function openRename(row) {
  editing.value = row
  tagName.value = row.name
  dialogVisible.value = true
}

async function save() {
  const name = tagName.value.trim()
  if (!name) {
    ElMessage.warning('标签名称不能为空')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await renameTag(editing.value.id, name)
      ElMessage.success('重命名成功')
    } else {
      await createTag(name)
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
    `确定删除标签「${row.name}」吗？删除后将同时解除所有人员与该标签的绑定。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return
  await deleteTag(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
