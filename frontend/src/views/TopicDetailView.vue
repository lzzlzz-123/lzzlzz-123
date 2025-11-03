<template>
  <div class="topic-detail">
    <section v-if="topicLoading" class="placeholder">加载话题中...</section>
    <section v-else-if="topicError" class="placeholder error">
      <p>{{ topicError }}</p>
      <RouterLink class="back-link" to="/topics">返回话题广场</RouterLink>
    </section>
    <template v-else-if="topic">
      <section class="topic-header">
        <div class="title">
          <h1>#{{ topic.name }}</h1>
          <span class="heat">🔥 {{ topic.heat }}</span>
        </div>
        <p class="description">{{ topic.description ?? "这个话题还没有简介" }}</p>
        <div class="meta">
          <span>成员 {{ topic.memberCount }}</span>
          <span>
            创建者
            <RouterLink :to="`/profile/${topic.owner.id}`">
              {{ topic.owner.displayName }}
            </RouterLink>
          </span>
          <span>创建于 {{ createdAtDisplay }}</span>
        </div>
        <div class="actions">
          <button
            v-if="!authStore.isAuthenticated"
            class="join"
            type="button"
            @click="redirectToLogin"
          >
            登录后加入
          </button>
          <button
            v-else-if="topic.joined"
            class="leave"
            type="button"
            :disabled="leaveLoading"
            @click="leaveCurrentTopic"
          >
            {{ leaveLoading ? "退出中..." : "退出话题" }}
          </button>
          <button
            v-else
            class="join"
            type="button"
            :disabled="joinLoading"
            @click="joinCurrentTopic"
          >
            {{ joinLoading ? "加入中..." : "加入话题" }}
          </button>
        </div>
        <p v-if="actionError" class="error">{{ actionError }}</p>
      </section>

      <PostComposer
        v-if="canPost && topicSummary"
        :locked-topic="topicSummary"
        @posted="handlePostCreated"
      />

      <section class="topic-posts">
        <div class="posts-header">
          <h2>话题帖子</h2>
          <button type="button" @click="refreshPosts" :disabled="postsLoading">
            {{ postsLoading && postsInitialLoading ? "刷新中..." : "刷新" }}
          </button>
        </div>
        <p v-if="postsError" class="error">{{ postsError }}</p>
        <section v-if="posts.length" class="post-list">
          <PostCard v-for="post in posts" :key="post.id" :post="post" />
        </section>
        <p v-if="postsInitialLoading" class="empty">加载中...</p>
        <p v-else-if="postsEmpty" class="empty">还没有人发表帖子</p>
        <div v-if="posts.length" class="post-actions">
          <button v-if="hasMore" :disabled="postsLoading" @click="loadMorePosts">
            {{ postsLoading && !postsInitialLoading ? "加载中..." : "加载更多" }}
          </button>
          <span v-else class="empty">没有更多帖子啦</span>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import PostComposer from "@/components/PostComposer.vue";
import PostCard from "@/components/PostCard.vue";
import { fetchTopic, fetchTopicPosts } from "@/api/topics";
import { useAuthStore } from "@/stores/auth";
import { useTopicStore } from "@/stores/topic";
import { useHotspotStore } from "@/stores/hotspot";
import type { TopicResponse, TopicSummary } from "@/types/topic";
import type { TimelinePost } from "@/types/post";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const topicStore = useTopicStore();
const hotspotStore = useHotspotStore();

const topic = ref<TopicResponse | null>(null);
const topicLoading = ref(false);
const topicError = ref<string | null>(null);
const joinLoading = ref(false);
const leaveLoading = ref(false);
const actionError = ref<string | null>(null);

const posts = ref<TimelinePost[]>([]);
const postsLoading = ref(false);
const postsError = ref<string | null>(null);
const page = ref(0);
const hasMore = ref(true);

const topicSummary = computed<TopicSummary | null>(() => {
  if (!topic.value) {
    return null;
  }
  return {
    id: topic.value.id,
    name: topic.value.name,
    description: topic.value.description,
    heat: topic.value.heat,
    memberCount: topic.value.memberCount,
    joined: topic.value.joined,
  };
});

const createdAtDisplay = computed(() => {
  if (!topic.value) {
    return "-";
  }
  const created = new Date(topic.value.createdAt);
  return Number.isNaN(created.getTime()) ? "-" : created.toLocaleString();
});

const postsInitialLoading = computed(() => postsLoading.value && posts.value.length === 0);
const postsEmpty = computed(() => !postsLoading.value && posts.value.length === 0);
const canPost = computed(() => authStore.isAuthenticated && Boolean(topic.value?.joined));

const parseTopicId = (): number | null => {
  const raw = route.params.id;
  const stringValue = Array.isArray(raw) ? raw[0] : raw;
  const numeric = Number(stringValue);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null;
};

const loadTopic = async (topicId: number) => {
  topicLoading.value = true;
  topicError.value = null;
  try {
    topic.value = await fetchTopic(topicId);
  } catch (error: any) {
    topic.value = null;
    topicError.value = error?.response?.data?.message ?? "加载话题详情失败";
  } finally {
    topicLoading.value = false;
  }
};

