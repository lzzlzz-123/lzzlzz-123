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

const emit = defineEmits<{
  posted: [];
}>();
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
  background: rgba(30, 41, 59, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1.5rem;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
}

.composer-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.composer-header img,
.placeholder {
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  object-fit: cover;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.2), rgba(129, 140, 248, 0.2));
  color: #38bdf8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.25rem;
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.composer-header h3 {
  font-size: 1.15rem;
  font-weight: 700;
  background: linear-gradient(to right, #f8fafc, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

textarea {
  width: 100%;
  resize: none;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1.25rem;
  color: #f8fafc;
  font-size: 1.1rem;
  line-height: 1.6;
  transition: all 0.2s;
}

textarea:focus {
  outline: none;
  border-color: rgba(56, 189, 248, 0.5);
  background: rgba(15, 23, 42, 0.5);
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.1);
}

.composer-controls {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.media-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.media-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  font-weight: 600;
  color: #94a3b8;
}

.media-counter {
  font-size: 0.8rem;
  padding: 0.125rem 0.5rem;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 0.5rem;
}

.media-actions {
  display: flex;
  gap: 0.75rem;
}

.upload {
  align-self: flex-start;
  background: rgba(56, 189, 248, 0.05);
  border: 1px dashed rgba(56, 189, 248, 0.4);
  border-radius: 0.75rem;
  color: #38bdf8;
  padding: 0.6rem 1.25rem;
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.upload:hover:not(:disabled) {
  background: rgba(56, 189, 248, 0.1);
  border-color: #38bdf8;
  transform: translateY(-1px);
}

.upload:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #fb7185;
  font-size: 0.85rem;
  font-weight: 500;
}

.media-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 1rem;
}

.media-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 0.75rem;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  flex-direction: column;
}

.media-item img,
.media-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-item small {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0.35rem;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(4px);
  font-size: 0.7rem;
  color: #cbd5e1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.remove {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  border: none;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 0.5rem;
  background: rgba(244, 63, 94, 0.8);
  color: white;
  font-size: 1.15rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 1;
}

.remove:hover {
  background: #f43f5e;
  transform: scale(1.1);
}

.topic-selector {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.topic-selector label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #94a3b8;
}

.topic-selector select {
  border-radius: 0.75rem;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 0.75rem;
  color: #f8fafc;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.2s;
}

.topic-selector select:focus {
  border-color: rgba(56, 189, 248, 0.5);
}

.topic-lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  align-self: flex-start;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
  font-weight: 600;
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.topic-lock-pill small {
  font-size: 0.75rem;
  color: rgba(56, 189, 248, 0.7);
  background: rgba(56, 189, 248, 0.1);
  padding: 0.125rem 0.35rem;
  border-radius: 0.35rem;
}

.privacy-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.privacy-section h4 {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #94a3b8;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.actions span {
  font-size: 0.85rem;
  font-weight: 500;
  color: #64748b;
}

.submit {
  border: none;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: white;
  padding: 0.75rem 2rem;
  border-radius: 0.75rem;
  font-weight: 700;
  font-size: 1rem;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(99, 102, 241, 0.4);
  filter: brightness(1.1);
}

.submit:active:not(:disabled) {
  transform: translateY(0);
}

.submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
