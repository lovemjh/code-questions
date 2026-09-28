import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录', public: true }
    },
    { path: '/', redirect: '/person-overview' },
    {
      path: '/person-overview',
      name: 'person-overview',
      component: () => import('../views/PersonOverviewView.vue'),
      meta: { title: '人员总览' }
    },
    {
      path: '/persons',
      name: 'persons',
      component: () => import('../views/PersonView.vue'),
      meta: { title: '人员管理' }
    },
    {
      path: '/org',
      name: 'org',
      component: () => import('../views/OrgView.vue'),
      meta: { title: '组织架构' }
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('../views/TagView.vue'),
      meta: { title: '标签管理' }
    },
    {
      path: '/users',
      name: 'users',
      component: () => import('../views/UserView.vue'),
      meta: { title: '用户管理', requireAdmin: true }
    },
    {
      path: '/roles',
      name: 'roles',
      component: () => import('../views/RoleView.vue'),
      meta: { title: '角色管理', requireAdmin: true }
    }
  ]
})

// 路由守卫：未登录跳转登录页，已登录访问登录页回人员总览，权限不足跳转人员总览
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public && token) {
    return '/person-overview'
  }
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.meta.requireAdmin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (!userInfo.roles || !userInfo.roles.includes('ADMIN')) {
      return '/person-overview'
    }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 企业内部人员管理系统` : '企业内部人员管理系统'
})

export default router
