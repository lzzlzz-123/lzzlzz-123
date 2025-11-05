<template>
  <div class="visibility-control">
    <div class="options" role="radiogroup" aria-label="动态可见范围">
      <label v-for="option in options" :key="option.value" class="option">
        <input
          type="radio"
          class="radio"
          :name="radioGroupName"
          :value="option.value"
          :checked="option.value === modelValue"
          @change="selectVisibility(option.value)"
        />
        <div class="copy">
          <strong>{{ option.label }}</strong>
          <p>{{ option.description }}</p>
        </div>
        <span v-if="option.value === modelValue" class="active-indicator">✓</span>
      </label>
    </div>

    <div v-if="isCustomVisibility" class="custom" aria-live="polite">
      <p class="hint">
        请选择可以查看该动态的联系人（已选 {{ allowedCount }}/{{ maxAllowed }} ）
      </p>
      <div v-if="uniqueConnections.length" class="user-grid">
        <label
          v-for="user in uniqueConnections"
          :key="user.id"
          class="user-option"
          :class="{ selected: allowedSet.has(user.id) }"
        >
          <input
            type="checkbox"
            class="checkbox"
            :value="user.id"
            :checked="allowedSet.has(user.id)"
            @change="toggleAllowed(user.id)"
          />
          <div class="user-meta">
            <div class="avatar" v-if="user.avatarUrl">
              <img :src="user.avatarUrl" :alt="user.displayName" />
            </div>
            <div class="avatar fallback" v-else>
              {{ getInitial(user) }}
            </div>
            <div class="text">
              <strong>{{ user.displayName }}</strong>
              <span>@{{ user.username }}</span>
            </div>
          </div>
        </label>
      </div>
      <p v-else class="empty">暂无可选联系人，先去关注或吸引一些粉丝吧～</p>
      <p v-if="selectionError" class="error">{{ selectionError }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { PostVisibility } from "@/types/post";
import type { UserSummary } from "@/types/user";

type VisibilityOption = {
  value: PostVisibility;
  label: string;
  description: string;
};

const VISIBILITY_OPTIONS: VisibilityOption[] = [
  { value: "PUBLIC", label: "公开", description: "任何人都可以看到这条动态" },
  { value: "FOLLOWERS_ONLY", label: "仅粉丝可见", description: "只有你的粉丝可以看到" },
  { value: "PRIVATE", label: "仅自己可见", description: "除你之外无人可见" },
  { value: "CUSTOM", label: "指定人可见", description: "只向选中的联系人开放" },
];

let controlSeed = 0;
const nextGroupName = () => `post-visibility-${++controlSeed}`;

const props = defineProps<{
  modelValue: PostVisibility;
  allowedUserIds: number[];
  connections: UserSummary[];
  maxAllowed?: number;
}>();

const emit = defineEmits<{
  (event: "update:modelValue", value: PostVisibility): void;
  (event: "update:allowedUserIds", value: number[]): void;
}>();

const radioGroupName = nextGroupName();
const selectionError = ref<string | null>(null);

const maxAllowed = computed(() => {
  const fallback = 20;
  const numeric = Number(props.maxAllowed);
  if (!Number.isFinite(numeric)) {
    return fallback;
  }
  return Math.max(1, Math.trunc(numeric));
});

const options = VISIBILITY_OPTIONS;
const isCustomVisibility = computed(() => props.modelValue === "CUSTOM");

const uniqueConnections = computed<UserSummary[]>(() => {
  const map = new Map<number, UserSummary>();
  props.connections.forEach((user) => {
    if (!user) return;
    const numericId = Number(user.id);
    if (!Number.isFinite(numericId) || map.has(numericId)) {
      return;
    }
    map.set(numericId, {
      ...user,
      id: numericId,
    });
  });
  return Array.from(map.values());
});

const sanitizedAllowedIds = computed<number[]>(() => {
  const unique = new Set<number>();
  props.allowedUserIds.forEach((value) => {
    const numeric = Number(value);
    if (Number.isFinite(numeric)) {
      unique.add(numeric);
    }
  });
  return Array.from(unique.values());
});

const allowedSet = computed(() => new Set<number>(sanitizedAllowedIds.value));
const allowedCount = computed(() => sanitizedAllowedIds.value.length);

const selectVisibility = (value: PostVisibility) => {
  if (value === props.modelValue) {
    return;
  }
  emit("update:modelValue", value);
  if (value !== "CUSTOM") {
    selectionError.value = null;
  }
};

const toggleAllowed = (userId: number) => {
  selectionError.value = null;
  const numeric = Number(userId);
  if (!Number.isFinite(numeric)) {
    return;
  }
  const next = new Set(allowedSet.value);
  if (next.has(numeric)) {
    next.delete(numeric);
  } else {
    if (next.size >= maxAllowed.value) {
      selectionError.value = `最多可以选择 ${maxAllowed.value} 位联系人`;
      return;
    }
    next.add(numeric);
  }
  emit("update:allowedUserIds", Array.from(next.values()));
};

const getInitial = (user: UserSummary) => {
  const display = (user.displayName || "").trim();
  if (display) {
    return display.charAt(0);
  }
  const username = (user.username || "").trim();
  if (username) {
    return username.charAt(0);
  }
  return "?";
};

watch(
  () => [props.modelValue, sanitizedAllowedIds.value, maxAllowed.value] as const,
  ([visibility, ids, limit]) => {
    if (visibility !== "CUSTOM") {
      if (ids.length) {
        emit("update:allowedUserIds", []);
      }
      selectionError.value = null;
      return;
    }
    if (ids.length > limit) {
      emit("update:allowedUserIds", ids.slice(0, limit));
      selectionError.value = `最多可以选择 ${limit} 位联系人`;
    } else {
      selectionError.value = null;
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.visibility-control {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.option {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.8rem 1rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.55);
  transition: border 0.2s ease, background 0.2s ease;
}

.option:hover {
  border-color: rgba(99, 102, 241, 0.4);
}

.radio {
  width: 18px;
  height: 18px;
  accent-color: #6366f1;
}

.copy strong {
  font-size: 0.95rem;
  color: #e2e8f0;
}

.copy p {
  margin: 0.2rem 0 0;
  color: #94a3b8;
  font-size: 0.85rem;
}

.active-indicator {
  color: #34d399;
  font-weight: 700;
}

.custom {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-radius: 1rem;
  border: 1px dashed rgba(99, 102, 241, 0.35);
  background: rgba(15, 23, 42, 0.45);
  padding: 1rem;
}

.hint {
  margin: 0;
  font-size: 0.85rem;
  color: #cbd5f5;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.5);
  transition: border 0.2s ease, transform 0.2s ease;
}

.user-option.selected {
  border-color: rgba(99, 102, 241, 0.6);
  transform: translateY(-1px);
}

.checkbox {
  width: 16px;
  height: 16px;
  accent-color: #6366f1;
}

.user-meta {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: center;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #e2e8f0;
}

.avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.fallback {
  font-size: 1rem;
}

.text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.text strong {
  font-size: 0.95rem;
  color: #e2e8f0;
}

.text span {
  font-size: 0.8rem;
  color: #94a3b8;
}

.empty {
  margin: 0;
  color: #94a3b8;
  font-size: 0.85rem;
}

.error {
  margin: 0;
  color: #fda4af;
  font-size: 0.85rem;
}
</style>
