import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/seats', name: 'seats', component: () => import('../views/SeatList.vue'), meta: { requiresAuth: true } },
  { path: '/my', name: 'my', component: () => import('../views/MyBookings.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/AdminOverview.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/seats', name: 'admin-seats', component: () => import('../views/AdminSeats.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/bookings', name: 'admin-bookings', component: () => import('../views/AdminBookings.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  // 兜底：登录了去座位页，没登录去登录页
  { path: '/:pathMatch(.*)*', redirect: () => {
    const store = useUserStore()
    return store.token ? '/seats' : '/login'
  } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：替代 React 版的 RequireLogin 组件
router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.requiresAuth && !store.token) {
    return { path: '/login', query: { from: to.fullPath } }
  }
  if (to.meta.requiresAdmin && store.user?.roleCode !== 'ADMIN') {
    return '/seats'
  }
  return true
})

export default router
