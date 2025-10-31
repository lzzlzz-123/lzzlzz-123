<template>
  <div v-if="post" class="post-detail">
    <PostCard :post="post" />
    <section class="comments">
      <h3>评论</h3>
      <form v-if="authStore.isAuthenticated" @submit.prevent="submitComment">
        <textarea v-model="commentContent" rows="3" placeholder="发表你的看法"></textarea>
        <button type="submit" :disabled="postingComment || !commentContent.trim()">
          {{ postingComment ? "发布中..." : "发布评论" }}
        </button>
      </form>
      <ul>
        <li v-for="comment in comments" :key="comment.id">
          <div class="header">
            <strong>{{ comment.author.displayName }}</strong>
            <span>@{{ comment.author.username }}</span>
            <time>{{ new Date(comment.createdAt).toLocaleString() }}</time>
          </div>
          <p>{{ comment.content }}</p>
        </li>
      </ul>
    </section>
  </div>
  <div v-else class="placeholder">加载中...</div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PostCard from "@/components/PostCard.vue";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { useFeedStore } from "@/stores/feed";
import { useHotspotStore } from "@/stores/hotspot";
import type { TimelinePost } from "@/types/post";
import { COMMENT_HEAT_WEIGHT, HOTSPOT_THRESHOLD } from "@/constants/heat";

interface CommentResponse {
  id: number;
  content: string;
  createdAt: string;
  author: {
    id: number;
    username: string;
    displayName: string;
    avatarUrl?: string | null;
  };
}

const route = useRoute();
const authStore = useAuthStore();
const feedStore = useFeedStore();
const hotspotStore = useHotspotStore();
const post = ref<TimelinePost | null>(null);
const comments = ref<CommentResponse[]>([]);
const commentContent = ref("");
const postingComment = ref(false);

const syncWithFeedStore = (updated: TimelinePost) => {
  const target = feedStore.posts.find((p) => p.id === updated.id);
  if (target) {
    Object.assign(target, updated);
  }
};

const fetchPost = async (id: string) => {
  const { data } = await api.get<TimelinePost>(`/posts/${id}`);
  post.value = data;
  syncWithFeedStore(data);
  hotspotStore.syncPost(data);
};

const fetchComments = async (id: string) => {
  const { data } = await api.get(`/posts/${id}/comments`);
  comments.value = data;
};

const load = async () => {
  const id = route.params.id as string;
  await Promise.all([fetchPost(id), fetchComments(id)]);
};

const submitComment = async () => {
  if (!post.value) return;
  postingComment.value = true;
  try {
    const { data } = await api.post<CommentResponse>(`/posts/${post.value.id}/comments`, {
      content: commentContent.value,
    });
    comments.value.push(data);
    commentContent.value = "";
    if (post.value) {
      post.value.commentCount += 1;
      post.value.heat += COMMENT_HEAT_WEIGHT;
      post.value.inHotspot = post.value.heat >= HOTSPOT_THRESHOLD;
      post.value.updatedAt = new Date().toISOString();
      syncWithFeedStore(post.value);
      hotspotStore.syncPost(post.value);
    }
  } finally {
    postingComment.value = false;
  }
};

watch(
  () => route.params.id,
  () => load()
);

onMounted(load);
</script>

<style scoped>
.post-detail {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.comments {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

textarea {
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.75rem 1rem;
  resize: none;
}

button {
  align-self: flex-end;
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
}

ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

li {
  background: rgba(15, 23, 42, 0.5);
  border-radius: 1rem;
  padding: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #cbd5f5;
}

.header time {
  margin-left: auto;
  color: #94a3b8;
}

.placeholder {
  text-align: center;
  color: #94a3b8;
}
</style>
