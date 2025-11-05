<template>
  <div class="admin-home-ads">
    <header class="admin-header">
      <h2>首页广告管理</h2>
      <button type="button" @click="refreshAds" :disabled="loading">
        {{ loading ? "刷新中..." : "刷新数据" }}
      </button>
    </header>

    <section class="card">
      <h3>新增广告位</h3>
      <form class="form" @submit.prevent="submitCreate">
        <div class="form-row">
          <label for="admin-ad-title">广告标题</label>
          <input
            id="admin-ad-title"
            v-model="createForm.title"
            type="text"
            maxlength="120"
            required
            @input="clearCreateFeedback"
          />
        </div>
        <div class="form-row">
          <label for="admin-ad-image">广告图片</label>
          <div class="image-upload-group">
            <button
              type="button"
              class="upload-button"
              :disabled="createImageUpload.uploading"
              @click="triggerCreateImagePicker"
            >
              {{ createImageUpload.uploading ? "上传中..." : createForm.imageUrl ? "重新上传图片" : "上传图片" }}
            </button>
            <input
              id="admin-ad-image"
              ref="createImageInput"
              type="file"
              accept="image/*"
              hidden
              @change="onCreateImageSelected"
            />
            <span v-if="createImageUpload.fileName" class="upload-info">{{ createImageUpload.fileName }}</span>
            <input
              v-model="createForm.imageUrl"
              type="text"
              readonly
              maxlength="255"
              placeholder="请先上传图片"
            />
          </div>
          <p v-if="createImageUpload.error" class="error">{{ createImageUpload.error }}</p>
          <div v-if="createForm.imageUrl" class="image-preview">
            <img :src="createForm.imageUrl" alt="广告预览" />
          </div>
        </div>
        <div class="form-row">
          <label for="admin-ad-target">跳转链接</label>
          <input
            id="admin-ad-target"
            v-model="createForm.targetUrl"
            type="url"
            maxlength="255"
            required
            placeholder="https://example.com"
            @input="clearCreateFeedback"
          />
        </div>
        <div class="form-row row-inline">
          <div class="inline-field">
            <label for="admin-ad-order">排序值</label>
            <input
              id="admin-ad-order"
              v-model.number="createForm.displayOrder"
              type="number"
              min="0"
            />
          </div>
          <label class="toggle">
            <input type="checkbox" v-model="createForm.active" />
            <span>启用</span>
          </label>
        </div>
        <p v-if="createError" class="error">{{ createError }}</p>
        <p v-if="createSuccess" class="success">{{ createSuccess }}</p>
        <button type="submit" :disabled="creating">
          {{ creating ? "创建中..." : "创建广告" }}
        </button>
      </form>
    </section>

    <section class="card">
      <div class="section-heading">
        <h3>广告列表</h3>
        <span v-if="loading">同步中...</span>
      </div>
      <p v-if="listError" class="error">{{ listError }}</p>
      <p v-else-if="!ads.length && loading" class="empty">加载中...</p>
      <p v-else-if="!ads.length" class="empty">暂未配置广告位</p>
      <ul v-if="ads.length" class="ad-list">
        <li v-for="ad in ads" :key="ad.id" class="ad-item">
          <div class="ad-preview" aria-hidden="true">
            <img :src="formState[ad.id]?.imageUrl ?? ad.imageUrl" :alt="formState[ad.id]?.title ?? ad.title" />
          </div>
          <form v-if="formState[ad.id]" class="ad-form" @submit.prevent="saveAd(ad.id)">
            <div class="ad-form-grid">
              <div class="form-row">
                <label :for="`ad-title-${ad.id}`">标题</label>
                <input
                  :id="`ad-title-${ad.id}`"
                  v-model="formState[ad.id].title"
                  type="text"
                  maxlength="120"
                  required
                  @input="onFieldChange(ad.id)"
                />
              </div>
              <div class="form-row">
                <label :for="`ad-image-${ad.id}`">广告图片</label>
                <div class="image-upload-group">
                  <button
                    type="button"
                    class="upload-button"
                    :disabled="editImageUpload[ad.id]?.uploading"
                    @click="triggerEditImagePicker(ad.id)"
                  >
                    {{
                      editImageUpload[ad.id]?.uploading
                        ? "上传中..."
                        : formState[ad.id].imageUrl
                        ? "重新上传图片"
                        : "上传图片"
                    }}
                  </button>
                  <input
                    :id="`ad-image-${ad.id}`"
                    type="file"
                    accept="image/*"
                    hidden
                    :ref="(el) => registerEditFileInput(ad.id, el as HTMLInputElement | null)"
                    @change="onEditImageSelected(ad.id, $event)"
                  />
                  <span v-if="editImageUpload[ad.id]?.fileName" class="upload-info">
                    {{ editImageUpload[ad.id]?.fileName }}
                  </span>
                  <input
                    v-model="formState[ad.id].imageUrl"
                    type="text"
                    readonly
                    maxlength="255"
                    placeholder="请上传图片"
                  />
                </div>
                <p v-if="editImageUpload[ad.id]?.error" class="error">{{ editImageUpload[ad.id]?.error }}</p>
                <div v-if="formState[ad.id].imageUrl" class="image-preview">
                  <img :src="formState[ad.id].imageUrl" :alt="formState[ad.id].title" />
                </div>
              </div>
              <div class="form-row">
                <label :for="`ad-target-${ad.id}`">跳转链接</label>
                <input
                  :id="`ad-target-${ad.id}`"
                  v-model="formState[ad.id].targetUrl"
                  type="url"
                  maxlength="255"
                  required
                  @input="onFieldChange(ad.id)"
                />
              </div>
              <div class="form-row row-inline">
                <div class="inline-field">
                  <label :for="`ad-order-${ad.id}`">排序值</label>
                  <input
                    :id="`ad-order-${ad.id}`"
                    v-model.number="formState[ad.id].displayOrder"
                    type="number"
                    min="0"
                    @input="onFieldChange(ad.id)"
                  />
                </div>
                <label class="toggle">
                  <input type="checkbox" v-model="formState[ad.id].active" @change="onFieldChange(ad.id)" />
                  <span>启用</span>
                </label>
              </div>
            </div>
            <div class="ad-form-footer">
              <div class="status">
                <p v-if="rowStatus[ad.id]?.error" class="error">{{ rowStatus[ad.id]?.error }}</p>
                <p v-else-if="rowStatus[ad.id]?.success" class="success">{{ rowStatus[ad.id]?.success }}</p>
                <small class="meta">
                  排序：{{ formState[ad.id].displayOrder }} ·
                  {{ formState[ad.id].active ? "启用中" : "已停用" }} ·
                  更新于 {{ formatDate(ad.updatedAt) }}
                </small>
              </div>
              <div class="ad-actions">
                <button type="submit" :disabled="savingId === ad.id">
                  {{ savingId === ad.id ? "保存中..." : "保存" }}
                </button>
                <button type="button" class="delete" :disabled="deletingId === ad.id" @click="removeAd(ad)">
                  {{ deletingId === ad.id ? "删除中..." : "删除" }}
                </button>
              </div>
            </div>
          </form>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useHomeAdStore } from "@/stores/homeAds";
