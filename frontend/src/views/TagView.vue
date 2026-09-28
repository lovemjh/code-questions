<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>全局标签库</span>
        <div v-if="canEdit" class="header-actions">
          <el-button :loading="exporting" @click="exportCsv">导出</el-button>
          <el-button :loading="importing" @click="startImport">导入</el-button>
          <el-button type="primary" @click="openCreate">新增标签</el-button>
        </div>
      </div>
    </template>

    <el-table v-loading="loading" :data="tags" border stripe>
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="标签名称" min-width="180" />
      <el-table-column label="创建时间" width="200">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <!-- 修改类操作仅管理员可见（与后端 /api/tags 的角色校验一致） -->
      <el-table-column v-if="canEdit" label="操作" width="160" fixed="right">
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

    <!-- 隐藏的 CSV 文件选择器（导入用） -->
    <input ref="fileInput" type="file" accept=".csv,text/csv" class="hidden-file" @change="onFileChange" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTags, createTag, renameTag, deleteTag, exportTags, importTags } from '../api/tag'
import { isAdmin } from '../utils/auth'

// 是否可维护标签：由角色决定
const canEdit = isAdmin()

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

// ---------- 导入 / 导出 ----------
const fileInput = ref(null)
const importing = ref(false)
const exporting = ref(false)
const MAX_FILE_SIZE = 2 * 1024 * 1024

async function startImport() {
  const confirmed = await ElMessageBox.confirm(
    '请选择 CSV 文件（首行表头：ID,标签名称）。ID 已存在的更新，不存在的新增；' +
      '导入前会整体校验，只要有一行有误就不会修改任何数据。',
    '导入标签',
    { type: 'info', confirmButtonText: '选择文件', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return
  // 清空后触发 click，保证连续选择同一文件也能再次上传
  fileInput.value.value = ''
  fileInput.value.click()
}

async function onFileChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!/\.csv$/i.test(file.name)) {
    ElMessage.warning('仅支持 CSV 文件（可由 Excel「另存为 CSV」生成）')
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.warning('文件不能超过 2MB')
    return
  }

  importing.value = true
  try {
    const result = await importTags(file)
    if (result.success) {
      const parts = [`新增 ${result.created} 条`, `更新 ${result.updated} 条`]
      if (result.unchanged) parts.push(`未变化 ${result.unchanged} 条`)
      await ElMessageBox.alert(
        `本次导入：${parts.join('、')}。<br/>当前标签总数：${result.total} 条。`,
        '导入成功',
        { type: 'success', dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
      )
      load()
    } else {
      showImportErrors(result)
    }
  } finally {
    importing.value = false
  }
}

function showImportErrors(result) {
  const errors = result.errors || []
  const lines = errors.map((e) => `第 ${e.row} 行：${escapeHtml(e.reason)}`)
  if (result.errorTotal > errors.length) {
    lines.push(`…共 ${result.errorTotal} 处错误，仅展示前 ${errors.length} 条`)
  }
  lines.push('<b>文件未做任何修改，请修正后重新导入。</b>')
  ElMessageBox.alert(lines.join('<br/>'), `导入失败：发现 ${result.errorTotal} 处错误`, {
    type: 'error',
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了'
  })
}

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await exportTags()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `标签库_${today()}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function today() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`
}

// 错误原因包含文件中的标签名，展示前做 HTML 转义，避免文件内容注入
function escapeHtml(value) {
  return String(value).replace(
    /[&<>"']/g,
    (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch])
  )
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0;
}

.hidden-file {
  display: none;
}
</style>
