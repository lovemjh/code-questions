import request from './request'

/** 全量角色列表 */
export function listRoles() {
  return request.get('/roles')
}