import { fetchAdminHomeAds, createHomeAd, updateHomeAd, deleteHomeAd } from "@/api/homeAds";
import { uploadMedia } from "@/api/media";
import type { HomeAd } from "@/types/homeAd";

interface AdFormModel {
  title: string;
  imageUrl: string;
  targetUrl: string;
  displayOrder: number;
  active: boolean;
}

interface UploadState {
  uploading: boolean;
  error: string | null;
  fileName: string | null;
}

const homeAdStore = useHomeAdStore();

const createImageInput = ref<HTMLInputElement | null>(null);
const createImageUpload = reactive<UploadState>({
  uploading: false,
  error: null,
  fileName: null,
});

const editImageUpload = reactive<Record<number, UploadState>>({});
const adFileInputs = new Map<number, HTMLInputElement>();

const ads = ref<HomeAd[]>([]);
const loading = ref(false);
const listError = ref<string | null>(null);

const createForm = reactive<AdFormModel>({
  title: "",
  imageUrl: "",
  targetUrl: "",
  displayOrder: 0,
  active: true,
});
const creating = ref(false);
const createError = ref<string | null>(null);
const createSuccess = ref<string | null>(null);

const formState = reactive<Record<number, AdFormModel>>({});
const rowStatus = reactive<Record<number, { error: string | null; success: string | null }>>({});
const savingId = ref<number | null>(null);
const deletingId = ref<number | null>(null);

