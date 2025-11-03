<template>
  <div class="topics">
    <section v-if="authStore.isAuthenticated" class="create-section">
      <h2>创建话题</h2>
      <form @submit.prevent="submitTopic">
        <div class="field">
          <label for="topic-name">话题名称</label>
          <input id="topic-name" v-model="name" type="text" maxlength="120" placeholder="例如：每日健身打卡" required />
        </div>
        <div class="field">
          <label for="topic-description">话题简介 (可选)</label>
          <textarea
            id="topic-description"
            v-model="description"
            maxlength="280"
            rows="3"
            placeholder="简要介绍这个话题"
          ></textarea>
        </div>
        <div class="actions">
          <span>{{ name.length }}/120</span>
          <button type="submit" :disabled="isCreating || !name.trim()">
            {{ isCreating ? "创建中..." : "创建话题" }}
          </button>
        </div>
        <p v-if="createError" class="error">{{ createError }}</p>
      </form>
    </section>

    <section class="ranking-section">
      <div class="section-header">
        <h2>话题热度榜</h2>
        <button type="button" @click="refreshRankings" :disabled="rankingLoading">
          {{ rankingLoading ? "刷新中..." : "刷新榜单" }}
        </button>
      </div>
      <p v-if="rankingError" class="error">{{ rankingError }}</p>
      <ul v-if="rankings.length" class="topic-list">
        <li v-for="(topic, index) in rankings" :key="topic.id">
          <span class="rank" :class="{ top: index < 3 }">#{{ index + 1 }}</span>
          <div class="body">
            <div class="title">
              <RouterLink :to="{ name: 'topic-detail', params: { id: topic.id } }" class="topic-link">
                <h3>{{ topic.name }}</h3>
              </RouterLink>
              <span class="heat">🔥 {{ topic.heat }}</span>
            </div>
            <p class="description">{{ topic.description ?? "这个话题还没有简介" }}</p>
            <div class="meta">成员 {{ topic.memberCount }}</div>
          </div>
          <button
            v-if="authStore.isAuthenticated"
            class="join"
            :class="{ joined: topic.joined }"
            :disabled="topic.joined || joiningTopicId === topic.id"
            @click="joinTopic(topic.id)"
          >
            {{ topic.joined ? "已加入" : joiningTopicId === topic.id ? "加入中..." : "加入" }}
          </button>
        </li>
      </ul>
      <p v-else-if="rankingLoading" class="empty">榜单加载中...</p>
      <p v-else class="empty">尚无话题热度数据</p>
    </section>

    <section v-if="authStore.isAuthenticated" class="my-section">
      <div class="section-header">
        <h2>我加入的话题</h2>
        <button type="button" @click="refreshMine" :disabled="myTopicsLoading">
          {{ myTopicsLoading ? "刷新中..." : "刷新" }}
        </button>
      </div>
      <p v-if="myTopicsError" class="error">{{ myTopicsError }}</p>
      <p v-if="actionError" class="error">{{ actionError }}</p>
      <ul v-if="myTopics.length" class="topic-list compact">
        <li v-for="topic in myTopics" :key="topic.id">
          <div class="body">
            <div class="title">
              <RouterLink :to="{ name: 'topic-detail', params: { id: topic.id } }" class="topic-link">
                <h3>{{ topic.name }}</h3>
              </RouterLink>
              <span class="heat">🔥 {{ topic.heat }}</span>
            </div>
            <p class="description">{{ topic.description ?? "这个话题还没有简介" }}</p>
            <div class="meta">成员 {{ topic.memberCount }}</div>
          </div>
          <button class="leave" :disabled="leavingTopicId === topic.id" @click="leaveTopic(topic.id)">
            {{ leavingTopicId === topic.id ? "退出中..." : "退出话题" }}
          </button>
        </li>
      </ul>
      <p v-else-if="myTopicsLoading" class="empty">加载中...</p>
      <p v-else class="empty">你还没有加入任何话题</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";
import { useTopicStore } from "@/stores/topic";

