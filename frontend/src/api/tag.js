import request from './request'

/** 全量标签列表 */
export function listTags() {
  return request.get('/tags')
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
