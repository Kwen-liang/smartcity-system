import { createRouter, createWebHistory } from 'vue-router'
import MainDashboard from '@/views/MainDashboard.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      // 2. 将默认路径 '/' 指向 MainDashboard
      component: MainDashboard
    }
  ]
})

export default router
