<template>
  <div class="profile" v-if="profile">
    <header class="profile-header">
      <div class="avatar">
        <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="avatar" />
        <div v-else class="avatar-fallback">{{ profile.displayName.slice(0, 1) }}</div>
      </div>
      <div class="meta">
        <div class="meta-header">
          <h2>{{ profile.displayName }}</h2>
          <span class="privacy-chip" :class="privacyChipClass">{{ privacyLabel }}</span>
        </div>
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
      <div class="actions-stack">
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
      </div>
    </header>

    <ProfileAboutCard
      class="about-card-wrapper"
      :bio="profile.bio"
      :signature="profile.signature"
      :location="profile.location"
      :created-at="profile.createdAt"
    />

    <section v-if="canEdit && isEditing" class="edit-section">
      <h3>编辑资料</h3>
      <form @submit.prevent="submitProfile" class="profile-form">
        <div class="form-row">
          <label for="profile-display-name">昵称</label>
          <input id="profile-display-name" v-model="form.displayName" type="text" maxlength="100" required />
        </div>
        <div class="form-row avatar-row">
          <label for="profile-avatar">头像</label>
          <div class="avatar-field">
            <input
              id="profile-avatar"
              v-model="form.avatarUrl"
              type="url"
              maxlength="255"
              placeholder="https://example.com/avatar.png"
            />
            <div class="avatar-actions">
              <button type="button" @click="pickAvatarFile" :disabled="avatarUploading">
                {{ avatarUploading ? "上传中..." : "本地上传" }}
              </button>
              <button
                v-if="form.avatarUrl"
                type="button"
                class="ghost"
                @click="clearAvatar"
                :disabled="avatarUploading"
              >
                清除
              </button>
              <input
                ref="avatarFileInput"
                type="file"
                accept="image/*"
                hidden
                @change="onAvatarFileSelected"
              />
            </div>
            <p v-if="avatarUploadError" class="form-hint error">{{ avatarUploadError }}</p>
            <div v-if="form.avatarUrl" class="avatar-preview">
              <img :src="form.avatarUrl" alt="avatar preview" />
            </div>
          </div>
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
        <div class="form-row">
          <label>主页隐私</label>
          <PrivacySettingSelector v-model="form.privacySetting" />
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <div class="form-actions">
          <button type="submit" :disabled="updatingProfile">
            {{ updatingProfile ? "保存中..." : "保存资料" }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="canEdit && managingPost" class="post-manage">
      <div class="post-manage-header">
        <h3>编辑动态</h3>
        <button type="button" class="close-btn" @click="cancelPostEdit">×</button>
      </div>
      <form @submit.prevent="submitPostUpdate" class="post-form">
        <label class="field-label" for="post-content">动态内容</label>
        <textarea
          id="post-content"
          v-model="postForm.content"
          rows="4"
          maxlength="500"
          placeholder="更新动态内容"
        ></textarea>
        <div class="visibility-wrapper">
          <h4>可见范围</h4>
          <PostVisibilityControl
            v-model="postForm.visibility"
            v-model:allowedUserIds="postForm.allowedUserIds"
            :connections="availableConnections"
          />
        </div>
        <p v-if="postFormError" class="form-error">{{ postFormError }}</p>
        <div class="form-actions">
          <button type="button" class="ghost" @click="cancelPostEdit">取消</button>
          <button type="submit" :disabled="updatingPost">
            {{ updatingPost ? "保存中..." : "保存动态" }}
          </button>
        </div>
      </form>
    </section>

    <section class="posts">
      <div class="posts-header">
        <h3>动态</h3>
        <span>{{ profile.postCount }} 条</span>
      </div>
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        :busy="isPostBusy(post.id)"
        @edit="startPostEdit"
        @delete="confirmDeletePost"
      />
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
import { storeToRefs } from "pinia";
import { useRoute } from "vue-router";
import PostCard from "@/components/PostCard.vue";
import PostVisibilityControl from "@/components/PostVisibilityControl.vue";
import PrivacySettingSelector from "@/components/PrivacySettingSelector.vue";
import ProfileAboutCard from "@/components/ProfileAboutCard.vue";
import api from "@/api/client";
import { uploadMedia } from "@/api/media";
import { useAuthStore } from "@/stores/auth";
import { useHotspotStore } from "@/stores/hotspot";
import { useConnectionsStore } from "@/stores/connections";
import { useFeedStore } from "@/stores/feed";
import type { TimelinePost, PostVisibility } from "@/types/post";
import type { PrivacySetting, UserProfile, UserSummary } from "@/types/user";

const route = useRoute();
const authStore = useAuthStore();
const hotspotStore = useHotspotStore();
const connectionsStore = useConnectionsStore();
const feedStore = useFeedStore();
const { followers, followees } = storeToRefs(connectionsStore);

const profile = ref<UserProfile | null>(null);
const posts = ref<TimelinePost[]>([]);
const page = ref(0);
const size = 10;
const hasMore = ref(true);
const loadingMore = ref(false);
const isEditing = ref(false);
const updatingProfile = ref(false);
const formError = ref<string | null>(null);
const avatarFileInput = ref<HTMLInputElement | null>(null);
const avatarUploading = ref(false);
const avatarUploadError = ref<string | null>(null);
const managingPost = ref<TimelinePost | null>(null);
const postFormError = ref<string | null>(null);
const updatingPost = ref(false);
const deletingPostId = ref<number | null>(null);

const form = reactive({
  displayName: "",
  avatarUrl: "",
  bio: "",
  signature: "",
  location: "",
  privacySetting: "PUBLIC" as PrivacySetting,
});

const postForm = reactive({
  content: "",
  visibility: "PUBLIC" as PostVisibility,
  allowedUserIds: [] as number[],
});

const privacyMap: Record<PrivacySetting, { label: string; className: string }> = {
  PUBLIC: { label: "公开主页", className: "privacy-public" },
  FOLLOWERS_ONLY: { label: "仅粉丝可见", className: "privacy-followers" },
  PRIVATE: { label: "仅自己可见", className: "privacy-private" },
};

const privacyLabel = computed(() => {
  if (!profile.value) return "";
  return privacyMap[profile.value.privacySetting]?.label ?? "公开主页";
});

const privacyChipClass = computed(() => {
  if (!profile.value) return "privacy-public";
  return privacyMap[profile.value.privacySetting]?.className ?? "privacy-public";
});

const canFollow = computed(() => {
  if (!authStore.isAuthenticated || !profile.value) return false;
  return authStore.user?.id !== profile.value.id;
});

const canEdit = computed(() => {
  if (!authStore.isAuthenticated || !profile.value) return false;
  return authStore.user?.id === profile.value.id;
});

const availableConnections = computed<UserSummary[]>(() => {
  const map = new Map<number, UserSummary>();
  [...followers.value, ...followees.value].forEach((user) => {
    const numericId = Number(user.id);
    if (!Number.isFinite(numericId)) {
      return;
    }
    if (authStore.user?.id && authStore.user.id === numericId) {
      return;
    }
    if (!map.has(numericId)) {
      map.set(numericId, { ...user, id: numericId });
    }
  });
  return Array.from(map.values());
});

const sanitizePrivacy = (value: any): PrivacySetting => {
  if (value === "FOLLOWERS_ONLY" || value === "PRIVATE") {
    return value;
  }
  return "PUBLIC";
};

const sanitizeVisibility = (value: any): PostVisibility => {
  if (value === "FOLLOWERS_ONLY" || value === "PRIVATE" || value === "CUSTOM") {
    return value;
  }
  return "PUBLIC";
};

const sanitizeAllowedUserIds = (value: any): number[] => {
  if (!Array.isArray(value)) {
    return [];
  }
  const unique = new Set<number>();
  value.forEach((item) => {
    const numeric = Number(item);
    if (Number.isFinite(numeric)) {
      unique.add(numeric);
    }
  });
  return Array.from(unique.values());
};

const normalizeProfile = (raw: any): UserProfile => {
  const toNumber = (input: any, fallback = 0) => {
    const numeric = Number(input);
    return Number.isFinite(numeric) ? numeric : fallback;
  };
  return {
    id: toNumber(raw?.id),
    username: String(raw?.username ?? ""),
    displayName: String(raw?.displayName ?? ""),
    email: String(raw?.email ?? ""),
    bio: raw?.bio ?? null,
    signature: raw?.signature ?? null,
    location: raw?.location ?? null,
    avatarUrl: raw?.avatarUrl ?? null,
    privacySetting: sanitizePrivacy(raw?.privacySetting),
    createdAt: String(raw?.createdAt ?? new Date().toISOString()),
    followerCount: toNumber(raw?.followerCount),
    followingCount: toNumber(raw?.followingCount),
    postCount: toNumber(raw?.postCount),
    followedByCurrentUser: Boolean(raw?.followedByCurrentUser),
  };
};

const normalizePost = (raw: any): TimelinePost => {
  const allowedUserIds = sanitizeAllowedUserIds(raw?.allowedUserIds);
  const visibility = sanitizeVisibility(raw?.visibility);
  return {
    ...raw,
    visibility,
    allowedUserIds,
  } as TimelinePost;
};

const fillForm = (value: UserProfile) => {
  form.displayName = value.displayName ?? "";
  form.avatarUrl = value.avatarUrl ?? "";
  form.bio = value.bio ?? "";
  form.signature = value.signature ?? "";
  form.location = value.location ?? "";
  form.privacySetting = value.privacySetting ?? "PUBLIC";
};

const pickAvatarFile = () => {
  avatarUploadError.value = null;
  avatarFileInput.value?.click();
};

const clearAvatar = () => {
  form.avatarUrl = "";
};

const onAvatarFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  try {
    avatarUploading.value = true;
    avatarUploadError.value = null;
    const uploaded = await uploadMedia([file]);
    if (uploaded.length && uploaded[0].url) {
      form.avatarUrl = uploaded[0].url;
    } else {
      avatarUploadError.value = "上传失败，请重试";
    }
  } catch (error: any) {
    avatarUploadError.value = error?.response?.data?.message ?? "上传失败";
  } finally {
    avatarUploading.value = false;
  }
};