const loadTopicPosts = async (topicId: number, reset = false) => {
  if (postsLoading.value) {
    return;
  }
  if (!hasMore.value && !reset) {
    return;
  }
  postsLoading.value = true;
  postsError.value = null;
  const requestedPage = reset ? 0 : page.value;
  if (reset) {
    page.value = 0;
    hasMore.value = true;
    posts.value = [];
  }
  try {
    const data = await fetchTopicPosts(topicId, requestedPage, 20);
    const fetched = data.content ?? [];
    if (requestedPage === 0) {
      posts.value = [...fetched];
    } else {
      fetched.forEach((post) => {
        const existingIndex = posts.value.findIndex((item) => item.id === post.id);
        if (existingIndex >= 0) {
          posts.value.splice(existingIndex, 1, post);
        } else {
          posts.value.push(post);
        }
      });
    }
    if (fetched.length) {
      hotspotStore.syncPosts(fetched);
    }
    hasMore.value = !data.last;
    page.value = requestedPage + 1;
  } catch (error: any) {
    postsError.value = error?.response?.data?.message ?? "加载话题帖子失败";
  } finally {
    postsLoading.value = false;
  }
};

const loadFromRoute = async () => {
  const topicId = parseTopicId();
  actionError.value = null;
  joinLoading.value = false;
  leaveLoading.value = false;
  postsError.value = null;
  if (!topicId) {
    topic.value = null;
    posts.value = [];
    hasMore.value = false;
    topicError.value = "未找到对应的话题";
    return;
  }
  await loadTopic(topicId);
  if (topic.value) {
    await loadTopicPosts(topicId, true);
  } else {
    posts.value = [];
    hasMore.value = false;
  }
};

const ensureAuthenticated = (): boolean => {
  if (authStore.isAuthenticated) {
    return true;
  }
  redirectToLogin();
  return false;
};

const redirectToLogin = () => {
  router.push({ name: "login", query: { redirect: route.fullPath } });
};

const joinCurrentTopic = async () => {
  if (!topic.value || !ensureAuthenticated()) {
    return;
  }
  try {
    joinLoading.value = true;
    actionError.value = null;
    const updated = await topicStore.joinTopic(topic.value.id);
    topic.value = updated;
  } catch (error: any) {
    actionError.value = error?.response?.data?.message ?? "加入话题失败";
  } finally {
    joinLoading.value = false;
  }
};

const leaveCurrentTopic = async () => {
  if (!topic.value || !ensureAuthenticated()) {
    return;
  }
  try {
    leaveLoading.value = true;
    actionError.value = null;
    await topicStore.leaveTopic(topic.value.id);
    await loadTopic(topic.value.id);
  } catch (error: any) {
    actionError.value = error?.response?.data?.message ?? "退出话题失败";
  } finally {
    leaveLoading.value = false;
  }
};

const handlePostCreated = (post: TimelinePost) => {
  const existingIndex = posts.value.findIndex((item) => item.id === post.id);
  if (existingIndex >= 0) {
    posts.value.splice(existingIndex, 1);
  }
  posts.value.unshift(post);
  hotspotStore.syncPost(post);
};

const loadMorePosts = () => {
  const topicId = topic.value?.id ?? parseTopicId();
  if (!topicId) {
    return;
  }
  void loadTopicPosts(topicId);
};

const refreshPosts = () => {
  const topicId = topic.value?.id ?? parseTopicId();
  if (!topicId) {
    return;
  }
  void loadTopicPosts(topicId, true);
};

watch(
  () => route.params.id,
  () => {
    void loadFromRoute();
  }
);

watch(
  () => authStore.isAuthenticated,
  () => {
    const topicId = topic.value?.id ?? parseTopicId();
    if (topicId) {
      void loadTopic(topicId);
    }
  }
);

onMounted(() => {
  void loadFromRoute();
});
</script>

<style scoped>
.topic-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.placeholder {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  color: #94a3b8;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.placeholder.error {
  color: #fda4af;
}

.back-link {
  color: #38bdf8;
  text-decoration: none;
}

.topic-header {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.title h1 {
  margin: 0;
  font-size: 1.5rem;
}

.heat {
  font-size: 1rem;
  color: #fb923c;
}

.description {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
  color: #cbd5f5;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.9rem;
  color: #94a3b8;
}

.meta a {
  color: #38bdf8;
  text-decoration: none;
}

.actions {
  display: flex;
  gap: 0.75rem;
}

.actions button {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.3rem;
  font-weight: 600;
  font-size: 0.95rem;
}

.actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.actions .join {
  background: rgba(34, 211, 238, 0.2);
  color: #22d3ee;
}

.actions .leave {
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
}

.topic-posts {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.posts-header h2 {
  margin: 0;
  font-size: 1.2rem;
}

.posts-header button {
  border: none;
  border-radius: 999px;
  padding: 0.45rem 1.1rem;
  background: rgba(99, 102, 241, 0.25);
  color: #c7d2fe;
  font-size: 0.85rem;
}

.posts-header button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.post-actions {
  display: flex;
  justify-content: center;
  color: #94a3b8;
}

.post-actions button {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.3rem;
  background: rgba(148, 163, 184, 0.15);
  color: inherit;
  font-weight: 500;
}

.post-actions button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #fda4af;
  text-align: center;
}

.empty {
  margin: 0;
  text-align: center;
  color: #94a3b8;
}
</style>
