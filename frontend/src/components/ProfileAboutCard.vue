<template>
  <section class="about-card">
    <header>
      <h3>个人档案</h3>
      <p>展示你的个性与所在位置</p>
    </header>
    <div class="body">
      <div class="bio" v-if="bio">
        <h4>个人简介</h4>
        <p>{{ bio }}</p>
      </div>
      <div v-else class="empty">暂未填写简介</div>

      <div class="grid">
        <div class="item">
          <span class="label">个性签名</span>
          <span class="value" v-if="signature">“{{ signature }}”</span>
          <span v-else class="placeholder">暂无签名</span>
        </div>
        <div class="item">
          <span class="label">所在城市</span>
          <span class="value" v-if="location">
            📍 {{ location }}
          </span>
          <span v-else class="placeholder">还没有填写</span>
        </div>
        <div class="item">
          <span class="label">注册时间</span>
          <span class="value">{{ formattedJoinDate }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  bio?: string | null;
  signature?: string | null;
  location?: string | null;
  createdAt?: string | null;
}>();

const formattedJoinDate = computed(() => {
  if (!props.createdAt) {
    return "未知";
  }
  const date = new Date(props.createdAt);
  if (Number.isNaN(date.getTime())) {
    return "未知";
  }
  return date.toLocaleDateString();
});
</script>

<style scoped>
.about-card {
  background: rgba(30, 41, 59, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #f8fafc;
}

header p {
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
  color: #64748b;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.bio h4 {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: #cbd5e1;
}

.bio p {
  margin: 0;
  color: #e2e8f0;
  line-height: 1.7;
  font-size: 1.05rem;
}

.empty {
  padding: 1.25rem;
  border-radius: 1rem;
  border: 1px dashed rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.2);
  color: #64748b;
  font-size: 0.95rem;
  text-align: center;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.2s;
}

.item:hover {
  background: rgba(15, 23, 42, 0.5);
  border-color: rgba(56, 189, 248, 0.2);
  transform: translateY(-2px);
}

.label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.value {
  color: #f8fafc;
  font-weight: 600;
  font-size: 1rem;
}

.placeholder {
  color: #475569;
}
</style>
