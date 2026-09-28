import request from './request'

/** 分页查询用户 */
export function getUsers(params) {
  return request.get('/users', { params })
}

/** 用户详情 */
export function getUser(id) {
  return request.get(`/users/${id}`)
}

/** 新增用户 */
export function createUser(data) {
  return request.post('/users', data)
}

/** 编辑用户 */
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}
