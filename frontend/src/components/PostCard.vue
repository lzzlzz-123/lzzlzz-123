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

    <div class="post-flags" v-if="visibilityBadge">
      <span class="visibility" :class="visibilityClass">{{ visibilityBadge }}</span>
      <span v-if="visibilityDetail" class="visibility-detail">{{ visibilityDetail }}</span>
    </div>

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
      <button class="action-btn" @click.stop="toggleLike" :class="{ liked: post.likedByCurrentUser }">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <path d="m11.645 20.91-.007-.003-.022-.012a15.247 15.247 0 0 1-.383-.218 25.18 25.18 0 0 1-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3c1.74 0 3.285.834 4.312 2.133C13.027 3.834 14.572 3 16.312 3c2.974 0 5.438 2.322 5.438 5.25 0 3.924-2.438 7.11-4.74 9.273a25.177 25.177 0 0 1-4.244 3.17 15.237 15.237 0 0 1-.383.219l-.022.012-.007.004-.003.001Z" />
        </svg>
        <span>{{ post.likeCount }}</span>
      </button>
      <RouterLink :to="`/post/${post.id}`" class="action-btn" @click.stop>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <path fill-rule="evenodd" d="M4.848 2.771A49.144 49.144 0 0 1 12 2.25c2.43 0 4.817.178 7.152.52 1.978.292 3.348 2.024 3.348 3.97v6.02c0 1.946-1.37 3.678-3.348 3.97a48.901 48.901 0 0 1-3.476.383.39.39 0 0 0-.297.17l-2.755 4.133a.75.75 0 0 1-1.248 0l-2.755-4.133a.39.39 0 0 0-.297-.17 48.9 48.9 0 0 1-3.476-.384c-1.978-.29-3.348-2.024-3.348-3.97V6.741c0-1.946 1.37-3.68 3.348-3.97Z" clip-rule="evenodd" />
        </svg>
        <span>{{ post.commentCount }}</span>
      </RouterLink>
      <span class="heat-indicator" :class="{ hot: post.inHotspot }">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <path fill-rule="evenodd" d="M12.969 2.14a1.5 1.5 0 0 0-1.938 0c-3.97 3.358-5.108 7.75-4.517 11.27C6.523 13.423 6.5 13.461 6.5 13.5c.014.053.028.103.041.147A3.727 3.727 0 0 0 5.25 17c0 2.071 1.679 3.75 3.75 3.75a3.75 3.75 0 0 0 3.75-3.75c0-1.145-.512-2.17-1.32-2.853.066-.011.13-.024.193-.039a8.502 8.502 0 0 0 4.364-2.218c.138-.135.263-.273.375-.41C17.242 10.334 17.5 8.35 17.5 6.75a1.5 1.5 0 0 0-2.531-1.08l-2 2a.75.75 0 0 1-1.249-.56l.249-5z" clip-rule="evenodd" />
        </svg>
        {{ post.heat }}
        <small v-if="post.inHotspot">热点</small>
      </span>
      <div v-if="canManage" class="manage-actions">
        <button type="button" class="manage-btn ghost" :disabled="busy" @click.stop="emitEdit">
          编辑
        </button>
        <button type="button" class="manage-btn danger" :disabled="busy" @click.stop="emitDelete">
          删除
        </button>
      </div>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useFeedStore } from "@/stores/feed";
import type { TimelinePost } from "@/types/post";

const props = defineProps<{
  post: TimelinePost;
  busy?: boolean;
}>();

const emit = defineEmits<{
  (event: "edit", post: TimelinePost): void;
  (event: "delete", post: TimelinePost): void;
}>();

const feedStore = useFeedStore();
const router = useRouter();

