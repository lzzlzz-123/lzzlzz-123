import { defineStore } from "pinia";
import api from "@/api/client";
import { useAuthStore } from "@/stores/auth";

export interface TimelinePost {
  id: number;
  content: string;
  mediaUrls: string[];
  createdAt: string;
  updatedAt: string;
  author: {
    id: number;
    username: string;
    displayName: string;
    avatarUrl?: string | null;
  };
  likeCount: number;
  commentCount: number;
  likedByCurrentUser: boolean;
}

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
      try {
        const { data } = await api.get("/posts/feed", {
          params: { page: this.page, size: this.size },
        });
        if (this.page === 0) {
          this.posts = data.content;
        } else {
          this.posts.push(...data.content);
        }
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
      const post = this.posts.find((p) => p.id === postId) ?? fallback;
      if (!post) return;
      if (post.likedByCurrentUser) {
        await api.delete(`/posts/${postId}/like`);
        post.likedByCurrentUser = false;
        post.likeCount = Math.max(0, post.likeCount - 1);
      } else {
        await api.post(`/posts/${postId}/like`);
        post.likedByCurrentUser = true;
        post.likeCount += 1;
      }
      if (fallback && fallback !== post) {
        fallback.likedByCurrentUser = post.likedByCurrentUser;
        fallback.likeCount = post.likeCount;
      }
    },
  },
});