const sanitizeAdList = (items: HomeAd[]): HomeAd[] => {
  if (!Array.isArray(items) || !items.length) {
    return [];
  }
  const map = new Map<number, HomeAd>();
  items.forEach((raw) => {
    if (!raw) return;
    const id = Number((raw as any).id ?? raw.id);
    if (!Number.isFinite(id)) return;
    const title = typeof raw.title === "string" ? raw.title.trim() : "";
    const imageUrl = typeof raw.imageUrl === "string" ? raw.imageUrl.trim() : "";
    const targetUrl = typeof raw.targetUrl === "string" ? raw.targetUrl.trim() : "";
    const orderValue = Number((raw as any).displayOrder ?? raw.displayOrder ?? 0);
    const displayOrder = Number.isFinite(orderValue) ? Math.max(0, Math.floor(orderValue)) : 0;
    const active = Boolean((raw as any).active ?? raw.active);
    const createdAt = typeof raw.createdAt === "string" ? raw.createdAt : "";
    const updatedAt = typeof raw.updatedAt === "string" ? raw.updatedAt : createdAt;
    if (!title || !imageUrl || !targetUrl) {
      return;
    }
    map.set(id, {
      ...raw,
      id,
      title,
      imageUrl,
      targetUrl,
      displayOrder,
      active,
      createdAt,
      updatedAt,
    });
  });
  return Array.from(map.values()).sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) {
      return a.displayOrder - b.displayOrder;
    }
    const updatedDiff = Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
    if (!Number.isNaN(updatedDiff) && updatedDiff !== 0) {
      return updatedDiff;
    }
    return a.id - b.id;
  });
};

const resetFormState = (items: HomeAd[]) => {
  Object.keys(formState).forEach((key) => delete formState[Number(key)]);
  Object.keys(rowStatus).forEach((key) => delete rowStatus[Number(key)]);
  Object.keys(editImageUpload).forEach((key) => delete editImageUpload[Number(key)]);
  adFileInputs.clear();
  items.forEach((ad) => {
    formState[ad.id] = {
      title: ad.title,
      imageUrl: ad.imageUrl,
      targetUrl: ad.targetUrl,
      displayOrder: ad.displayOrder,
      active: ad.active,
    };
    rowStatus[ad.id] = { error: null, success: null };
    editImageUpload[ad.id] = { uploading: false, error: null, fileName: null };
  });
};

const ensureEditUploadEntry = (adId: number): UploadState => {
  if (!editImageUpload[adId]) {
    editImageUpload[adId] = { uploading: false, error: null, fileName: null };
  }
  return editImageUpload[adId];
};

const registerEditFileInput = (adId: number, el: HTMLInputElement | null) => {
  if (el) {
    adFileInputs.set(adId, el);
  } else {
    adFileInputs.delete(adId);
  }
};

const triggerCreateImagePicker = () => {
  createImageUpload.error = null;
  clearCreateFeedback();
  createImageInput.value?.click();
};

const onCreateImageSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) {
    return;
  }
  clearCreateFeedback();
  createImageUpload.error = null;
  try {
    createImageUpload.uploading = true;
    const [uploaded] = await uploadMedia([file]);
    if (!uploaded || uploaded.mediaType !== "image") {
      createImageUpload.error = "请上传图片文件";
      return;
    }
    const url = typeof uploaded.url === "string" ? uploaded.url.trim() : "";
    if (!url) {
      createImageUpload.error = "上传失败，请重试";
      return;
    }
    createForm.imageUrl = url;
    createImageUpload.fileName = uploaded.originalFilename ?? file.name;
  } catch (error: any) {
    createImageUpload.error = error?.response?.data?.message ?? "上传失败，请重试";
  } finally {
    createImageUpload.uploading = false;
  }
};

const triggerEditImagePicker = (adId: number) => {
  const state = ensureEditUploadEntry(adId);
  state.error = null;
  const input = adFileInputs.get(adId);
  input?.click();
};

