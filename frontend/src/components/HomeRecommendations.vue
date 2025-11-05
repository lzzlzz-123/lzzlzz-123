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
  background: rgba(15, 23, 42, 0.65);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.recommendations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.recommendations-header h3 {
  margin: 0;
  font-size: 1.15rem;
}

.recommendations-header button {
  border: none;
  border-radius: 999px;
  padding: 0.4rem 1.25rem;
  background: rgba(99, 102, 241, 0.2);
  color: #c7d2fe;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.recommendations-header button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.recommendations-header button:not(:disabled):hover {
  background: rgba(99, 102, 241, 0.35);
}

.recommendations-error {
  margin: 0;
  color: #fca5a5;
}

.recommendations-placeholder,
.recommendations-empty {
  margin: 0;
  color: #94a3b8;
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
