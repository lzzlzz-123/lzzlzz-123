import { defineStore } from "pinia";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { useHotspotStore } from "@/stores/hotspot";
import { HOTSPOT_THRESHOLD, LIKE_HEAT_WEIGHT } from "@/constants/heat";
import type { TimelinePost } from "@/types/post";

interface TimelineState {
  posts: TimelinePost[];
  page: number;
  size: number;
  hasMore: boolean;
  loading: boolean;
  error: string | null;
}

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
    async createPost(payload: { content: string; mediaUrls: string[] }) {
      const authStore = useAuthStore();
      if (!authStore.isAuthenticated) {
        throw new Error("请先登录");
      }
      const { data } = await api.post("/posts", payload);
      this.prepend(data);
      return data as TimelinePost;
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
