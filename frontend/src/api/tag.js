import request from './request'

/** 全量标签列表 */
export function listTags() {
  return request.get('/tags')
}

/** 人员打标大盘统计（文字汇总 + 饼图）：人员维度，每人仅统计一次 */
export function getTagOverview() {
  return request.get('/tags/stats/overview')
}

/** 各标签绑定人次统计（柱状图）：人次维度，同一人员绑定多标签会多次统计 */
export function getTagBindingCounts() {
  return request.get('/tags/stats/bindings')
}

/** 新增标签（自动加入全局标签库） */
export function createTag(name) {
  return request.post('/tags', { name })
}

/** 重命名标签 */
export function renameTag(id, name) {
  return request.put(`/tags/${id}`, { name })
}

/** 删除标签（后端级联解除人员绑定） */
export function deleteTag(id) {
  return request.delete(`/tags/${id}`)
}

/** 导出标签 CSV（管理员，返回 Blob 文件流） */
export function exportTags() {
  return request.get('/tags/export', { responseType: 'blob' })
}

/** 导入标签 CSV（管理员）：以 ID 为唯一键，同 ID 更新、异 ID 新增 */
export function importTags(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/tags/import', form)
}
