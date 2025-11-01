<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand" @click="$router.push('/')">
        <span class="logo">微</span>
        <h1>Microblog</h1>
      </div>
      <nav>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/hotspot">热点聚焦</RouterLink>
        <RouterLink to="/topics">话题</RouterLink>
        <RouterLink v-if="authStore.user?.admin" to="/admin/hotspot">热点管理</RouterLink>
        <RouterLink v-if="!authStore.isAuthenticated" to="/login">登录</RouterLink>
        <RouterLink v-if="!authStore.isAuthenticated" to="/register">注册</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" :to="`/profile/${authStore.user?.id}`">我的主页</RouterLink>
        <button v-if="authStore.isAuthenticated" class="logout" @click="authStore.logout">退出</button>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { RouterLink, RouterView } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const authStore = useAuthStore();

onMounted(() => {
  if (authStore.token && !authStore.user) {
    authStore.fetchCurrentUser().catch(() => authStore.logout());
  }
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.25rem;
  font-weight: 600;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #22d3ee, #6366f1);
  color: #0f172a;
  font-weight: 700;
  font-size: 1.25rem;
}

nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

nav a {
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  transition: background 0.3s;
}

nav a.router-link-active {
  background: rgba(99, 102, 241, 0.2);
}

.logout {
  border: none;
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
}

.app-main {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1.5rem 4rem;
}
</style>
