import { createRouter, createWebHistory } from 'vue-router'
import RoomList from '@/components/RoomList.vue'
import OpenVidu from '@/components/OpenVidu.vue'

const routes = [
  {
    path: '/',
    name: 'RoomList',
    component: RoomList
  },
  {
    path: '/room/:sessionId',
    name: 'OpenVidu',
    component: OpenVidu,
    props: true
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
