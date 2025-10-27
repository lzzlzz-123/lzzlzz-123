<template>
  <article class="post-card" @click="openDetail">
    <header>
      <img v-if="post.author.avatarUrl" :src="post.author.avatarUrl" alt="avatar" />
      <div class="meta">
        <strong>{{ post.author.displayName }}</strong>
        <span>@{{ post.author.username }}</span>
      </div>
      <time>{{ formattedTime }}</time>
    </header>
    <p class="content">{{ post.content }}</p>
    <div v-if="post.mediaUrls?.length" class="media-grid">
      <img v-for="(url, index) in post.mediaUrls" :key="index" :src="url" :alt="`media-${index}`" @click.stop />
    </div>
    <footer>
      <button @click.stop="toggleLike">
        <span :class="{ liked: post.likedByCurrentUser }">♥</span>
        <span>{{ post.likeCount }}</span>
      </button>
      <RouterLink :to="`/post/${post.id}`" @click.stop>
        💬
        <span>{{ post.commentCount }}</span>
      </RouterLink>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useFeedStore, type TimelinePost } from "@/stores/feed";

const props = defineProps<{ post: TimelinePost }>();
const feedStore = useFeedStore();
const router = useRouter();

const formattedTime = computed(() => new Date(props.post.createdAt).toLocaleString());

const toggleLike = async () => {
  await feedStore.toggleLike(props.post.id, props.post);
};

const openDetail = () => {
  router.push({ name: "post", params: { id: props.post.id } });
};
</script>

<style scoped>
.post-card {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: border 0.3s, transform 0.3s;
}

.post-card:hover {
  border-color: rgba(99, 102, 241, 0.4);
  transform: translateY(-2px);
}

header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

header img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.meta span {
  color: #94a3b8;
  font-size: 0.85rem;
}

time {
  margin-left: auto;
  color: #94a3b8;
  font-size: 0.85rem;
}

.content {
  margin: 0;
  line-height: 1.6;
  white-space: pre-line;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.5rem;
}

.media-grid img {
  width: 100%;
  border-radius: 0.75rem;
  object-fit: cover;
}

footer {
  display: flex;
  gap: 1rem;
  align-items: center;
}

footer button,
footer a {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: none;
  background: transparent;
  color: #cbd5f5;
  font-weight: 500;
  font-size: 0.95rem;
}

footer button span.liked {
  color: #fda4af;
}
</style>
