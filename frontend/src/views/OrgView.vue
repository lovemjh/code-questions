<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>组织架构</span>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>

    <!--
      拖拽调整层级（仅管理员可拖动）：
      拖到人员节点上释放 = 移动为该人员下属；拖到底部投放栏（或树两侧空白处）释放 = 设置为独立（顶级）节点
    -->
    <div
      ref="wrapRef"
      v-loading="loading"
      class="tree-wrap"
      @dragover="onWrapDragOver"
      @dragleave="onWrapDragLeave"
      @drop="onWrapDrop"
    >
      <el-tree
        :data="tree"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
        :draggable="canEdit"
        :allow-drop="allowDrop"
        @node-drag-start="onDragStart"
        @node-drag-over="onNodeDragOver"
        @node-drag-end="onDragEnd"
        @node-drop="onNodeDrop"
      >
        <template #default="{ data }">
          <!-- 拖拽中：自身及其下属 subtree 不可作为投放目标，做弱化显示 -->
          <div class="node" :class="{ 'node-forbidden': dragState.active && forbiddenIds.has(data.id) }">
            <span class="node-info">
              <b>{{ data.name }}</b>
              <span class="meta">{{ data.department }}<template v-if="data.position"> · {{ data.position }}</template></span>
            </span>
          </div>
        </template>
      </el-tree>

      <!-- 拖拽期间固定悬浮在视口底部的投放栏：树再长也无需长距离滚动，随时可投放 -->
      <div v-if="dragState.active" class="drop-dock" :class="{ 'drop-dock-active': blankHover }">
        释放到此处：设置为独立（顶级）节点
      </div>
    </div>

    <!-- 跟随鼠标的悬浮感知提示 -->
    <div v-show="dragTip.visible" class="drag-tip" :style="{ left: dragTip.x + 'px', top: dragTip.y + 'px' }">
      {{ dragTip.text }}
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { getPersonTree, updateSuperior } from '../api/person'
import { isAdmin } from '../utils/auth'

// 是否可拖拽调整上下级关系：由角色决定（后端 PUT /api/persons/** 同样仅管理员）
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

// ---------- 拖拽调整层级 ----------
const wrapRef = ref(null)
const dragState = ref({ active: false })
/** 拖拽人员自身及其全部下属的 id：投放会形成组织循环，禁止作为目标 */
const forbiddenIds = ref(new Set())
const blankHover = ref(false)
/** 空白区已释放标记：用于忽略 EP 依据「最后悬停节点」补发的 node-drop */
let blankDropped = false

const dragTip = ref({ visible: false, x: 0, y: 0, text: '' })

function onDragStart(node) {
  // 每次拖拽重置空白释放标记（EP 在 node-drag-end 之后才补发 node-drop，不能在 dragend 里重置）
  blankDropped = false

  // 收集被拖人员自身及其全部下属 id：投放这些目标会形成组织循环
  const ids = new Set()
  const flatten = (n) => {
    ids.add(n.id)
    for (const c of n.children || []) flatten(c)
  }
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.id === node.data.id) {
        flatten(n)
        return true
      }
      if (find(n.children || [])) return true
    }
    return false
  }
  find(tree.value)

  dragState.value = { active: true, id: node.data.id, name: node.data.name }
  forbiddenIds.value = ids
}

/** 仅允许投放到人员节点内部（成为其下属），且不能是自己或自己的下属 */
function allowDrop(draggingNode, dropNode, type) {
  return type === 'inner' && !forbiddenIds.value.has(dropNode.data.id)
}

/** 节点上的原生 dragover 被 EP stop，悬浮感知走 node-drag-over 事件 */
function onNodeDragOver(draggingNode, dropNode, event) {
  blankHover.value = false
  const targetId = dropNode.data.id
  const text = forbiddenIds.value.has(targetId)
    ? '不能移动到自己或自己的下属'
    : `移动为「${dropNode.data.name}」下属`
  showTip(event.clientX, event.clientY, text)
}

function onDragEnd() {
  // 注意：此处不重置 blankDropped——EP 先发 node-drag-end 再发 node-drop，
  // 若在这里清标记，「悬停节点 → 拖到投放栏/空白区释放」会误触发一次移动到最后悬停节点的投放
  dragState.value = { active: false }
  forbiddenIds.value = new Set()
  blankHover.value = false
  dragTip.value.visible = false
}

async function onNodeDrop(draggingNode, dropNode) {
  // 悬停过节点后再拖回空白区释放：EP 仍会按最后悬停的节点补发 node-drop，忽略之
  if (blankDropped) return
  await applyMove(draggingNode.data.id, draggingNode.data.name, dropNode.data.id, dropNode.data.name)
}

/** 空白区域悬浮感知（节点上的 dragover 被 EP stop，能冒泡到这里的只有空白区域） */
function onWrapDragOver(event) {
  if (!dragState.value.active) return
  event.preventDefault()
  blankHover.value = true
  showTip(event.clientX, event.clientY, '释放：立即设置为独立（顶级）节点')
}

function onWrapDragLeave(event) {
  const wrap = wrapRef.value
  if (wrap && event.relatedTarget && wrap.contains(event.relatedTarget)) return
  blankHover.value = false
  dragTip.value.visible = false
}

/** 空白区域释放：设为顶级节点 */
async function onWrapDrop(event) {
  if (!dragState.value.active) return
  if (event.target.closest('.node')) return
  event.preventDefault()
  blankDropped = true
  await applyMove(dragState.value.id, dragState.value.name, null, '')
}

async function applyMove(personId, personName, superiorId, superiorName) {
  try {
    await updateSuperior(personId, superiorId)
    ElMessage.success(
      superiorId == null
        ? `已将「${personName}」设置为独立（顶级）节点`
        : `已将「${personName}」移动为「${superiorName}」的下属`
    )
  } finally {
    // 无论成功与否都重新拉取：成功反映新结构，失败时回滚 el-tree 拖拽后的本地临时结构
    load()
  }
}

function showTip(x, y, text) {
  const left = Math.min(x + 14, window.innerWidth - 240)
  const top = Math.min(y + 18, window.innerHeight - 44)
  dragTip.value = { visible: true, x: Math.max(8, left), y: Math.max(8, top), text }
}

onMounted(() => {
  load()
  // 页面进入提示：本页通过拖拽调整层级关系（拖拽仅管理员可用，普通用户不展示）
  if (canEdit) {
    ElNotification({
      title: '拖拽调整层级',
      message:
        '拖动人员节点到目标人员上释放：移动为该人员的下属；<br/>拖到屏幕底部弹出的投放栏释放：立即设置为独立（顶级）节点。',
      type: 'info',
      position: 'top-right',
      duration: 10000,
      dangerouslyUseHTMLString: true
    })
  }
})
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
  align-items: center;
  padding-right: 12px;
  transition: opacity 0.15s;
}

/* 拖拽中不可作为目标的节点（自身及下属）弱化显示 */
.node-forbidden {
  opacity: 0.45;
}

.meta {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

/* 拖拽期间固定悬浮在视口底部的投放栏 */
.drop-dock {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: 2500;
  padding: 14px 48px;
  font-size: 14px;
  color: #909399;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.15s;
}

.drop-dock.drop-dock-active {
  color: #409eff;
  border-color: #409eff;
  background: #ecf5ff;
}

/* 跟随鼠标的悬浮感知提示 */
.drag-tip {
  position: fixed;
  z-index: 3000;
  padding: 6px 12px;
  font-size: 13px;
  color: #fff;
  background: #303133;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  pointer-events: none;
  white-space: nowrap;
}
</style>
