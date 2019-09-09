import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import VueI18n from 'vue-i18n'
import iView from 'iview'
import 'iview/dist/styles/iview.css'

import zh from 'iview/dist/locale/zh-CN'
import en from 'iview/dist/locale/en-US'

import zhCN from './assets/lang/zh'
import enUS from './assets/lang/en'

import './assets/styles/reset.less'
import './assets/styles/lib.less'
import './assets/styles/main.less'

Vue.use(VueI18n)
Vue.use(iView)
Vue.locale = () => {}

const messages = {
  zh: Object.assign(zh, zhCN),
  en: Object.assign(en, enUS)
}

const saveLang = window.localStorage.getItem('lang')
let language = navigator.language
if (!saveLang) {
  window.localStorage.setItem('lang', language.split('-')[0])
} else {
  language = saveLang
}

const i18n = new VueI18n({
  locale: language,
  messages
})

Vue.config.productionTip = false

new Vue({
  i18n,
  router,
  store,
  render: h => h(App)
}).$mount('#app')
