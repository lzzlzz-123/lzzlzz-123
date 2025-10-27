<template>
  <div class="auth-card">
    <h2>登录</h2>
    <form @submit.prevent="submit">
      <label>
        用户名或邮箱
        <input v-model="usernameOrEmail" type="text" required />
      </label>
      <label>
        密码
        <input v-model="password" type="password" required />
      </label>
      <button type="submit" :disabled="authStore.status === 'loading'">
        {{ authStore.status === "loading" ? "登录中..." : "登录" }}
      </button>
      <p v-if="authStore.error" class="error">{{ authStore.error }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const usernameOrEmail = ref("");
const password = ref("");
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const submit = async () => {
  await authStore.login({ usernameOrEmail: usernameOrEmail.value, password: password.value });
  const redirect = (route.query.redirect as string) ?? "/";
  router.replace(redirect);
};
</script>

<style scoped>
.auth-card {
  margin: 0 auto;
  max-width: 420px;
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.25rem;
  padding: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: #cbd5f5;
}

input {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.75rem 1rem;
}

button {
  border: none;
  border-radius: 999px;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
}

.error {
  color: #fca5a5;
}
</style>
