import { createRouter, createWebHashHistory } from 'vue-router'
import MainUI from '../components/MainUI.vue'

const routes = [
    { path: '/', component: MainUI },
    { path: '/tiangong', component: MainUI },
    { path: '/tongyi', component: MainUI },
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router