const VIDEO_EXTENSIONS = ["mp4", "webm", "ogg", "ogv", "mov", "m4v", "avi"] as const;
const isVideo = (url: string) => {
  if (!url) return false;
  const clean = url.split(/[?#]/)[0];
  const extension = clean.split(".").pop()?.toLowerCase();
  return extension ? VIDEO_EXTENSIONS.includes(extension as (typeof VIDEO_EXTENSIONS)[number]) : false;
};

const formattedTime = computed(() => new Date(props.post.createdAt).toLocaleString());
const canManage = computed(() => Boolean(props.post.ownedByCurrentUser));
const busy = computed(() => Boolean(props.busy));

const visibilityBadge = computed(() => {
  switch (props.post.visibility) {
    case "FOLLOWERS_ONLY":
      return "粉丝可见";
    case "PRIVATE":
      return "仅自己可见";
    case "CUSTOM":
      return "指定人可见";
    default:
      return "";
  }
});

const visibilityClass = computed(() => {
  const visibility = props.post.visibility?.toLowerCase?.();
  return visibility ? `visibility-${visibility}` : "";
});

const visibilityDetail = computed(() => {
  if (props.post.visibility !== "CUSTOM") {
    return "";
  }
  const count = props.post.allowedUserIds?.length ?? 0;
  return count > 0 ? `已指定 ${count} 人可见` : "";
});

const toggleLike = async () => {
  await feedStore.toggleLike(props.post.id, props.post);
};

const openDetail = () => {
  router.push({ name: "post", params: { id: props.post.id } });
};

const emitEdit = () => {
  emit("edit", props.post);
};

const emitDelete = () => {
  emit("delete", props.post);
};
</script>

<style scoped>
.post-card {
  background: rgba(30, 41, 59, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.post-card:hover {
  border-color: rgba(56, 189, 248, 0.4);
  background: rgba(30, 41, 59, 0.6);
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.2), 0 4px 6px -2px rgba(0, 0, 0, 0.1);
}

header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

header img {
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  object-fit: cover;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.meta strong {
  font-size: 1.05rem;
  color: #f8fafc;
}

.meta span {
  color: #64748b;
  font-size: 0.875rem;
}

time {
  margin-left: auto;
  color: #64748b;
  font-size: 0.8rem;
}

.post-flags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: -0.5rem;
}

.visibility {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.visibility-followers_only {
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
  border-color: rgba(56, 189, 248, 0.1);
}

.visibility-private {
  background: rgba(148, 163, 184, 0.15);
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.1);
}

.visibility-custom {
  background: rgba(139, 92, 246, 0.1);
  color: #a78bfa;
  border-color: rgba(139, 92, 246, 0.1);
}

.content {
  margin: 0;
  line-height: 1.7;
  font-size: 1.05rem;
  color: #e2e8f0;
  white-space: pre-line;
}

.topic-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  align-self: flex-start;
  padding: 0.25rem 0.75rem;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.topic-pill:hover {
  background: rgba(56, 189, 248, 0.2);
  border-color: rgba(56, 189, 248, 0.3);
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.75rem;
  border-radius: 1rem;
  overflow: hidden;
}

.media-item {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.media-item img,
.media-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.media-item:hover img {
  transform: scale(1.05);
}

footer {
  display: flex;
  gap: 1.5rem;
  align-items: center;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  background: transparent;
  color: #64748b;
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 0.2s;
  padding: 0.5rem;
  margin: -0.5rem;
  border-radius: 0.5rem;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #f8fafc;
}

.action-btn.liked {
  color: #f43f5e;
}

.action-btn.liked:hover {
  background: rgba(244, 63, 94, 0.1);
}

.heat-indicator {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: #f97316;
  font-weight: 700;
  font-size: 0.9rem;
  padding: 0.25rem 0.75rem;
  background: rgba(249, 115, 22, 0.1);
  border-radius: 999px;
}

.heat-indicator.hot {
  color: #fb923c;
  background: rgba(251, 146, 60, 0.15);
  box-shadow: 0 0 10px rgba(251, 146, 60, 0.2);
}

.heat-indicator small {
  margin-left: 0.25rem;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
}

.manage-actions {
  display: inline-flex;
  gap: 0.5rem;
}

.manage-btn {
  border-radius: 0.5rem;
  padding: 0.4rem 0.8rem;
  font-size: 0.8rem;
  font-weight: 600;
  transition: all 0.2s;
}

.manage-btn.ghost {
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #94a3b8;
  background: transparent;
}

.manage-btn.ghost:hover {
  background: rgba(148, 163, 184, 0.1);
  color: #f8fafc;
  border-color: rgba(148, 163, 184, 0.4);
}

.manage-btn.danger {
  border: 1px solid rgba(244, 63, 94, 0.2);
  color: #f43f5e;
  background: rgba(244, 63, 94, 0.05);
}

.manage-btn.danger:hover {
  background: rgba(244, 63, 94, 0.15);
  color: #fb7185;
  border-color: rgba(244, 63, 94, 0.4);
}
</style>
