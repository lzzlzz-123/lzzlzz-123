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
        <RouterLink v-if="authStore.user?.admin" to="/admin/home-ads">广告管理</RouterLink>
        <RouterLink v-if="!authStore.isAuthenticated" to="/login">登录</RouterLink>
        <RouterLink v-if="!authStore.isAuthenticated" to="/register">注册</RouterLink>
        <RouterLink v-if="authStore.isAuthenticated" :to="`/profile/${authStore.user?.id}`">我的主页</RouterLink>
        <button v-if="authStore.isAuthenticated" class="logout" @click="authStore.logout">退出</button>
      </nav>
    </header>
    <main class="app-main">
      <RouterView v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
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
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 2rem;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 20px -5px rgba(0, 0, 0, 0.3);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.25rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}

.brand:hover {
  opacity: 0.9;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.6rem;
  background: linear-gradient(135deg, #38bdf8, #818cf8);
  color: #0f172a;
  font-weight: 800;
  font-size: 1.15rem;
  box-shadow: 0 0 15px rgba(56, 189, 248, 0.4);
}

.brand h1 {
  font-size: 1.25rem;
  letter-spacing: -0.025em;
  background: linear-gradient(to right, #f8fafc, #cbd5e1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

nav a {
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  transition: all 0.2s;
  font-size: 0.95rem;
  font-weight: 500;
  color: #94a3b8;
}

nav a:hover {
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.05);
}

nav a.router-link-active {
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
}

.logout {
  border: none;
  background: rgba(244, 63, 94, 0.1);
  color: #fb7185;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s;
}

.logout:hover {
  background: rgba(244, 63, 94, 0.2);
  color: #fda4af;
}

.app-main {
  flex: 1;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1rem 4rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
