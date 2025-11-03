<template>
  <div class="composer">
    <div class="composer-header">
      <img v-if="authStore.user?.avatarUrl" :src="authStore.user?.avatarUrl" alt="avatar" />
      <div class="placeholder" v-else>{{ authStore.user?.displayName?.[0] ?? "你" }}</div>
      <h3>分享新动态</h3>
    </div>
    <textarea
      v-model="content"
      rows="4"
      maxlength="500"
      placeholder="此刻想说什么？"
    ></textarea>
    <div class="composer-controls">
      <div class="media-section">
        <div class="media-header">
          <span>媒体附件</span>
          <span class="media-counter">{{ uploadedMedia.length }}/{{ MAX_MEDIA }}</span>
        </div>
        <div class="media-actions">
          <button type="button" class="upload" :disabled="!canAddMoreMedia || isUploading" @click="openFilePicker">
            {{ isUploading ? "上传中..." : "上传图片 / 视频" }}
          </button>
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
      </div>

      <div v-if="topicOptions.length" class="topic-selector">
        <label v-if="isTopicLocked">关联话题</label>
        <label v-else for="composer-topic">选择话题</label>
        <div v-if="isTopicLocked" class="topic-lock-pill">
          <span>#{{ topicOptions[0].name }}</span>
          <small>🔥 {{ topicOptions[0].heat }}</small>
        </div>
        <select v-else id="composer-topic" v-model="selectedTopicId">
          <option :value="null">不关联话题</option>
          <option v-for="topic in topicOptions" :key="topic.id" :value="topic.id">
            {{ topic.name }} · 热度 {{ topic.heat }}
          </option>
        </select>
      </div>

      <div class="privacy-section">
        <h4>可见范围</h4>
        <PostVisibilityControl
          v-model="visibility"
          v-model:allowedUserIds="allowedUserIds"
          :connections="availableConnections"
        />
      </div>
    </div>
    <div class="actions">
      <span>{{ content.length }}/500</span>
      <button type="button" class="submit" :disabled="!canSubmit" @click="submit">
        {{ isSubmitting ? "发布中..." : "发布" }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import PostVisibilityControl from "@/components/PostVisibilityControl.vue";
import { uploadMedia } from "@/api/media";
import { useAuthStore } from "@/stores/auth";
import { useConnectionsStore } from "@/stores/connections";
import { useFeedStore } from "@/stores/feed";
import { useTopicStore } from "@/stores/topic";
import type { UploadedMedia } from "@/types/media";
import type { PostVisibility } from "@/types/post";
import type { UserSummary } from "@/types/user";
import type { TopicSummary } from "@/types/topic";

const MAX_MEDIA = 4;

const props = defineProps<{
  lockedTopic?: TopicSummary | null;
}>();

const emit = defineEmits<["posted"]>();
const authStore = useAuthStore();
const feedStore = useFeedStore();
const topicStore = useTopicStore();
const connectionsStore = useConnectionsStore();
const { myTopics } = storeToRefs(topicStore);
const { followers, followees } = storeToRefs(connectionsStore);

const content = ref("");
const uploadedMedia = ref<UploadedMedia[]>([]);
const isSubmitting = ref(false);
const isUploading = ref(false);
const uploadError = ref<string | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const selectedTopicId = ref<number | null>(props.lockedTopic?.id ?? null);
const visibility = ref<PostVisibility>("PUBLIC");
const allowedUserIds = ref<number[]>([]);

const isTopicLocked = computed(() => Boolean(props.lockedTopic));
const topicOptions = computed<TopicSummary[]>(() => {
  if (props.lockedTopic) {
    return [props.lockedTopic];
  }
  return myTopics.value ?? [];
});

const availableConnections = computed<UserSummary[]>(() => {
  const map = new Map<number, UserSummary>();
  [...followers.value, ...followees.value].forEach((user) => {
    const numeric = Number(user.id);
    if (!Number.isFinite(numeric)) {
      return;
    }
    if (authStore.user?.id && authStore.user.id === numeric) {
      return;
    }
    if (!map.has(numeric)) {
      map.set(numeric, { ...user, id: numeric });
    }
  });
  return Array.from(map.values());
});

const ensureConnectionsLoaded = async () => {
  if (!authStore.isAuthenticated) return;
  if (connectionsStore.loaded || connectionsStore.status === "loading") return;
  try {
    await connectionsStore.fetch();
  } catch (error) {
    console.error(error);
  }
};

const canAddMoreMedia = computed(() => uploadedMedia.value.length < MAX_MEDIA);
const canSubmit = computed(() => {
  if (isSubmitting.value || isUploading.value) return false;
  if (!content.value.trim()) return false;
  if (visibility.value === "CUSTOM" && allowedUserIds.value.length === 0) return false;
  return true;
});

const openFilePicker = () => {
  uploadError.value = null;
  fileInput.value?.click();
};

const onFilesSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  input.value = "";
  if (!files.length) return;

  const remaining = MAX_MEDIA - uploadedMedia.value.length;
  if (remaining <= 0) {
    uploadError.value = `最多只能上传${MAX_MEDIA}个文件`;
    return;
  }

  const selected = files.slice(0, remaining);
  if (files.length > remaining) {
    uploadError.value = `最多只能上传${MAX_MEDIA}个文件`;
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

const submit = async () => {
  if (!canSubmit.value) return;
  try {
    isSubmitting.value = true;
    const payload = {
      content: content.value,
      mediaUrls: uploadedMedia.value.map((item) => item.url),
      topicId: selectedTopicId.value,
      visibility: visibility.value,
      allowedUserIds: visibility.value === "CUSTOM" ? allowedUserIds.value : [],
    };
    const post = await feedStore.createPost(payload);
    emit("posted", post);
    content.value = "";
    uploadedMedia.value = [];
    uploadError.value = null;
    if (selectedTopicId.value) {
      void topicStore.fetchRankings(true);
    }
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(() => {
  if (authStore.isAuthenticated) {
    void topicStore.fetchMyTopics();
  }
});

watch(
  () => authStore.isAuthenticated,
  (isLoggedIn) => {
    if (isLoggedIn) {
      void topicStore.fetchMyTopics(true);
      if (visibility.value === "CUSTOM") {
        void ensureConnectionsLoaded();
      }
    } else {
      topicStore.resetMyTopics();
      connectionsStore.reset();
      allowedUserIds.value = [];
      if (!isTopicLocked.value) {
        selectedTopicId.value = null;
      }
    }
  }
);

watch(
  () => props.lockedTopic,
  (locked) => {
    if (locked) {
      selectedTopicId.value = locked.id;
    } else if (!topicOptions.value.some((topic) => topic.id === selectedTopicId.value)) {
      selectedTopicId.value = null;
    }
  }
);

watch(topicOptions, (options) => {
  if (!options.some((topic) => topic.id === selectedTopicId.value)) {
    selectedTopicId.value = null;
  }
});

watch(visibility, (value) => {
  if (value === "CUSTOM") {
    void ensureConnectionsLoaded();
  }
});
</script>

<style scoped>
.composer {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 1.25rem;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.composer-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.composer-header img,
.placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

textarea {
  width: 100%;
  resize: none;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 1rem;
  color: inherit;
  font-size: 1rem;
  line-height: 1.6;
}

textarea:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.6);
}

.composer-controls {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.media-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.media-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.95rem;
  color: #cbd5f5;
}

.media-counter {
  font-size: 0.85rem;
  color: #94a3b8;
}

.media-actions {
  display: flex;
  gap: 0.75rem;
}

.upload {
  align-self: flex-start;
  background: transparent;
  border: 1px dashed rgba(99, 102, 241, 0.7);
  border-radius: 999px;
  color: #c7d2fe;
  padding: 0.45rem 1.2rem;
}

.upload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #fda4af;
  font-size: 0.85rem;
}

.media-preview {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
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
}

.media-item video {
  background: #0f172a;
}

.media-item small {
  padding: 0 0.5rem 0.5rem;
  font-size: 0.75rem;
  color: #94a3b8;
}

.remove {
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

.topic-selector {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.topic-selector label {
  font-size: 0.9rem;
  color: #cbd5f5;
}

.topic-selector select {
  border-radius: 0.75rem;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 0.6rem 0.75rem;
  color: inherit;
}

.topic-lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  align-self: flex-start;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
  font-weight: 500;
}

.topic-lock-pill small {
  font-size: 0.75rem;
  color: #94a3b8;
}

.privacy-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.privacy-section h4 {
  margin: 0;
  font-size: 0.95rem;
  color: #cbd5f5;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.85rem;
  color: #94a3b8;
}

.submit {
  border: none;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  color: #0f172a;
  padding: 0.55rem 1.4rem;
  border-radius: 999px;
  font-weight: 600;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
