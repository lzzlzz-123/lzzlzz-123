<template>
  <div class="privacy-selector" role="radiogroup" aria-label="隐私设置">
    <label v-for="option in options" :key="option.value" class="option">
      <input
        type="radio"
        class="radio"
        name="privacy"
        :value="option.value"
        :checked="option.value === modelValue"
        @change="onSelect(option.value)"
      />
      <div class="copy">
        <strong>{{ option.label }}</strong>
        <p>{{ option.description }}</p>
      </div>
      <span v-if="option.value === modelValue" class="active-indicator">✓</span>
    </label>
  </div>
</template>

<script setup lang="ts">
import type { PrivacySetting } from "@/types/user";

type Option = {
  value: PrivacySetting;
  label: string;
  description: string;
};

const props = defineProps<{
  modelValue: PrivacySetting;
}>();

const emit = defineEmits<{
  (event: "update:modelValue", value: PrivacySetting): void;
}>();

const options: Option[] = [
  { value: "PUBLIC", label: "公开", description: "任何人都可以访问并查看你的主页" },
  { value: "FOLLOWERS_ONLY", label: "仅粉丝可见", description: "只有你的粉丝才能访问你的主页" },
  { value: "PRIVATE", label: "完全私密", description: "只有你自己可以访问主页" },
];

const onSelect = (value: PrivacySetting) => {
  if (value !== props.modelValue) {
    emit("update:modelValue", value);
  }
};
</script>

<style scoped>
.privacy-selector {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.option {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem 1rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(15, 23, 42, 0.55);
  transition: border 0.2s ease, background 0.2s ease;
}

.option:hover {
  border-color: rgba(99, 102, 241, 0.35);
}

.radio {
  accent-color: #6366f1;
  width: 18px;
  height: 18px;
}

.copy strong {
  font-size: 0.95rem;
  color: #e2e8f0;
}

.copy p {
  margin: 0.15rem 0 0;
  font-size: 0.85rem;
  color: #94a3b8;
}

.active-indicator {
  color: #34d399;
  font-weight: 700;
  font-size: 0.95rem;
}
</style>
