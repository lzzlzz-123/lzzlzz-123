<template>
  <section v-if="shouldDisplay" class="home-recommendations">
    <header class="recommendations-header">
      <h3>为你推荐</h3>
      <button type="button" :disabled="refreshDisabled" @click="refresh">
        {{ refreshLabel }}
      </button>
    </header>
    <p v-if="error" class="recommendations-error">{{ error }}</p>
    <div v-if="showLoading" class="recommendations-placeholder">推荐内容加载中...</div>
    <div v-else-if="posts.length" class="recommendations-list">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
    </div>
    <p v-else-if="!error" class="recommendations-empty">暂无推荐内容</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import PostCard from "@/components/PostCard.vue";
import { useRecommendationStore } from "@/stores/recommendations";

const recommendationStore = useRecommendationStore();
const { posts, loading, refreshing, error, initialized } = storeToRefs(recommendationStore);

const showLoading = computed(() => loading.value && !initialized.value);
const isRefreshing = computed(() => refreshing.value || (loading.value && initialized.value));
const refreshDisabled = computed(() => showLoading.value || isRefreshing.value);
const refreshLabel = computed(() => (isRefreshing.value ? "刷新中..." : "换一批"));
const shouldDisplay = computed(() => showLoading.value || posts.value.length > 0 || Boolean(error.value));

const refresh = () => {
  if (refreshDisabled.value) return;
  void recommendationStore.refresh();
};

onMounted(() => {
  void recommendationStore.loadInitial();
});
</script>

<style scoped>
.home-recommendations {
  background: rgba(30, 41, 59, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.recommendations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.recommendations-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  background: linear-gradient(to right, #f8fafc, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.recommendations-header button {
  border: none;
  border-radius: 0.75rem;
  padding: 0.5rem 1.25rem;
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.recommendations-header button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.recommendations-header button:not(:disabled):hover {
  background: rgba(56, 189, 248, 0.2);
  transform: translateY(-1px);
}

.recommendations-error {
  margin: 0;
  color: #fb7185;
  font-size: 0.9rem;
  font-weight: 500;
}

.recommendations-placeholder,
.recommendations-empty {
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
  text-align: center;
  padding: 2rem 0;
}

.recommendations-list {
  display: grid;
  gap: 1rem;
}

@media (min-width: 640px) {
  .recommendations-list {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }
}
</style>
