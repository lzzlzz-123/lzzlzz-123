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
    <RouterLink
      v-if="post.topic"
      class="topic-pill"
      :to="{ name: 'topic-detail', params: { id: post.topic.id } }"
      @click.stop
    >
      #{{ post.topic.name }}
    </RouterLink>
    <div v-if="post.mediaUrls?.length" class="media-grid">
      <div v-for="(url, index) in post.mediaUrls" :key="index" class="media-item">
        <video v-if="isVideo(url)" controls :src="url" @click.stop></video>
        <img v-else :src="url" :alt="`media-${index}`" @click.stop />
      </div>
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
      <span class="heat-indicator" :class="{ hot: post.inHotspot }">
        🔥 {{ post.heat }}
        <small v-if="post.inHotspot">热点</small>
      </span>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useFeedStore } from "@/stores/feed";
import type { TimelinePost } from "@/types/post";

const props = defineProps<{ post: TimelinePost }>();
const feedStore = useFeedStore();
const router = useRouter();

const VIDEO_EXTENSIONS = ["mp4", "webm", "ogg", "ogv", "mov", "m4v", "avi"] as const;
const isVideo = (url: string) => {
  if (!url) return false;
  const clean = url.split(/[?#]/)[0];
  const extension = clean.split(".").pop()?.toLowerCase();
  return extension ? VIDEO_EXTENSIONS.includes(extension as typeof VIDEO_EXTENSIONS[number]) : false;
};

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

.topic-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  align-self: flex-start;
  margin-top: -0.25rem;
  margin-bottom: 0.25rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.85rem;
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
}

.media-item {
  position: relative;
  overflow: hidden;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
}

.media-item img,
.media-item video {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.media-item video {
  background: #0f172a;
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

.heat-indicator {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: #f97316;
  font-weight: 600;
  font-size: 0.95rem;
}

.heat-indicator.hot {
  color: #fb923c;
}

.heat-indicator small {
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.2);
  font-size: 0.7rem;
  color: inherit;
}
</style>