const onEditImageSelected = async (adId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) {
    return;
  }
  const state = ensureEditUploadEntry(adId);
  state.error = null;
  const model = formState[adId];
  if (!model) {
    return;
  }
  updateRowStatus(adId, { error: null, success: null });
  try {
    state.uploading = true;
    const [uploaded] = await uploadMedia([file]);
    if (!uploaded || uploaded.mediaType !== "image") {
      state.error = "请上传图片文件";
      return;
    }
    const url = typeof uploaded.url === "string" ? uploaded.url.trim() : "";
    if (!url) {
      state.error = "上传失败，请重试";
      return;
    }
    model.imageUrl = url;
    state.fileName = uploaded.originalFilename ?? file.name;
    onFieldChange(adId);
  } catch (error: any) {
    state.error = error?.response?.data?.message ?? "上传失败，请重试";
  } finally {
    state.uploading = false;
  }
};

const updateRowStatus = (id: number, message: { error?: string | null; success?: string | null }) => {
  if (!rowStatus[id]) {
    rowStatus[id] = { error: null, success: null };
  }
  rowStatus[id].error = message.error ?? null;
  rowStatus[id].success = message.success ?? null;
};

const formatDate = (value: string) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleString();
};

const loadAds = async () => {
  if (loading.value) return;
  loading.value = true;
  listError.value = null;
  try {
    const data = await fetchAdminHomeAds();
    const sanitized = sanitizeAdList(data);
    ads.value = sanitized;
    resetFormState(sanitized);
    homeAdStore.setAds(sanitized);
  } catch (error: any) {
    listError.value = error?.response?.data?.message ?? "加载广告失败";
  } finally {
    loading.value = false;
  }
};

const clearCreateFeedback = () => {
  createError.value = null;
  createSuccess.value = null;
  createImageUpload.error = null;
};

const submitCreate = async () => {
  if (creating.value) return;
  if (createImageUpload.uploading) {
    createError.value = "图片上传中，请稍候";
    createSuccess.value = null;
    return;
  }
  const title = createForm.title.trim();
  const imageUrl = createForm.imageUrl.trim();
  const targetUrl = createForm.targetUrl.trim();
  const displayOrder = Number.isFinite(createForm.displayOrder) ? Math.max(0, Math.floor(createForm.displayOrder)) : 0;
  if (!title || !imageUrl || !targetUrl) {
    createError.value = "请完整填写广告信息";
    createSuccess.value = null;
    return;
  }
  creating.value = true;
  createError.value = null;
  createSuccess.value = null;
  try {
    await createHomeAd({
      title,
      imageUrl,
      targetUrl,
      displayOrder,
      active: createForm.active,
    });
    createSuccess.value = "广告已创建";
    createForm.title = "";
    createForm.imageUrl = "";
    createForm.targetUrl = "";
    createForm.displayOrder = 0;
    createForm.active = true;
    createImageUpload.fileName = null;
    createImageUpload.error = null;
    if (createImageInput.value) {
      createImageInput.value.value = "";
    }
    await loadAds();
  } catch (error: any) {
    createError.value = error?.response?.data?.message ?? "创建失败";
  } finally {
    creating.value = false;
  }
};

const onFieldChange = (adId: number) => {
  if (rowStatus[adId]) {
    rowStatus[adId].error = null;
    rowStatus[adId].success = null;
  }
};

const saveAd = async (adId: number) => {
  const model = formState[adId];
  if (!model || savingId.value === adId) {
    return;
  }
  const uploadState = editImageUpload[adId];
  if (uploadState?.uploading) {
    updateRowStatus(adId, { error: "图片上传中，请稍候" });
    return;
  }
  const title = model.title.trim();
  const imageUrl = model.imageUrl.trim();
  const targetUrl = model.targetUrl.trim();
  const displayOrder = Number.isFinite(model.displayOrder) ? Math.max(0, Math.floor(model.displayOrder)) : 0;
  if (!title || !imageUrl || !targetUrl) {
    updateRowStatus(adId, { error: "请完整填写必填项" });
    return;
  }
  savingId.value = adId;
  updateRowStatus(adId, { error: null, success: null });
  try {
    const updated = await updateHomeAd(adId, {
      title,
      imageUrl,
      targetUrl,
      displayOrder,
      active: model.active,
    });
    const merged = sanitizeAdList([
      ...ads.value.filter((item) => item.id !== adId),
      updated,
    ]);
    ads.value = merged;
    resetFormState(merged);
    updateRowStatus(adId, { success: "已保存" });
    homeAdStore.setAds(merged);
  } catch (error: any) {
    updateRowStatus(adId, { error: error?.response?.data?.message ?? "保存失败" });
  } finally {
    savingId.value = null;
  }
};

