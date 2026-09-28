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
