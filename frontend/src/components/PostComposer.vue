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
    <div class="media-inputs">
      <input
        v-for="(url, index) in mediaUrls"
        :key="index"
        v-model="mediaUrls[index]"
        type="url"
        placeholder="图片或视频链接"
      />
      <button class="link-add" type="button" @click="addMediaField" :disabled="mediaUrls.length >= 4">
        添加媒体
      </button>
    </div>
    <div class="actions">
      <span>{{ content.length }}/500</span>
      <button type="button" class="submit" :disabled="isSubmitting || !content.trim()" @click="submit">
        {{ isSubmitting ? "发布中..." : "发布" }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { useFeedStore } from "@/stores/feed";

const emit = defineEmits<["posted"]>();
const authStore = useAuthStore();
const feedStore = useFeedStore();

const content = ref("");
const mediaUrls = ref<string[]>([]);
const isSubmitting = ref(false);

const addMediaField = () => {
  if (mediaUrls.value.length < 4) {
    mediaUrls.value.push("");
  }
};

const submit = async () => {
  if (!content.value.trim()) return;
  try {
    isSubmitting.value = true;
    const payload = {
      content: content.value,
      mediaUrls: mediaUrls.value.filter((url) => url.trim().length > 0),
    };
    const post = await feedStore.createPost(payload);
    emit("posted", post);
    content.value = "";
    mediaUrls.value = [];
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.composer {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 1.25rem;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

.media-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.media-inputs input {
  border-radius: 0.75rem;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 0.75rem 1rem;
  color: inherit;
}

.link-add {
  align-self: flex-start;
  background: transparent;
  border: 1px dashed rgba(99, 102, 241, 0.7);
  border-radius: 999px;
  color: #c7d2fe;
  padding: 0.4rem 1rem;
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