const toggleEdit = () => {
  if (!profile.value) return;
  if (!isEditing.value) {
    fillForm(profile.value);
    formError.value = null;
    avatarUploadError.value = null;
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
      privacySetting: form.privacySetting,
    };
    const { data } = await api.put<UserProfile>("/users/me", payload);
    const normalized = normalizeProfile(data);
    profile.value = normalized;
    fillForm(normalized);
    isEditing.value = false;
    await authStore.fetchCurrentUser().catch(() => undefined);
  } catch (error: any) {
    formError.value = error?.response?.data?.message ?? "更新失败";
  } finally {
    updatingProfile.value = false;
  }
};

const ensureConnectionsLoaded = async () => {
  if (!authStore.isAuthenticated) return;
  if (connectionsStore.loaded || connectionsStore.status === "loading") return;
  try {
    await connectionsStore.fetch();
  } catch (error) {
    console.error(error);
  }
};

const startPostEdit = async (post: TimelinePost) => {
  if (!canEdit.value) return;
  managingPost.value = post;
  postForm.content = post.content ?? "";
  postForm.visibility = sanitizeVisibility(post.visibility);
  postForm.allowedUserIds = sanitizeAllowedUserIds(post.allowedUserIds);
  postFormError.value = null;
  if (postForm.visibility === "CUSTOM") {
    await ensureConnectionsLoaded();
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
};

const cancelPostEdit = () => {
  managingPost.value = null;
  postForm.content = "";
  postForm.visibility = "PUBLIC";
  postForm.allowedUserIds = [];
  postFormError.value = null;
  updatingPost.value = false;
};

const applyPostUpdate = (updated: TimelinePost) => {
  const normalized = normalizePost(updated);
  const existing = posts.value.find((post) => post.id === normalized.id);
  if (existing) {
    Object.assign(existing, normalized);
  } else {
    posts.value.unshift(normalized);
  }
  hotspotStore.syncPost(normalized);
  feedStore.updatePost(normalized);
};

const submitPostUpdate = async () => {
  if (!managingPost.value) return;
  if (!postForm.content.trim()) {
    postFormError.value = "动态内容不能为空";
    return;
  }
  if (postForm.visibility === "CUSTOM" && postForm.allowedUserIds.length === 0) {
    postFormError.value = "请至少选择一位可见联系人";
    return;
  }
  updatingPost.value = true;
  postFormError.value = null;
  try {
    const payload = {
      content: postForm.content.trim(),
      visibility: postForm.visibility,
      allowedUserIds: postForm.visibility === "CUSTOM" ? postForm.allowedUserIds : [],
    };
    const { data } = await api.put<TimelinePost>(`/posts/${managingPost.value.id}`, payload);
    applyPostUpdate(data);
    managingPost.value = null;
  } catch (error: any) {
    postFormError.value = error?.response?.data?.message ?? "更新失败";
  } finally {
    updatingPost.value = false;
  }
};

const confirmDeletePost = async (post: TimelinePost) => {
  if (!canEdit.value) return;
  const confirmed = window.confirm("确定删除这条动态吗？");
  if (!confirmed) return;
  deletingPostId.value = post.id;
  try {
    await api.delete(`/posts/${post.id}`);
    posts.value = posts.value.filter((item) => item.id !== post.id);
    hotspotStore.removePost(post.id);
    feedStore.removePost(post.id);
    if (profile.value) {
      profile.value.postCount = Math.max(0, profile.value.postCount - 1);
    }
    if (managingPost.value?.id === post.id) {
      cancelPostEdit();
    }
  } catch (error: any) {
    const message = error?.response?.data?.message ?? "删除失败";
    window.alert(message);
  } finally {
    deletingPostId.value = null;
  }
};

const isPostBusy = (postId: number) => {
  if (deletingPostId.value === postId) {
    return true;
  }
  if (managingPost.value?.id === postId) {
    return updatingPost.value;
  }
  return false;
};

const fetchProfile = async (id: string | string[]) => {
  const { data } = await api.get(`/users/${id}`);
  const normalized = normalizeProfile(data);
  profile.value = normalized;
  if (authStore.user?.id === normalized.id) {
    fillForm(normalized);
  }
};

const fetchPosts = async () => {
  if (!profile.value || loadingMore.value || !hasMore.value) return;
  loadingMore.value = true;
  try {
    const { data } = await api.get(`/posts/user/${profile.value.id}`, {
      params: { page: page.value, size },
    });
    const fetched: TimelinePost[] = (data.content ?? []).map((item: any) => normalizePost(item));
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
  managingPost.value = null;
  formError.value = null;
  postFormError.value = null;
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
    void load();
  }
);

watch(
  () => postForm.visibility,
  (visibility) => {
    if (visibility === "CUSTOM") {
      void ensureConnectionsLoaded();
    }
  }
);

onMounted(() => {
  void load();
});
</script>

<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.profile-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
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
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.meta-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
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

.actions-stack {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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

.privacy-chip {
  padding: 0.3rem 0.8rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
}

.privacy-public {
  background: rgba(34, 197, 94, 0.15);
  color: #bbf7d0;
}

.privacy-followers {
  background: rgba(59, 130, 246, 0.15);
  color: #bfdbfe;
}

.privacy-private {
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
}

.about-card-wrapper {
  margin-top: -0.5rem;
}

.edit-section,
.post-manage {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-form,
.post-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-row label,
.field-label,
.visibility-wrapper h4 {
  font-weight: 600;
  color: #cbd5f5;
  margin: 0;
}

.form-row input,
.form-row textarea,
.post-form textarea {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.65rem 0.9rem;
  resize: vertical;
}

.form-row input:focus,
.form-row textarea:focus,
.post-form textarea:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.avatar-row .avatar-field {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.avatar-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.avatar-actions button {
  border: 1px solid rgba(99, 102, 241, 0.6);
  background: transparent;
  color: #c7d2fe;
  border-radius: 999px;
  padding: 0.35rem 1.1rem;
}

.avatar-actions .ghost {
  border-color: rgba(148, 163, 184, 0.4);
  color: #e2e8f0;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-hint {
  margin: 0;
  font-size: 0.85rem;
  color: #94a3b8;
}

.form-error {
  color: #fda4af;
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.form-actions button {
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
}

.form-actions .ghost {
  background: rgba(148, 163, 184, 0.25);
  color: #e2e8f0;
}

.form-actions button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.post-manage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.close-btn {
  border: none;
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 1.1rem;
  line-height: 1;
}

.posts {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.posts-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #cbd5f5;
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
  background: rgba(99, 102, 241, 0.2);
  color: #cbd5f5;
}

.placeholder {
  text-align: center;
  color: #94a3b8;
}

@media (max-width: 720px) {
  .profile-header {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }

  .meta-header {
    justify-content: center;
  }

  .stats {
    justify-content: center;
  }

  .actions-stack {
    flex-direction: row;
  }
}
</style>
