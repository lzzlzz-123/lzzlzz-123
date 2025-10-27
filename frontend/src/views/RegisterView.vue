<template>
  <div class="auth-card">
    <h2>注册</h2>
    <form @submit.prevent="submit">
      <label>
        用户名
        <input v-model="username" type="text" minlength="3" maxlength="50" required />
      </label>
      <label>
        昵称
        <input v-model="displayName" type="text" minlength="1" maxlength="100" required />
      </label>
      <label>
        邮箱
        <input v-model="email" type="email" required />
      </label>
      <label>
        密码
        <input v-model="password" type="password" minlength="8" maxlength="64" required />
      </label>
      <button type="submit" :disabled="authStore.status === 'loading'">
        {{ authStore.status === "loading" ? "注册中..." : "注册" }}
      </button>
      <p v-if="authStore.error" class="error">{{ authStore.error }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const username = ref("");
const displayName = ref("");
const email = ref("");
const password = ref("");
const authStore = useAuthStore();
const router = useRouter();

const submit = async () => {
  await authStore.register({
    username: username.value,
    displayName: displayName.value,
    email: email.value,
    password: password.value,
  });
  router.replace("/");
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
