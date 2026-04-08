import { createApp, h, render } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import CustomCheckbox from './components/CustomCheckbox.vue'
import NeoSwitch from './components/NeoSwitch.vue'
import LockSwitch from './components/LockSwitch.vue'
import LoadingSpinner from './components/LoadingSpinner.vue'

const app = createApp(App)

app.component('CustomCheckbox', CustomCheckbox)
app.component('NeoSwitch', NeoSwitch)
app.component('LockSwitch', LockSwitch)

app.directive('loading', {
  mounted(el, binding) {
    if (binding.value) appendLoading(el)
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      if (binding.value) appendLoading(el)
      else removeLoading(el)
    }
  },
  unmounted(el) {
    removeLoading(el)
  }
})

function appendLoading(el) {
  if (el.querySelector('.custom-loading-overlay')) return
  const style = window.getComputedStyle(el)
  if (!style.position || style.position === 'static') {
    el.style.position = 'relative'
  }
  const overlay = document.createElement('div')
  overlay.className = 'custom-loading-overlay'
  render(h(LoadingSpinner, { label: '正在加载…', inline: true }), overlay)
  el.appendChild(overlay)
}

function removeLoading(el) {
  const overlay = el.querySelector('.custom-loading-overlay')
  if (!overlay) return
  render(null, overlay)
  el.removeChild(overlay)
}

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
