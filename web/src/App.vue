<script setup>
import { onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElButton, ElMenu, ElMenuItem, ElTag } from 'element-plus'
import { useUserStore } from './stores/user.js'
import request from './api/request.js'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const user = computed(() => store.user)
const isAdmin = computed(() => user.value?.roleCode === 'ADMIN')

// 页面一打开就验证 token 还有没有效（可能已被拉黑/踢下线）
onMounted(() => {
  if (!store.token) return
  request.get('/auth/me').then((me) => store.setUser(me)).catch(() => store.logout())
})

function logout() {
  store.logout()
  router.push('/login')
}

// 用 computed 绑定 el-menu 的 default-active，路由变了菜单高亮跟着变
const activeMenu = computed(() => route.path)
</script>

<template>
  <el-container style="min-height: 100vh">
    <el-header style="display: flex; align-items: center; gap: 24px; background: #1d2939">
      <div style="color: #fff; font-size: 18px; white-space: nowrap">
        研习空间预约与履约平台
      </div>

      <el-menu
        v-if="store.token"
        :default-active="activeMenu"
        mode="horizontal"
        theme="dark"
        :ellipsis="false"
        style="flex: 1; min-width: 0; border-bottom: none"
        @select="(key) => router.push(key)"
      >
        <el-menu-item index="/seats">
          座位预约
        </el-menu-item>
        <el-menu-item index="/my">
          我的预约
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/admin"
        >
          运营看板
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/admin/seats"
        >
          座位管理
        </el-menu-item>
        <el-menu-item
          v-if="isAdmin"
          index="/admin/bookings"
        >
          预约管理
        </el-menu-item>
      </el-menu>

      <div style="margin-left: auto; display: flex; align-items: center; gap: 12px">
        <el-tag
          v-if="user"
          :type="isAdmin ? 'warning' : 'primary'"
          effect="dark"
        >
          {{ user.nickname || user.username }}
        </el-tag>
        <el-button
          v-if="store.token"
          size="small"
          @click="logout"
        >
          退出
        </el-button>
        <router-link
          v-else-if="route.path !== '/login'"
          to="/login"
          style="color: #fff"
        >去登录</router-link>
      </div>
    </el-header>

    <el-main style="padding: 24px">
      <router-view />
    </el-main>
  </el-container>
</template>
