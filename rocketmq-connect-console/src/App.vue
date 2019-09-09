<template>
  <div>
    <nav class="flex-h navbar">
      <menu class="flex-h">
        <a href="#" @click="activeIndex=1">{{$t('TITLE')}}</a>
        <ul class="flex-h">
          <li
            v-for="(item, index) in routes"
            :key="index"
            class="ml-5"
            :class="{active:index===activeIndex}"
            @click="toggleTabs(index)"
          >
            <router-link :to="item.path">{{$t(item.name)}}</router-link>
          </li>
        </ul>
      </menu>
      <Dropdown @on-click="handleChangeLang">
        <a href="javascript:void(0)">
          {{$t('CHANGE_LANG')}}
          <Icon type="ios-arrow-down"></Icon>
        </a>
        <DropdownMenu slot="list">
          <DropdownItem name="en">{{$t('EN_US')}}</DropdownItem>
          <DropdownItem name="zh">{{$t('ZH_CN')}}</DropdownItem>
        </DropdownMenu>
      </Dropdown>
    </nav>
    <router-view></router-view>
  </div>
</template>

<script>
export default {
  data () {
    return {
      routes: [
        { path: '/ops', name: 'OPS' },
        { path: '/', name: 'DASHBOARD' },
        { path: '/cluster', name: 'CLUSTER' },
        { path: '/topic', name: 'TOPIC' },
        { path: '/consumer', name: 'CONSUMER' },
        { path: '/producer', name: 'PRODUCER' },
        { path: '/message', name: 'MESSAGE' },
        { path: '/messageTrace', name: 'MESSAGETRACE' }
      ],
      activeIndex: 1
    }
  },
  methods: {
    handleChangeLang (name) {
      window.localStorage.setItem('lang', name)
      this.$i18n.locale = name
    },
    toggleTabs (index) {
      this.activeIndex = index
    }
  }
}
</script>

<style lang="less">
.navbar {
  height: 60px;
  font-size: 16px;
  padding: 15px;
  color: #ffffffd6;
  background-color: #009688;
  justify-content: space-between;
  a,a:hover {
    color: inherit;
  }

  li {
    padding: 20px 15px;
  }
}
.active {
  background-color: rgba(255, 255, 255, 0.1);
}
</style>
