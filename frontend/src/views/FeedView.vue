<template>
  <div class="feed">
    <PostComposer v-if="authStore.isAuthenticated" @posted="onPosted" />
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
import PostComposer from "@/components/PostComposer.vue";
import PostCard from "@/components/PostCard.vue";
import { useFeedStore } from "@/stores/feed";
import { useAuthStore } from "@/stores/auth";

const feedStore = useFeedStore();
const authStore = useAuthStore();

const onPosted = () => {
  // Already handled in store by prepend
};

onMounted(async () => {
  if (!feedStore.posts.length) {
    await feedStore.loadInitial();
  }
});
</script>

<style scoped>
.feed {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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
