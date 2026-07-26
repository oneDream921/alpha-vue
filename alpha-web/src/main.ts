import Antd from 'ant-design-vue'
import { createApp } from 'vue'

import App from './App.vue'
import { permission } from './directives/permission'
import router from './router'
import { ensureManagementRoutes } from './router'
import { authStore } from './stores/auth'
import { pinia } from './stores/pinia'
import 'ant-design-vue/dist/reset.css'
import './styles/index.css'

ensureManagementRoutes(authStore.state.routes)

createApp(App)
    .use(pinia)
    .use(router)
    .use(Antd)
    .directive('permission', permission)
    .mount('#app')
