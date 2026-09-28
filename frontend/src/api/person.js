import request from './request'

/** 分页查询人员：{ pageNum, pageSize, name, tagId } */
export function getPersons(params) {
  return request.get('/persons', { params })
}

/** 人员详情 */
export function getPerson(id) {
  return request.get(`/persons/${id}`)
}

/** 组织架构树 */
export function getPersonTree() {
  return request.get('/persons/tree')
}

/** 调整上级；superiorId 为 null 表示设为顶级节点 */
export function updateSuperior(id, superiorId) {
  return request.put(`/persons/${id}/superior`, { superiorId })
}

/** 全量替换人员标签绑定 */
export function bindTags(id, tagIds) {
  return request.put(`/persons/${id}/tags`, { tagIds })
}
