<template>
  <div class="profile" v-if="profile">
    <header class="profile-header">
      <div class="avatar">
        <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="avatar" />
        <div v-else class="avatar-fallback">{{ profile.displayName.slice(0, 1) }}</div>
      </div>
      <div class="meta">
        <h2>{{ profile.displayName }}</h2>
        <p>@{{ profile.username }}</p>
        <p class="bio" v-if="profile.bio">{{ profile.bio }}</p>
        <p class="signature" v-if="profile.signature">“{{ profile.signature }}”</p>
        <p class="location" v-if="profile.location">📍 {{ profile.location }}</p>
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
      <button
        v-else-if="canEdit"
        class="edit-btn"
        @click="toggleEdit"
      >
        {{ isEditing ? "取消" : "编辑资料" }}
      </button>
    </header>

    <section v-if="canEdit && isEditing" class="edit-section">
      <h3>编辑资料</h3>
      <form @submit.prevent="submitProfile" class="profile-form">
        <div class="form-row">
          <label for="profile-display-name">昵称</label>
          <input id="profile-display-name" v-model="form.displayName" type="text" maxlength="100" required />
        </div>
        <div class="form-row">
          <label for="profile-avatar">头像地址</label>
          <input id="profile-avatar" v-model="form.avatarUrl" type="url" maxlength="255" placeholder="https://example.com/avatar.png" />
        </div>
        <div class="form-row">
          <label for="profile-bio">个人简介</label>
          <textarea id="profile-bio" v-model="form.bio" rows="3" maxlength="280" placeholder="介绍一下自己"></textarea>
        </div>
        <div class="form-row">
          <label for="profile-signature">个性签名</label>
          <input id="profile-signature" v-model="form.signature" type="text" maxlength="280" placeholder="写一句话表达自己" />
        </div>
        <div class="form-row">
          <label for="profile-location">所在地</label>
          <input id="profile-location" v-model="form.location" type="text" maxlength="120" placeholder="你在哪里" />
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <div class="form-actions">
          <button type="submit" :disabled="updatingProfile">
            {{ updatingProfile ? "保存中..." : "保存资料" }}
          </button>
        </div>
      </form>
    </section>

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
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PostCard from "@/components/PostCard.vue";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { useHotspotStore } from "@/stores/hotspot";
import type { TimelinePost } from "@/types/post";

interface ProfileResponse {
  id: number;
  username: string;
  displayName: string;
  email: string;
  bio: string | null;
  signature: string | null;
  location: string | null;
  avatarUrl: string | null;
  createdAt: string;
  followerCount: number;
  followingCount: number;
  postCount: number;
  followedByCurrentUser: boolean;
}

const route = useRoute();
const authStore = useAuthStore();
const hotspotStore = useHotspotStore();
const profile = ref<ProfileResponse | null>(null);
const posts = ref<TimelinePost[]>([]);
const page = ref(0);
const size = 10;
const hasMore = ref(true);
const loadingMore = ref(false);
const isEditing = ref(false);
const updatingProfile = ref(false);
const formError = ref<string | null>(null);
const form = reactive({
  displayName: "",
  avatarUrl: "",
  bio: "",
  signature: "",
  location: "",
});

const canFollow = computed(() => {
  if (!authStore.isAuthenticated || !profile.value) return false;
  return authStore.user?.id !== profile.value.id;
});

const canEdit = computed(() => {
  if (!authStore.isAuthenticated || !profile.value) return false;
  return authStore.user?.id === profile.value.id;
});

const fillForm = (value: ProfileResponse) => {
  form.displayName = value.displayName ?? "";
  form.avatarUrl = value.avatarUrl ?? "";
  form.bio = value.bio ?? "";
  form.signature = value.signature ?? "";
  form.location = value.location ?? "";
};

const toggleEdit = () => {
  if (!profile.value) return;
  if (!isEditing.value) {
    fillForm(profile.value);
    formError.value = null;
  }
  isEditing.value = !isEditing.value;
};

const submitProfile = async () => {
  if (!profile.value) return;
  if (!form.displayName.trim()) {
    formError.value = "昵称不能为空";
    return;
  }
  updatingProfile.value = true;
  formError.value = null;
  try {
    const payload = {
      displayName: form.displayName.trim(),
      avatarUrl: form.avatarUrl.trim() ? form.avatarUrl.trim() : null,
      bio: form.bio.trim() ? form.bio.trim() : null,
      signature: form.signature.trim() ? form.signature.trim() : null,
      location: form.location.trim() ? form.location.trim() : null,
    };
    const { data } = await api.put<ProfileResponse>("/users/me", payload);
    profile.value = data;
    fillForm(data);
    isEditing.value = false;
    await authStore.fetchCurrentUser().catch(() => undefined);
  } catch (error: any) {
    formError.value = error?.response?.data?.message ?? "更新失败";
  } finally {
    updatingProfile.value = false;
  }
};

const fetchProfile = async (id: string | string[]) => {
  const { data } = await api.get(`/users/${id}`);
  profile.value = data;
  if (authStore.user?.id === data.id) {
    fillForm(data);
  }
};

const fetchPosts = async () => {
  if (!profile.value || loadingMore.value || !hasMore.value) return;
  loadingMore.value = true;
  try {
    const { data } = await api.get(`/posts/user/${profile.value.id}`, {
      params: { page: page.value, size },
    });
    const fetched: TimelinePost[] = data.content ?? [];
    if (page.value === 0) {
      posts.value = fetched.map((item) => ({ ...item }));
    } else {
      fetched.forEach((item) => {
        const existing = posts.value.find((post) => post.id === item.id);
        if (existing) {
          Object.assign(existing, item);
        } else {
          posts.value.push(item);
        }
      });
    }
    hotspotStore.syncPosts(fetched);
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
  isEditing.value = false;
  formError.value = null;
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
  align-items: flex-start;
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.avatar {
  width: 104px;
  height: 104px;
  flex-shrink: 0;
}

.avatar img,
.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 700;
  font-size: 2.25rem;
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

.signature {
  color: #f1f5f9;
  font-style: italic;
}

.location {
  color: #cbd5f5;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
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

.edit-btn {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: rgba(148, 163, 184, 0.15);
  color: #e2e8f0;
  font-weight: 600;
  transition: background 0.3s;
}

.edit-btn:hover {
  background: rgba(148, 163, 184, 0.3);
}

.edit-section {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-row label {
  font-weight: 600;
  color: #cbd5f5;
}

.form-row input,
.form-row textarea {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.65rem 0.9rem;
  resize: vertical;
}

.form-row input:focus,
.form-row textarea:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.form-error {
  color: #fda4af;
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.form-actions button {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
}

.form-actions button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
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
