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
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #e2e8f0;
}

header p {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: #94a3b8;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.bio h4 {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
  color: #cbd5f5;
}

.bio p {
  margin: 0;
  color: #e2e8f0;
  line-height: 1.6;
}

.empty {
  padding: 0.75rem 1rem;
  border-radius: 0.85rem;
  border: 1px dashed rgba(148, 163, 184, 0.25);
  color: #94a3b8;
  font-size: 0.9rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.label {
  font-size: 0.8rem;
  color: #94a3b8;
  letter-spacing: 0.02em;
}

.value {
  color: #e2e8f0;
  font-weight: 600;
}

.placeholder {
  color: #64748b;
}
</style>
