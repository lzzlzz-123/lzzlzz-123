import { defineStore } from "pinia";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { useHotspotStore } from "@/stores/hotspot";
import { HOTSPOT_THRESHOLD, LIKE_HEAT_WEIGHT } from "@/constants/heat";
import type { PostVisibility, TimelinePost } from "@/types/post";

interface TimelineState {
  posts: TimelinePost[];
  page: number;
  size: number;
  hasMore: boolean;
  loading: boolean;
  error: string | null;
}

const sanitizeAllowedUserIds = (ids?: number[]): number[] => {
  if (!ids) return [];
  const unique = new Set<number>();
  ids.forEach((value) => {
    const numeric = Number(value);
    if (Number.isFinite(numeric)) {
      unique.add(numeric);
    }
  });
  return Array.from(unique.values());
};

export const useFeedStore = defineStore("feed", {
  state: (): TimelineState => ({
    posts: [],
    page: 0,
    size: 20,
    hasMore: true,
    loading: false,
    error: null,
  }),
  actions: {
    async loadInitial() {
      this.posts = [];
      this.page = 0;
      this.hasMore = true;
      await this.fetchMore();
    },
    async fetchMore() {
      if (this.loading || !this.hasMore) return;
      this.loading = true;
      this.error = null;
      const hotspotStore = useHotspotStore();
      try {
        const { data } = await api.get("/posts/feed", {
          params: { page: this.page, size: this.size },
        });
        const fetched: TimelinePost[] = data.content ?? [];
        if (this.page === 0) {
          this.posts = fetched.map((item) => ({ ...item }));
        } else {
          fetched.forEach((item) => {
            const existing = this.posts.find((post) => post.id === item.id);
            if (existing) {
              Object.assign(existing, item);
            } else {
              this.posts.push(item);
            }
          });
        }
        hotspotStore.syncPosts(fetched);
        this.hasMore = !data.last;
        this.page += 1;
      } catch (error: any) {
        this.error = error?.response?.data?.message ?? "加载失败";
      } finally {
        this.loading = false;
      }
    },
    prepend(post: TimelinePost) {
      this.posts.unshift(post);
      useHotspotStore().syncPost(post);
    },
    updatePost(updated: TimelinePost) {
      const hotspotStore = useHotspotStore();
      const existing = this.posts.find((post) => post.id === updated.id);
      if (existing) {
        Object.assign(existing, updated);
      }
      hotspotStore.syncPost(updated);
    },
    removePost(postId: number) {
      const before = this.posts.length;
      this.posts = this.posts.filter((post) => post.id !== postId);
      if (this.posts.length !== before) {
        useHotspotStore().removePost(postId);
      }
    },
    async createPost(payload: {
      content: string;
      mediaUrls: string[];
      topicId?: number | null;
      visibility?: PostVisibility | null;
      allowedUserIds?: number[];
    }) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        throw new Error("请先登录");
      }
      const sanitizedAllowed = payload.visibility === "CUSTOM" ? sanitizeAllowedUserIds(payload.allowedUserIds) : [];
      const requestBody: Record<string, unknown> = {
        content: payload.content,
        mediaUrls: payload.mediaUrls,
        topicId: payload.topicId ?? null,
      };
      if (payload.visibility) {
        requestBody.visibility = payload.visibility;
      }
      if (payload.visibility === "CUSTOM") {
        requestBody.allowedUserIds = sanitizedAllowed;
      }
      const { data } = await api.post<TimelinePost>("/posts", requestBody);
      const created: TimelinePost = {
        ...data,
        allowedUserIds: Array.isArray((data as any)?.allowedUserIds)
          ? sanitizeAllowedUserIds((data as any).allowedUserIds)
          : sanitizedAllowed,
      };
      this.prepend(created);
      return created;
    },
    async toggleLike(postId: number, fallback?: TimelinePost) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        throw new Error("请先登录");
      }
      const hotspotStore = useHotspotStore();
      const post = this.posts.find((p) => p.id === postId) ?? fallback;
      if (!post) return;
      if (post.likedByCurrentUser) {
        await api.delete(`/posts/${postId}/like`);
        post.likedByCurrentUser = false;
        post.likeCount = Math.max(0, post.likeCount - 1);
        post.heat = Math.max(0, post.heat - LIKE_HEAT_WEIGHT);
      } else {
        await api.post(`/posts/${postId}/like`);
        post.likedByCurrentUser = true;
        post.likeCount += 1;
        post.heat += LIKE_HEAT_WEIGHT;
      }
      post.inHotspot = post.heat >= HOTSPOT_THRESHOLD;
      const nowIso = new Date().toISOString();
      post.updatedAt = nowIso;
      if (fallback && fallback !== post) {
        fallback.likedByCurrentUser = post.likedByCurrentUser;
        fallback.likeCount = post.likeCount;
        fallback.heat = post.heat;
        fallback.inHotspot = post.inHotspot;
        fallback.updatedAt = nowIso;
      }
      hotspotStore.syncPost(post);
    },
  },
});
