<template>
  <div class="hotspot">
    <header class="hotspot-header">
      <h2>热点聚焦</h2>
      <button @click="refresh" :disabled="loading || rankingLoading">
        {{ isInitialLoading ? "加载中..." : "刷新" }}
      </button>
    </header>
    <div class="hotspot-content">
      <section class="ranking-section">
        <div class="section-header">
          <h3>热度榜 TOP20</h3>
          <button type="button" @click="refreshRanking" :disabled="rankingLoading">
            {{ rankingLoading ? "刷新中..." : "更新榜单" }}
          </button>
        </div>
        <p v-if="rankingError" class="error">{{ rankingError }}</p>
        <ol v-if="ranking.length" class="ranking-list">
          <li v-for="(item, index) in ranking" :key="item.id">
            <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <div class="ranking-body">
              <RouterLink :to="`/post/${item.id}`" class="ranking-title">
                <strong>{{ item.author.displayName }}</strong>
                <span class="username">@{{ item.author.username }}</span>
              </RouterLink>
              <RouterLink :to="`/post/${item.id}`" class="ranking-snippet">
                {{ snippet(item.content) }}
              </RouterLink>
              <div class="ranking-meta">
                <span>🔥 {{ item.heat }}</span>
                <span>♥ {{ item.likeCount }}</span>
              </div>
            </div>
          </li>
        </ol>
        <p v-else-if="rankingLoading" class="empty">榜单加载中...</p>
        <p v-else class="empty">暂无热度榜数据</p>
      </section>
      <section class="hotspot-feed">
        <p v-if="error" class="error">{{ error }}</p>
        <section class="hotspot-list">
          <PostCard v-for="post in posts" :key="post.id" :post="post" />
          <p v-if="isInitialLoading" class="empty">加载中...</p>
          <p v-else-if="isEmpty" class="empty">暂时没有热点内容</p>
        </section>
        <div class="actions">
          <button v-if="hasMore" :disabled="loading" @click="loadMore">
            {{ loading ? "加载中..." : "加载更多" }}
          </button>
          <span v-else>没有更多热点</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { RouterLink } from "vue-router";
import { storeToRefs } from "pinia";
import PostCard from "@/components/PostCard.vue";
import { useHotspotStore } from "@/stores/hotspot";

const hotspotStore = useHotspotStore();
const { posts, loading, hasMore, error, ranking, rankingLoading, rankingError } = storeToRefs(hotspotStore);

const isInitialLoading = computed(() => loading.value && posts.value.length === 0);
const isEmpty = computed(() => !loading.value && posts.value.length === 0);

const snippet = (content: string) => {
  const trimmed = content.trim();
  return trimmed.length > 60 ? `${trimmed.slice(0, 60)}...` : trimmed || "(无内容)";
};

const refresh = () => hotspotStore.refresh();
const refreshRanking = () => hotspotStore.fetchRanking(true);
const loadMore = () => hotspotStore.fetchMore();

onMounted(() => {
  void hotspotStore.loadInitial();
  void hotspotStore.fetchRankingIfNeeded();
});
</script>

<style scoped>
.hotspot {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.hotspot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.hotspot-header button {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #f97316, #facc15);
  color: #0f172a;
  font-weight: 600;
}

.hotspot-header button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.hotspot-content {
  display: grid;
  grid-template-columns: minmax(0, 320px) minmax(0, 1fr);
  gap: 1.5rem;
}

@media (max-width: 960px) {
  .hotspot-content {
    grid-template-columns: 1fr;
  }
}

.ranking-section,
.hotspot-feed {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  margin: 0;
  font-size: 1.1rem;
}

.section-header button {
  border: none;
  border-radius: 999px;
  padding: 0.4rem 1rem;
  background: rgba(99, 102, 241, 0.25);
  color: #c7d2fe;
  font-size: 0.85rem;
}

.section-header button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ranking-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.ranking-list li {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  background: rgba(15, 23, 42, 0.45);
  border-radius: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.rank {
  font-size: 1.2rem;
  font-weight: 700;
  color: #94a3b8;
  min-width: 28px;
  text-align: center;
}

.rank.top {
  color: #f97316;
}

.ranking-body {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1;
}

.ranking-title {
  display: inline-flex;
  gap: 0.5rem;
  align-items: baseline;
  color: inherit;
  text-decoration: none;
}

.ranking-title strong {
  font-size: 1rem;
}

.username {
  color: #94a3b8;
  font-size: 0.85rem;
}

.ranking-snippet {
  color: #cbd5f5;
  font-size: 0.9rem;
  text-decoration: none;
  line-height: 1.4;
}

.ranking-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.85rem;
  color: #f97316;
}

.error {
  margin: 0;
  color: #fda4af;
  text-align: center;
}

.hotspot-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.empty {
  text-align: center;
  color: #94a3b8;
  margin: 1.5rem 0;
}

.actions {
  display: flex;
  justify-content: center;
  color: #94a3b8;
}

.actions button {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: rgba(148, 163, 184, 0.15);
  color: inherit;
}

.actions button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
