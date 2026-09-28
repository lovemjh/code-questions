import request from './request'

/** 用户登录 */
export function login(data) {
  return request.post('/auth/login', data)
}
