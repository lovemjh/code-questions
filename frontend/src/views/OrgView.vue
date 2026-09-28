<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>组织架构</span>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>

    <el-tree
      v-loading="loading"
      :data="tree"
      node-key="id"
      default-expand-all
      :expand-on-click-node="false"
    >
      <template #default="{ data }">
        <div class="node">
          <span class="node-info">
            <b>{{ data.name }}</b>
            <span class="meta">{{ data.department }}<template v-if="data.position"> · {{ data.position }}</template></span>
          </span>
          <!-- 调整上级属于修改操作，仅管理员可见 -->
          <el-button v-if="canEdit" link type="primary" size="small" @click="openAdjust(data)">调整上级</el-button>
        </div>
      </template>
    </el-tree>

    <!-- 调整上级对话框：清空选择表示设为顶级节点 -->
    <el-dialog v-model="adjustVisible" title="调整上级" width="480px" :close-on-click-modal="false">
      <p class="tip">
        为 <b>{{ current && current.name }}</b> 选择新的上级（不选择表示设为顶级节点）：
      </p>
      <el-tree-select
        v-model="newSuperiorId"
        :data="adjustTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        default-expand-all
        placeholder="不选择表示设为顶级节点"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPersonTree, updateSuperior } from '../api/person'
import { isAdmin } from '../utils/auth'

// 是否可调整上下级关系：由角色决定
const canEdit = isAdmin()

const loading = ref(false)
const tree = ref([])

async function load() {
  loading.value = true
  try {
    tree.value = await getPersonTree()
  } finally {
    loading.value = false
  }
}

// ---------- 调整上级 ----------
const adjustVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const newSuperiorId = ref(null)
const adjustTree = ref([])

/** 剔除本人节点：本人及其下属 subtree 不可作为自己的上级（后端仍有循环校验兜底） */
function removeFromTree(nodes, id) {
  return nodes
    .filter((n) => n.id !== id)
    .map((n) => ({ ...n, children: removeFromTree(n.children || [], id) }))
}

function openAdjust(node) {
  current.value = node
  newSuperiorId.value = null
  adjustTree.value = removeFromTree(tree.value, node.id)
  adjustVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await updateSuperior(current.value.id, newSuperiorId.value || null)
    ElMessage.success('上级调整成功')
    adjustVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.node {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 12px;
}

.meta {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.tip {
  margin: 0 0 12px;
}
</style>
