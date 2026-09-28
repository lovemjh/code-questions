<template>
  <div class="person-overview">
    <!-- 上区域：左侧文字汇总 + 右侧饼图 -->
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>打标总览</span>
          </template>
          <div v-loading="loading" class="summary">
            <div class="summary-item">
              <div class="summary-label">人员总数量</div>
              <div class="summary-value">
                <span class="summary-num">{{ overview.total }}</span> 人
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-label">已分配标签人员</div>
              <div class="summary-value">
                <span class="summary-num has-tag">{{ overview.hasTagCount }}</span> 人
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-label">未分配标签人员</div>
              <div class="summary-value">
                <span class="summary-num no-tag">{{ overview.noTagCount }}</span> 人
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span>人员打标分布</span>
          </template>
          <div v-loading="loading" class="chart-area">
            <div class="chart-box">
              <div ref="pieRef" class="chart" />
              <!-- 全部人员都未打标/都已打标时仅剩一个扇区，无人员数据时展示空状态 -->
              <el-empty v-if="!loading && pieEmpty" description="暂无人员数据" :image-size="80" class="chart-empty" />
            </div>
            <div class="stat-note">统计口径：人员总数，每人仅统计一次</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下区域：通栏柱状图 -->
    <el-card shadow="never" class="panel-card">
      <template #header>
        <span>标签绑定人次统计</span>
      </template>
      <div v-loading="loading" class="chart-area">
        <div class="chart-box chart-box-bar">
          <div ref="barRef" class="chart" />
          <el-empty v-if="!loading && barEmpty" description="暂无标签数据" :image-size="80" class="chart-empty" />
        </div>
        <div class="stat-note">统计口径：标签绑定人次，同一人员绑定多标签会多次统计</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getTagOverview, getTagBindingCounts } from '../api/tag'

echarts.use([PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

// 配色沿用 Element Plus 主色，与整体风格保持一致
const COLOR_HAS_TAG = '#409EFF'
const COLOR_NO_TAG = '#E6A23C'

const loading = ref(true)
const overview = ref({ total: 0, hasTagCount: 0, noTagCount: 0 })
const bindings = ref([])

const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

const pieEmpty = computed(() => overview.value.total === 0)
const barEmpty = computed(() => bindings.value.length === 0)

function renderPie() {
  // 0 人的分类不出扇区：全部已打标 / 全部未打标时仅显示对应一项
  const data = [
    { name: '已分配标签', value: overview.value.hasTagCount, itemStyle: { color: COLOR_HAS_TAG } },
    { name: '未分配标签', value: overview.value.noTagCount, itemStyle: { color: COLOR_NO_TAG } }
  ].filter((item) => item.value > 0)

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: ({ name, value }) => `类型：${name}，人数：${value}`
    },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        name: '人员打标分布',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '46%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 人' },
        data
      }
    ]
  })
}

function renderBar() {
  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const { name, value } = params[0]
        return `标签：${name}，绑定人次：${value}`
      }
    },
    grid: { left: 48, right: 24, top: 32, bottom: 40 },
    xAxis: {
      type: 'category',
      data: bindings.value.map((item) => item.tagName),
      axisLabel: { interval: 0, rotate: bindings.value.length > 8 ? 30 : 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '绑定人次',
        type: 'bar',
        barMaxWidth: 40,
        itemStyle: { color: COLOR_HAS_TAG, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top' },
        data: bindings.value.map((item) => item.count)
      }
    ]
  })
}

function resizeCharts() {
  if (pieChart) pieChart.resize()
  if (barChart) barChart.resize()
}

async function load() {
  loading.value = true
  try {
    const [overviewData, bindingData] = await Promise.all([getTagOverview(), getTagBindingCounts()])
    overview.value = overviewData
    bindings.value = bindingData || []
    renderPie()
    renderBar()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  pieChart = echarts.init(pieRef.value)
  barChart = echarts.init(barRef.value)
  window.addEventListener('resize', resizeCharts)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  if (pieChart) pieChart.dispose()
  if (barChart) barChart.dispose()
})
</script>

<style scoped>
.person-overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 左侧文字汇总：与右侧图表区域等高、垂直居中 */
.summary {
  height: 330px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 36px;
}

.summary-item {
  padding-left: 32px;
}

.summary-label {
  font-size: 14px;
  color: #909399;
}

.summary-value {
  margin-top: 6px;
  font-size: 14px;
  color: #606266;
}

.summary-num {
  font-size: 32px;
  font-weight: 600;
  margin-right: 4px;
  color: #303133;
}

.summary-num.has-tag {
  color: #409eff;
}

.summary-num.no-tag {
  color: #e6a23c;
}

.chart-box {
  position: relative;
  height: 300px;
}

.chart-box-bar {
  height: 360px;
}

.chart {
  width: 100%;
  height: 100%;
}

/* 空状态覆盖层：盖住图表区域，提示文案见 description */
.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

/* 图表下方的统计口径小字标注 */
.stat-note {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style>
