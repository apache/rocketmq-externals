import Vue from 'vue'
import Router from 'vue-router'

import Ops from './views/containers/Ops.vue'
import Dashboard from './views/containers/Dashboard.vue'
import Cluster from './views/containers/Cluster.vue'
import Topic from './views/containers/Topic.vue'
import Consumer from './views/containers/Consumer.vue'
import Producer from './views/containers/Producer.vue'
import Message from './views/containers/Message.vue'
import MessageTrace from './views/containers/MessageTrace.vue'
import NotFound from './views/containers/404.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard
    },
    {
      path: '/ops',
      name: 'Ops',
      component: Ops
    },
    {
      path: '/cluster',
      name: 'Cluster',
      component: Cluster
    },
    {
      path: '/topic',
      name: 'Topic',
      component: Topic
    },
    {
      path: '/consumer',
      name: 'Consumer',
      component: Consumer
    },
    {
      path: '/producer',
      name: 'Producer',
      component: Producer
    },
    {
      path: '/message',
      name: 'Message',
      component: Message
    },
    {
      path: '/messageTrace',
      name: 'MessageTrace',
      component: MessageTrace
    },
    {
      path: '/404',
      name: '404',
      component: NotFound
    },
    {
      path: '*',
      redirect: '/404'
    }
  ]
})