const removeAd = async (ad: HomeAd) => {
  if (deletingId.value === ad.id) {
    return;
  }
  if (!window.confirm(`确定要删除广告「${ad.title}」吗？`)) {
    return;
  }
  deletingId.value = ad.id;
  updateRowStatus(ad.id, { error: null, success: null });
  try {
    await deleteHomeAd(ad.id);
    const remaining = sanitizeAdList(ads.value.filter((item) => item.id !== ad.id));
    ads.value = remaining;
    resetFormState(remaining);
    homeAdStore.setAds(remaining);
  } catch (error: any) {
    updateRowStatus(ad.id, { error: error?.response?.data?.message ?? "删除失败" });
  } finally {
    deletingId.value = null;
  }
};

const refreshAds = async () => {
  await loadAds();
};

onMounted(() => {
  void loadAds();
});
</script>

<style scoped>
.admin-home-ads {
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

.admin-header button {
  border: none;
  border-radius: 999px;
  padding: 0.45rem 1.4rem;
  background: rgba(99, 102, 241, 0.25);
  color: #c7d2fe;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.admin-header button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-header button:not(:disabled):hover {
  background: rgba(99, 102, 241, 0.4);
}

.card {
  background: rgba(15, 23, 42, 0.7);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1.5rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
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
  font-weight: 500;
  color: #cbd5f5;
}

.form-row input {
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  padding: 0.6rem 0.85rem;
}

.image-upload-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form .upload-button,
.ad-form .upload-button {
  align-self: flex-start;
  background: rgba(59, 130, 246, 0.25);
  border-radius: 999px;
  border: none;
  padding: 0.45rem 1.2rem;
  color: #bfdbfe;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.form .upload-button:disabled,
.ad-form .upload-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form .upload-button:not(:disabled):hover,
.ad-form .upload-button:not(:disabled):hover {
  background: rgba(59, 130, 246, 0.45);
}

.upload-info {
  font-size: 0.85rem;
  color: #94a3b8;
}

.image-preview {
  display: flex;
  align-items: center;
  margin-top: 0.25rem;
}

.image-preview img {
  max-width: 220px;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  object-fit: cover;
}

.row-inline {
  flex-direction: row;
  align-items: center;
  gap: 1rem;
}

.inline-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  color: #cbd5f5;
}

.toggle input {
  width: 18px;
  height: 18px;
}

.form button,
.ad-actions button {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1.4rem;
  background: rgba(59, 130, 246, 0.3);
  color: #bfdbfe;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.form button:disabled,
.ad-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form button:not(:disabled):hover,
.ad-actions button:not(:disabled):hover {
  background: rgba(59, 130, 246, 0.45);
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-heading span {
  color: #94a3b8;
  font-size: 0.9rem;
}

.ad-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.ad-item {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 1.25rem;
  padding: 1.25rem;
  border-radius: 1.25rem;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.ad-preview img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.ad-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ad-form-grid {
  display: grid;
  gap: 1rem;
}

@media (min-width: 960px) {
  .ad-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.ad-form-footer {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.status .meta {
  display: block;
  color: #94a3b8;
  margin-top: 0.25rem;
  font-size: 0.85rem;
}

.ad-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.ad-actions .delete {
  background: rgba(239, 68, 68, 0.25);
  color: #fecaca;
}

.error {
  color: #fca5a5;
  margin: 0;
}

.success {
  color: #a7f3d0;
  margin: 0;
}

.empty {
  color: #94a3b8;
  margin: 0;
}

@media (max-width: 768px) {
  .ad-item {
    grid-template-columns: 1fr;
  }

  .ad-preview img {
    height: 180px;
  }
}
</style>
