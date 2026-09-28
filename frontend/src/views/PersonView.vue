<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏：姓名关键词 + 标签筛选 -->
      <div class="toolbar">
        <el-input
          v-model="query.name"
          placeholder="姓名关键词"
          clearable
          style="width: 200px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select
          v-model="query.tagId"
          placeholder="按标签筛选"
          clearable
          style="width: 180px"
          @change="search"
        >
          <el-option v-for="t in tagOptions" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <!-- 人员表格：基础信息由外部系统同步，只读展示 -->
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="employeeNo" label="工号" width="100" />
        <el-table-column prop="department" label="部门" min-width="130" />
        <el-table-column prop="position" label="职位" min-width="130" />
        <el-table-column label="上级" width="100">
          <template #default="{ row }">{{ row.superiorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t.id" size="small" class="tag-item">{{ t.name }}</el-tag>
            <span v-if="!row.tags || !row.tags.length">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pager"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <!-- 编辑对话框：仅可维护上级关系与标签绑定，基础信息只读 -->
    <el-dialog
      v-model="editVisible"
      title="编辑人员"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="姓名">
          <el-input :model-value="editForm.name" disabled />
        </el-form-item>
        <el-form-item label="工号">
          <el-input :model-value="editForm.employeeNo" disabled />
        </el-form-item>
        <el-form-item label="部门 / 职位">
          <el-input
            :model-value="[editForm.department, editForm.position].filter(Boolean).join(' / ')"
            disabled
          />
        </el-form-item>
        <el-form-item label="上级">
          <el-tree-select
            v-model="editForm.superiorId"
            :data="superiorTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            default-expand-all
            placeholder="不选择表示设为顶级节点"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="editForm.tagIds"
            multiple
            filterable
            allow-create
            default-first-option
            :loading="tagCreating"
            placeholder="可输入新标签名称直接创建"
            style="width: 100%"
            @change="onTagsChange"
          >
            <el-option v-for="t in tagOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPersons, getPersonTree, updateSuperior, bindTags } from '../api/person'
import { listTags, createTag } from '../api/tag'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, name: '', tagId: null })

const tagOptions = ref([])

async function load() {
  loading.value = true
  try {
    const page = await getPersons({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      tagId: query.tagId || undefined
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

function reset() {
  query.name = ''
  query.tagId = null
  search()
}

async function loadTags() {
  tagOptions.value = await listTags()
}

// ---------- 编辑弹窗 ----------
const editVisible = ref(false)
const saving = ref(false)
const tagCreating = ref(false)
const superiorTree = ref([])
const editForm = reactive({
  id: null,
  name: '',
  employeeNo: '',
  department: '',
  position: '',
  superiorId: null,
  tagIds: []
})

/** 从组织树中剔除本人（本人及其下属不能作为自己的上级） */
function removeFromTree(nodes, id) {
  return nodes
    .filter((n) => n.id !== id)
    .map((n) => ({ ...n, children: removeFromTree(n.children || [], id) }))
}

async function openEdit(row) {
  editForm.id = row.id
  editForm.name = row.name
  editForm.employeeNo = row.employeeNo
  editForm.department = row.department
  editForm.position = row.position
  editForm.superiorId = row.superiorId
  editForm.tagIds = (row.tags || []).map((t) => t.id)

  const [tree, tags] = await Promise.all([getPersonTree(), listTags()])
  superiorTree.value = removeFromTree(tree, row.id)
  tagOptions.value = tags
  editVisible.value = true
}

/** 标签多选：输入的新名称先创建进全局标签库，再以标签ID参与绑定 */
async function onTagsChange(vals) {
  tagCreating.value = true
  try {
    for (let i = 0; i < vals.length; i++) {
      const v = vals[i]
      if (typeof v !== 'string') continue
      const name = v.trim()
      if (!name) {
        vals.splice(i, 1)
        i--
        continue
      }
      // 已存在同名标签直接复用，否则创建并加入全局标签库
      const existing = tagOptions.value.find((t) => t.name === name)
      const tag = existing || (await createTag(name))
      if (!existing) tagOptions.value.push(tag)
      vals[i] = tag.id
    }
    editForm.tagIds = [...new Set(vals)]
  } catch (e) {
    // 创建失败（如重名冲突）：移除未落库的字符串项，保持选中项合法
    editForm.tagIds = vals.filter((v) => typeof v !== 'string')
  } finally {
    tagCreating.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updateSuperior(editForm.id, editForm.superiorId || null)
    await bindTags(editForm.id, editForm.tagIds)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  loadTags()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 2px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
