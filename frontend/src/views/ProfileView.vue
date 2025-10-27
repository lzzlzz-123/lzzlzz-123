<template>
  <div class="profile" v-if="profile">
    <header class="profile-header">
      <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="avatar" />
      <div class="meta">
        <h2>{{ profile.displayName }}</h2>
        <p>@{{ profile.username }}</p>
        <p class="bio" v-if="profile.bio">{{ profile.bio }}</p>
        <div class="stats">
          <span>粉丝 {{ profile.followerCount }}</span>
          <span>关注 {{ profile.followingCount }}</span>
          <span>发布 {{ profile.postCount }}</span>
        </div>
      </div>
      <button
        v-if="canFollow"
        class="follow-btn"
        @click="toggleFollow"
      >
        {{ profile.followedByCurrentUser ? "已关注" : "关注" }}
      </button>
    </header>

    <section class="posts">
      <h3>动态</h3>
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
      <div class="actions">
        <button v-if="hasMore" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? "加载中..." : "加载更多" }}
        </button>
        <span v-else>没有更多内容</span>
      </div>
    </section>
  </div>
  <div v-else class="placeholder">加载中...</div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PostCard from "@/components/PostCard.vue";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import type { TimelinePost } from "@/stores/feed";

interface ProfileResponse {
  id: number;
  username: string;
  displayName: string;
  email: string;
  bio: string | null;
  avatarUrl: string | null;
  createdAt: string;
  followerCount: number;
  followingCount: number;
  postCount: number;
  followedByCurrentUser: boolean;
}

const route = useRoute();
const authStore = useAuthStore();
const profile = ref<ProfileResponse | null>(null);
const posts = ref<TimelinePost[]>([]);
const page = ref(0);
const size = 10;
const hasMore = ref(true);
const loadingMore = ref(false);

const canFollow = computed(() => {
  if (!authStore.isAuthenticated || !profile.value) return false;
  return authStore.user?.id !== profile.value.id;
});

const fetchProfile = async (id: string | string[]) => {
  const { data } = await api.get(`/users/${id}`);
  profile.value = data;
};

const fetchPosts = async () => {
  if (!profile.value || loadingMore.value || !hasMore.value) return;
  loadingMore.value = true;
  try {
    const { data } = await api.get(`/posts/user/${profile.value.id}`, {
      params: { page: page.value, size },
    });
    if (page.value === 0) {
      posts.value = data.content;
    } else {
      posts.value.push(...data.content);
    }
    hasMore.value = !data.last;
    page.value += 1;
  } finally {
    loadingMore.value = false;
  }
};

const load = async () => {
  const id = route.params.id as string;
  page.value = 0;
  hasMore.value = true;
  posts.value = [];
  await fetchProfile(id);
  await fetchPosts();
};

const toggleFollow = async () => {
  if (!profile.value || !authStore.isAuthenticated) return;
  if (profile.value.followedByCurrentUser) {
    await api.delete(`/users/${profile.value.id}/follow`);
    profile.value.followedByCurrentUser = false;
    profile.value.followerCount -= 1;
  } else {
    await api.post(`/users/${profile.value.id}/follow`);
    profile.value.followedByCurrentUser = true;
    profile.value.followerCount += 1;
  }
};

const loadMore = () => fetchPosts();

watch(
  () => route.params.id,
  () => {
    load();
  }
);

onMounted(load);
</script>

<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.profile-header {
  display: flex;
  gap: 1.5rem;
  align-items: center;
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.profile-header img {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  object-fit: cover;
}

.meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.meta p {
  margin: 0;
  color: #94a3b8;
}

.bio {
  color: #e2e8f0 !important;
}

.stats {
  display: flex;
  gap: 1rem;
  color: #cbd5f5;
}

.follow-btn {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
}

.posts {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.actions {
  display: flex;
  justify-content: center;
  color: #94a3b8;
}

.placeholder {
  text-align: center;
  color: #94a3b8;
}
</style>
