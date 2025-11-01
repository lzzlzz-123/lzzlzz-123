<template>
  <div class="admin-hotspot">
    <header class="admin-header">
      <h2>热点管理中心</h2>
      <button type="button" @click="refreshAll" :disabled="isRefreshing">
        {{ isRefreshing ? "刷新中..." : "刷新数据" }}
      </button>
    </header>

    <div class="admin-grid">
      <section class="card">
        <h3>创建热点话题</h3>
        <form class="form" @submit.prevent="submitTopic">
          <div class="form-row">
            <label for="admin-topic-name">话题名称</label>
            <input id="admin-topic-name" v-model="topicName" type="text" maxlength="120" required />
          </div>
          <div class="form-row">
            <label for="admin-topic-description">话题描述</label>
            <textarea
              id="admin-topic-description"
              v-model="topicDescription"
              rows="3"
              maxlength="280"
              placeholder="可选"
            ></textarea>
          </div>
          <div class="form-row">
            <label for="admin-topic-heat">初始热度</label>
            <input id="admin-topic-heat" v-model.number="topicInitialHeat" type="number" min="0" />
          </div>
          <p v-if="topicError" class="error">{{ topicError }}</p>
          <p v-if="topicSuccess" class="success">{{ topicSuccess }}</p>
          <button type="submit" :disabled="topicSubmitting">
            {{ topicSubmitting ? "创建中..." : "创建话题" }}
          </button>
        </form>
      </section>

      <section class="card">
        <h3>创建热点聚焦</h3>
        <form class="form" @submit.prevent="submitPost">
          <div class="form-row">
            <label for="admin-post-content">热点内容</label>
            <textarea
              id="admin-post-content"
              v-model="postContent"
              rows="4"
              maxlength="500"
              placeholder="输入热点内容"
            ></textarea>
          </div>
          <div class="form-row">
            <label for="admin-post-topic">关联话题</label>
            <select id="admin-post-topic" v-model="selectedTopicId">
              <option :value="null">不关联话题</option>
              <option v-for="topic in topicOptions" :key="topic.id" :value="topic.id">
                {{ topic.name }} · 热度 {{ topic.heat }}
              </option>
            </select>
          </div>
          <div class="form-row">
            <label for="admin-post-heat">初始热度</label>
            <input id="admin-post-heat" v-model.number="postInitialHeat" type="number" min="0" />
          </div>
          <div class="form-row media-upload">
            <label>媒体附件</label>
            <div class="media-actions">
              <button type="button" @click="openFilePicker" :disabled="isUploading || !canAddMoreMedia">
                {{ isUploading ? "上传中..." : "上传图片 / 视频" }}
              </button>
              <span>{{ uploadedMedia.length }}/{{ MAX_MEDIA }}</span>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/*,video/*"
              multiple
              hidden
              @change="onFilesSelected"
            />
          </div>
          <p v-if="uploadError" class="error">{{ uploadError }}</p>
          <div v-if="uploadedMedia.length" class="media-preview">
            <div v-for="(media, index) in uploadedMedia" :key="media.url" class="media-item">
              <button type="button" class="remove" @click="removeMedia(index)">×</button>
              <img
                v-if="media.mediaType !== 'video'"
                :src="media.url"
                :alt="media.originalFilename ?? `media-${index}`"
              />
              <video v-else controls :src="media.url"></video>
              <small>{{ media.originalFilename ?? `附件${index + 1}` }}</small>
            </div>
          </div>
          <p v-if="postError" class="error">{{ postError }}</p>
          <p v-if="postSuccess" class="success">{{ postSuccess }}</p>
          <button type="submit" :disabled="!canSubmitPost">
            {{ postSubmitting ? "发布中..." : "创建热点聚焦" }}
          </button>
        </form>
      </section>
    </div>

    <section class="card">
      <div class="section-heading">
        <h3>热点榜单控制</h3>
        <span v-if="hotspotRankingLoading">同步中...</span>
      </div>
      <p v-if="hotspotRankingError" class="error">{{ hotspotRankingError }}</p>
      <p v-else-if="!hotspotRanking.length && hotspotRankingLoading" class="empty">加载中...</p>
      <p v-else-if="!hotspotRanking.length" class="empty">暂无热点数据</p>
      <p v-if="postControlError" class="error">{{ postControlError }}</p>
      <p v-if="postControlMessage" class="success">{{ postControlMessage }}</p>
      <ul v-if="hotspotRanking.length" class="list">
        <li v-for="(post, index) in hotspotRanking" :key="post.id">
          <div class="list-main">
            <span class="rank">#{{ index + 1 }}</span>
            <div class="list-body">
              <RouterLink :to="`/post/${post.id}`" class="title">{{ snippet(post.content) }}</RouterLink>
              <div class="meta">
                <span>作者：{{ post.author.displayName }}</span>
                <span>当前热度：{{ post.heat }}</span>
              </div>
            </div>
          </div>
          <div class="list-actions">
            <input
              type="number"
              min="0"
              :value="postHeatEdits[post.id] ?? post.heat"
              @input="onPostHeatInput(post.id, $event)"
            />
            <button type="button" @click="savePostHeat(post)" :disabled="updatingPostHeat === post.id">
              {{ updatingPostHeat === post.id ? "保存中..." : "保存" }}
            </button>
          </div>
        </li>
      </ul>
    </section>

    <section class="card">
      <div class="section-heading">
        <h3>话题热度榜控制</h3>
        <span v-if="topicRankingsLoading">同步中...</span>
      </div>
      <p v-if="topicRankingsError" class="error">{{ topicRankingsError }}</p>
      <p v-else-if="!topicRankings.length && topicRankingsLoading" class="empty">加载中...</p>
      <p v-else-if="!topicRankings.length" class="empty">暂无话题数据</p>
      <p v-if="topicControlError" class="error">{{ topicControlError }}</p>
      <p v-if="topicControlMessage" class="success">{{ topicControlMessage }}</p>
      <ul v-if="topicRankings.length" class="list">
        <li v-for="(topic, index) in topicRankings" :key="topic.id">
          <div class="list-main">
            <span class="rank">#{{ index + 1 }}</span>
            <div class="list-body">
              <RouterLink :to="`/topics?focus=${topic.id}`" class="title">#{{ topic.name }}</RouterLink>
              <div class="meta">
                <span>当前热度：{{ topic.heat }}</span>
                <span>成员：{{ topic.memberCount }}</span>
              </div>
            </div>
          </div>
          <div class="list-actions">
            <input
              type="number"
              min="0"
              :value="topicHeatEdits[topic.id] ?? topic.heat"
              @input="onTopicHeatInput(topic.id, $event)"
            />
            <button type="button" @click="saveTopicHeat(topic)" :disabled="updatingTopicHeat === topic.id">
              {{ updatingTopicHeat === topic.id ? "保存中..." : "保存" }}
            </button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { storeToRefs } from "pinia";
import { uploadMedia } from "@/api/media";
import { createHotspotPost, updateHotspotPostHeat, createHotTopic, updateTopicHeat } from "@/api/admin";
import { useHotspotStore } from "@/stores/hotspot";
import { useTopicStore } from "@/stores/topic";
import { useFeedStore } from "@/stores/feed";
import type { UploadedMedia } from "@/types/media";
import type { TimelinePost } from "@/types/post";
import type { TopicResponse, TopicSummary } from "@/types/topic";

const MAX_MEDIA = 4;

const hotspotStore = useHotspotStore();
const topicStore = useTopicStore();
const feedStore = useFeedStore();

const { ranking: hotspotRanking, rankingLoading: hotspotRankingLoading, rankingError: hotspotRankingError } =
  storeToRefs(hotspotStore);
const { rankings: topicRankings, rankingsLoading: topicRankingsLoading, rankingsError: topicRankingsError } =
  storeToRefs(topicStore);

const topicName = ref("");
const topicDescription = ref("");
const topicInitialHeat = ref<number>(100);
const topicSubmitting = ref(false);
const topicError = ref<string | null>(null);
const topicSuccess = ref<string | null>(null);

const postContent = ref("");
const selectedTopicId = ref<number | null>(null);
const postInitialHeat = ref<number>(80);
const uploadedMedia = ref<UploadedMedia[]>([]);
const isUploading = ref(false);
const uploadError = ref<string | null>(null);
const postSubmitting = ref(false);
const postError = ref<string | null>(null);
const postSuccess = ref<string | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const postHeatEdits = ref<Record<number, number>>({});
const topicHeatEdits = ref<Record<number, number>>({});
const updatingPostHeat = ref<number | null>(null);
const updatingTopicHeat = ref<number | null>(null);
const postControlMessage = ref<string | null>(null);
const postControlError = ref<string | null>(null);
const topicControlMessage = ref<string | null>(null);
const topicControlError = ref<string | null>(null);

const isRefreshing = ref(false);

const canAddMoreMedia = computed(() => uploadedMedia.value.length < MAX_MEDIA);
const canSubmitPost = computed(() => postContent.value.trim().length > 0 && !postSubmitting.value && !isUploading.value);

const topicOptions = computed(() => topicRankings.value);

const snippet = (content: string) => {
  const trimmed = content.trim();
  return trimmed.length > 60 ? `${trimmed.slice(0, 60)}...` : trimmed || "(无内容)";
};

const toTopicSummary = (topic: TopicResponse | TopicSummary): TopicSummary => ({
  id: topic.id,
  name: topic.name,
  description: topic.description,
  heat: topic.heat,
  memberCount: topic.memberCount,
  joined: topic.joined,
});

const setRecordValue = (target: { value: Record<number, number> }, key: number, value: number) => {
  target.value = { ...target.value, [key]: value };
};

const removeRecordValue = (target: { value: Record<number, number> }, key: number) => {
  const { [key]: _removed, ...rest } = target.value;
  target.value = rest;
};

const refreshAll = async () => {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  postControlMessage.value = null;
  postControlError.value = null;
  topicControlMessage.value = null;
  topicControlError.value = null;
  try {
    await Promise.all([hotspotStore.fetchRanking(true), topicStore.fetchRankings(true)]);
  } catch (error: any) {
    const message = error?.response?.data?.message ?? "刷新失败";
    postControlError.value = message;
    topicControlError.value = message;
  } finally {
    isRefreshing.value = false;
  }
};

const submitTopic = async () => {
  if (!topicName.value.trim()) {
    topicError.value = "话题名称不能为空";
    return;
  }
  topicSubmitting.value = true;
  topicError.value = null;
  topicSuccess.value = null;
  try {
    const response = await createHotTopic({
      name: topicName.value.trim(),
      description: topicDescription.value.trim() ? topicDescription.value.trim() : null,
      initialHeat: Math.max(0, Math.floor(topicInitialHeat.value)),
    });
    const summary = toTopicSummary(response);
    topicStore.updateRanking(summary);
    topicSuccess.value = `已创建话题 #${response.name}`;
    topicName.value = "";
    topicDescription.value = "";
    selectedTopicId.value = summary.id;
  } catch (error: any) {
    topicError.value = error?.response?.data?.message ?? "创建失败";
  } finally {
    topicSubmitting.value = false;
  }
};

const openFilePicker = () => {
  uploadError.value = null;
  fileInput.value?.click();
};

const onFilesSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  input.value = "";
  if (!files.length) {
    return;
  }
  const remaining = MAX_MEDIA - uploadedMedia.value.length;
  if (remaining <= 0) {
    uploadError.value = `最多只能上传${MAX_MEDIA}个文件`;
    return;
  }
  const selected = files.slice(0, remaining);
  if (files.length > remaining) {
    uploadError.value = `最多只能上传${MAX_MEDIA}个文件`;
  } else {
    uploadError.value = null;
  }
  try {
    isUploading.value = true;
    const uploaded = await uploadMedia(selected);
    uploadedMedia.value.push(...uploaded);
  } catch (error: any) {
    uploadError.value = error?.response?.data?.message ?? "上传失败，请重试";
  } finally {
    isUploading.value = false;
  }
};

const removeMedia = (index: number) => {
  uploadedMedia.value.splice(index, 1);
};

const submitPost = async () => {
  if (!canSubmitPost.value) return;
  postSubmitting.value = true;
  postError.value = null;
  postSuccess.value = null;
  try {
    const payloadTopicId = selectedTopicId.value ?? null;
    const post = await createHotspotPost({
      content: postContent.value.trim(),
      mediaUrls: uploadedMedia.value.map((item) => item.url),
      topicId: payloadTopicId,
      initialHeat: Math.max(0, Math.floor(postInitialHeat.value)),
    });
    hotspotStore.syncPost(post);
    const existing = feedStore.posts.find((item) => item.id === post.id);
    if (existing) {
      Object.assign(existing, post);
    } else {
      feedStore.prepend(post);
    }
    postContent.value = "";
    uploadedMedia.value = [];
    postSuccess.value = "热点内容已发布";
    postError.value = null;
    selectedTopicId.value = post.topic?.id ?? payloadTopicId;
    uploadError.value = null;
  } catch (error: any) {
    postError.value = error?.response?.data?.message ?? "创建失败";
  } finally {
    postSubmitting.value = false;
  }
};

const onPostHeatInput = (postId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const parsed = Math.max(0, Math.floor(Number(input.value)));
  if (Number.isFinite(parsed)) {
    setRecordValue(postHeatEdits, postId, parsed);
  }
};

const getPostTargetHeat = (post: TimelinePost) => postHeatEdits.value[post.id] ?? post.heat;

const savePostHeat = async (post: TimelinePost) => {
  const targetHeat = Math.max(0, getPostTargetHeat(post));
  if (targetHeat === post.heat) {
    removeRecordValue(postHeatEdits, post.id);
    postControlMessage.value = "热度未发生变化";
    return;
  }
  updatingPostHeat.value = post.id;
  postControlError.value = null;
  postControlMessage.value = null;
  try {
    const updated = await updateHotspotPostHeat(post.id, targetHeat);
    hotspotStore.syncPost(updated);
    const existing = feedStore.posts.find((item) => item.id === updated.id);
    if (existing) {
      Object.assign(existing, updated);
    }
    removeRecordValue(postHeatEdits, post.id);
    postControlMessage.value = `已更新热度至 ${updated.heat}`;
  } catch (error: any) {
    postControlError.value = error?.response?.data?.message ?? "更新失败";
  } finally {
    updatingPostHeat.value = null;
  }
};

const onTopicHeatInput = (topicId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const parsed = Math.max(0, Math.floor(Number(input.value)));
  if (Number.isFinite(parsed)) {
    setRecordValue(topicHeatEdits, topicId, parsed);
  }
};

const getTopicTargetHeat = (topic: TopicSummary) => topicHeatEdits.value[topic.id] ?? topic.heat;

const saveTopicHeat = async (topic: TopicSummary) => {
  const targetHeat = Math.max(0, getTopicTargetHeat(topic));
  if (targetHeat === topic.heat) {
    removeRecordValue(topicHeatEdits, topic.id);
    topicControlMessage.value = "热度未发生变化";
    return;
  }
  updatingTopicHeat.value = topic.id;
  topicControlError.value = null;
  topicControlMessage.value = null;
  try {
    const updated = await updateTopicHeat(topic.id, targetHeat);
    const summary = toTopicSummary(updated);
    topicStore.updateRanking(summary);
    removeRecordValue(topicHeatEdits, topic.id);
    topicControlMessage.value = `已更新热度至 ${summary.heat}`;
  } catch (error: any) {
    topicControlError.value = error?.response?.data?.message ?? "更新失败";
  } finally {
    updatingTopicHeat.value = null;
  }
};

watch(topicOptions, (options) => {
  if (selectedTopicId.value && !options.some((topic) => topic.id === selectedTopicId.value)) {
    selectedTopicId.value = null;
  }
});

watch(hotspotRanking, (posts) => {
  const keep = new Set(posts.map((post) => post.id));
  const next: Record<number, number> = {};
  Object.entries(postHeatEdits.value).forEach(([key, value]) => {
    if (keep.has(Number(key))) {
      next[Number(key)] = value;
    }
  });
  postHeatEdits.value = next;
});

watch(topicRankings, (topics) => {
  const keep = new Set(topics.map((topic) => topic.id));
  const next: Record<number, number> = {};
  Object.entries(topicHeatEdits.value).forEach(([key, value]) => {
    if (keep.has(Number(key))) {
      next[Number(key)] = value;
    }
  });
  topicHeatEdits.value = next;
});

onMounted(() => {
  void hotspotStore.fetchRanking(true);
  void topicStore.fetchRankings(true);
});
</script>

<style scoped>
.admin-hotspot {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem 1.75rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.admin-header h2 {
  margin: 0;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.card {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card h3 {
  margin: 0;
}

.form {
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
.form-row textarea,
.form-row select {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.65rem 0.9rem;
}

.form-row textarea {
  resize: vertical;
  min-height: 120px;
}

.form-row input:focus,
.form-row textarea:focus,
.form-row select:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.media-upload {
  gap: 0.75rem;
}

.media-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.media-actions span {
  font-size: 0.85rem;
  color: #94a3b8;
}

.media-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.media-item {
  position: relative;
  width: 140px;
  border-radius: 0.85rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.media-item img,
.media-item video {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.media-item video {
  background: #0f172a;
}

.media-item small {
  padding: 0 0.5rem 0.5rem;
  font-size: 0.75rem;
  color: #94a3b8;
}

.media-item .remove {
  position: absolute;
  top: 4px;
  right: 4px;
  border: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.75);
  color: #e2e8f0;
  font-size: 0.85rem;
  line-height: 1;
}

button {
  align-self: flex-start;
  border: none;
  border-radius: 999px;
  padding: 0.6rem 1.4rem;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.media-actions button {
  background: transparent;
  border: 1px dashed rgba(99, 102, 241, 0.7);
  color: #c7d2fe;
  padding: 0.5rem 1.2rem;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.section-heading span {
  font-size: 0.85rem;
  color: #94a3b8;
}

.error {
  margin: 0;
  color: #fda4af;
  font-size: 0.9rem;
}

.success {
  margin: 0;
  color: #34d399;
  font-size: 0.9rem;
}

.empty {
  margin: 0;
  color: #94a3b8;
  font-size: 0.9rem;
  text-align: center;
  padding: 0.5rem 0;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list li {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 1rem;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.45);
}

.list-main {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  flex: 1;
}

.rank {
  font-weight: 700;
  color: #f97316;
  min-width: 32px;
  font-size: 1.1rem;
}

.list-body {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  flex: 1;
}

.list-body .title {
  color: #e2e8f0;
  text-decoration: none;
  font-weight: 600;
}

.list-body .title:hover {
  text-decoration: underline;
}

.list-body .meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #94a3b8;
  font-size: 0.85rem;
}

.list-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.list-actions input {
  width: 110px;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.5);
  color: inherit;
  padding: 0.55rem 0.75rem;
}

.list-actions input:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.6);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.35);
}

.list-actions button {
  padding: 0.55rem 1.1rem;
}

@media (max-width: 768px) {
  .admin-grid {
    grid-template-columns: 1fr;
  }

  .list li {
    flex-direction: column;
    align-items: stretch;
  }

  .list-actions {
    justify-content: flex-end;
  }
}
</style>
