<template>
  <div class="feed">
    <HomeAdCarousel v-if="ads.length" :ads="ads" class="feed-carousel" />
    <div v-else-if="adsLoading" class="ads-placeholder">广告位加载中...</div>
    <p v-else-if="adsError" class="ads-error">{{ adsError }}</p>

    <PostComposer v-if="authStore.isAuthenticated" @posted="onPosted" />

    <HomeRecommendations />

    <section class="feed-list">
      <PostCard v-for="post in feedStore.posts" :key="post.id" :post="post" />
    </section>
    <div class="feed-actions">
      <button v-if="feedStore.hasMore" :disabled="feedStore.loading" @click="feedStore.fetchMore">
        {{ feedStore.loading ? "加载中..." : "加载更多" }}
      </button>
      <span v-else>已经到底啦 ✨</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { storeToRefs } from "pinia";
import PostComposer from "@/components/PostComposer.vue";
import PostCard from "@/components/PostCard.vue";
import HomeAdCarousel from "@/components/HomeAdCarousel.vue";
import HomeRecommendations from "@/components/HomeRecommendations.vue";
import { useFeedStore } from "@/stores/feed";
import { useAuthStore } from "@/stores/auth";
import { useHomeAdStore } from "@/stores/homeAds";

const feedStore = useFeedStore();
const authStore = useAuthStore();
const homeAdStore = useHomeAdStore();
const { ads, loading: adsLoading, error: adsError } = storeToRefs(homeAdStore);

const onPosted = () => {
  // 已在 store 中处理 prepend
};

onMounted(async () => {
  if (!feedStore.posts.length) {
    await feedStore.loadInitial();
  }
  void homeAdStore.fetchAds();
});
</script>

<style scoped>
.feed {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.feed-carousel {
  min-height: 220px;
}

.ads-placeholder,
.ads-error {
  padding: 1.25rem;
  border-radius: 1.25rem;
  border: 1px dashed rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.5);
  text-align: center;
  color: #cbd5f5;
}

.ads-error {
  border-color: rgba(252, 165, 165, 0.4);
  color: #fca5a5;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.feed-actions {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
  color: #94a3b8;
}

.feed-actions button {
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  color: inherit;
  padding: 0.5rem 1.5rem;
}
</style>