const authStore = useAuthStore();
const topicStore = useTopicStore();
const { rankings, rankingLoading, rankingError, myTopics, myTopicsLoading, myTopicsError } = storeToRefs(topicStore);

const name = ref("");
const description = ref("");
const isCreating = ref(false);
const createError = ref<string | null>(null);
const actionError = ref<string | null>(null);
const joiningTopicId = ref<number | null>(null);
const leavingTopicId = ref<number | null>(null);

const submitTopic = async () => {
  if (!name.value.trim()) return;
  try {
    isCreating.value = true;
    createError.value = null;
    await topicStore.createTopic({ name: name.value.trim(), description: description.value.trim() || null });
    name.value = "";
    description.value = "";
    actionError.value = null;
  } catch (error: any) {
    createError.value = error?.response?.data?.message ?? "创建话题失败";
  } finally {
    isCreating.value = false;
  }
};

const joinTopic = async (topicId: number) => {
  try {
    joiningTopicId.value = topicId;
    actionError.value = null;
    await topicStore.joinTopic(topicId);
  } catch (error: any) {
    actionError.value = error?.response?.data?.message ?? "加入话题失败";
  } finally {
    joiningTopicId.value = null;
  }
};

const leaveTopic = async (topicId: number) => {
  try {
    leavingTopicId.value = topicId;
    actionError.value = null;
    await topicStore.leaveTopic(topicId);
  } catch (error: any) {
    actionError.value = error?.response?.data?.message ?? "退出话题失败";
  } finally {
    leavingTopicId.value = null;
  }
};

const refreshRankings = () => topicStore.fetchRankings(true);
const refreshMine = () => topicStore.fetchMyTopics(true);

onMounted(() => {
  void topicStore.fetchRankings();
  if (authStore.isAuthenticated) {
    void topicStore.fetchMyTopics();
  }
});

watch(
  () => authStore.isAuthenticated,
  (isLoggedIn) => {
    if (isLoggedIn) {
      void topicStore.fetchMyTopics(true);
    } else {
      topicStore.resetMyTopics();
    }
  }
);
</script>

<style scoped>
.topics {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

section {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.create-section form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

label {
  font-size: 0.9rem;
  color: #cbd5f5;
}

input,
textarea {
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.7rem 0.9rem;
}

textarea {
  resize: vertical;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.85rem;
  color: #94a3b8;
}

.actions button,
.section-header button,
.join,
.leave {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.2rem;
  font-weight: 600;
}

.actions button {
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-header h2 {
  margin: 0;
}

.section-header button {
  background: rgba(99, 102, 241, 0.25);
  color: #c7d2fe;
}

.section-header button:disabled,
.actions button:disabled,
.join:disabled,
.leave:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.topic-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.topic-list li {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  background: rgba(15, 23, 42, 0.45);
  border-radius: 1.25rem;
  padding: 1rem 1.25rem;
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.topic-list.compact li {
  justify-content: space-between;
}

.rank {
  font-weight: 700;
  font-size: 1.1rem;
  color: #94a3b8;
  min-width: 52px;
}

.rank.top {
  color: #f97316;
}

.body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.title h3 {
  margin: 0;
  font-size: 1.05rem;
}

.topic-link {
  color: inherit;
  text-decoration: none;
}

.topic-link:hover h3 {
  color: #c7d2fe;
}

.heat {
  font-size: 0.9rem;
  color: #fb923c;
}

.description {
  margin: 0;
  font-size: 0.9rem;
  color: #cbd5f5;
  line-height: 1.5;
}

.meta {
  font-size: 0.8rem;
  color: #94a3b8;
}

.join {
  background: rgba(34, 211, 238, 0.2);
  color: #22d3ee;
}

.join.joined {
  background: rgba(148, 163, 184, 0.2);
  color: #94a3b8;
}

.leave {
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
}

.error {
  margin: 0;
  color: #fda4af;
}

.empty {
  margin: 0;
  text-align: center;
  color: #94a3b8;
}
</style>
