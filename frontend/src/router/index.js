import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/persons' },
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
    }
  ]
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 企业内部人员管理系统` : '企业内部人员管理系统'
})

export default router
