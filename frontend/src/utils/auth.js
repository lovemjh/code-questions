/**
 * 登录态与权限相关的本地工具方法
 *
 * 权限由“角色”决定（登录接口返回的 roles，来源于用户-角色关联表），
 * 与用户名无关：admin 只是一个普通用户名，它拥有管理员权限是因为被赋予了 ADMIN 角色。
 */

/** 读取本地保存的登录用户信息 */
export function getUserInfo() {
  return JSON.parse(localStorage.getItem('userInfo') || '{}')
}

/** 当前登录用户是否拥有管理员（ADMIN）角色 */
export function isAdmin() {
  const roles = getUserInfo().roles || []
  return roles.includes('ADMIN')
}
