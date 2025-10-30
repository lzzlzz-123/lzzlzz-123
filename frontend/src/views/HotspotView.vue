<template>
  <div class="hotspot">
    <header class="hotspot-header">
      <h2>热点聚焦</h2>
      <button @click="refresh" :disabled="loading">
        {{ loading && !posts.length ? "加载中..." : "刷新" }}
      </button>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <section class="hotspot-list">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
      <p v-if="!loading && !posts.length" class="empty">暂时没有热点内容</p>
    </section>
    <div class="actions">
      <button v-if="hasMore" :disabled="loading" @click="loadMore">
        {{ loading ? "加载中..." : "加载更多" }}
      </button>
      <span v-else>没有更多热点</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import PostCard from "@/components/PostCard.vue";
import api from "@/api/client";
import type { TimelinePost } from "@/stores/feed";
import { HOTSPOT_THRESHOLD } from "@/constants/heat";

const posts = ref<TimelinePost[]>([]);
const page = ref(0);
const size = 10;
const hasMore = ref(true);
const loading = ref(false);
const error = ref<string | null>(null);

const fetchHotspot = async (reset = false) => {
  if (loading.value) return;
  if (reset) {
    page.value = 0;
    hasMore.value = true;
    posts.value = [];
  }
  if (!hasMore.value) return;
  loading.value = true;
  error.value = null;
  try {
    const { data } = await api.get("/posts/hotspot", {
      params: { page: page.value, size },
    });
    if (page.value === 0) {
      posts.value = data.content;
    } else {
      posts.value.push(...data.content);
    }
    hasMore.value = !data.last;
    page.value += 1;
  } catch (err: any) {
    error.value = err?.response?.data?.message ?? "加载失败";
  } finally {
    loading.value = false;
  }
};

const refresh = () => fetchHotspot(true);
const loadMore = () => fetchHotspot(false);

watch(
  posts,
  () => {
    if (posts.value.some((post) => post.heat < HOTSPOT_THRESHOLD)) {
      posts.value = posts.value.filter((post) => post.heat >= HOTSPOT_THRESHOLD);
    }
  },
  { deep: true }
);

onMounted(() => {
  fetchHotspot(true);
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

.hotspot-header h2 {
  margin: 0;
  font-size: 1.35rem;
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
  margin: 2rem 0;
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
