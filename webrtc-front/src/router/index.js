import { createRouter, createWebHistory } from 'vue-router'
import RoomList from '@/components/meeting/RoomList.vue'
import OpenVidu from '@/components/meeting/MainChat.vue'
import HomeVue from '@/components/streaming/RoomList.vue'
import StreamOpenVidu from '@/components/streaming/MainChat.vue'

const routes = [
  {
    path: '/meeting',
    name: 'RoomList',
    component: RoomList
  },
  {
    path: '/room/:sessionId',
    name: 'OpenVidu',
    component: OpenVidu
  },
  {
    path: '/streamroom',
    name: 'HomeVue',
    component: HomeVue
  },
  {
    path: '/streamroom/:sessionId',
    name: 'StreamOpenVidu',
    component: StreamOpenVidu
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
