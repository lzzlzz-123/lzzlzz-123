import { defineStore } from "pinia";
import api from "@/api/client";
import { HOTSPOT_THRESHOLD } from "@/constants/heat";
import type { TimelinePost } from "@/types/post";

const sortHotspotPosts = (a: TimelinePost, b: TimelinePost) => {
  if (b.heat !== a.heat) {
    return b.heat - a.heat;
  }
  const updatedDiff = new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime();
  if (!Number.isNaN(updatedDiff) && updatedDiff !== 0) {
    return updatedDiff;
  }
  const createdDiff = new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  if (!Number.isNaN(createdDiff) && createdDiff !== 0) {
    return createdDiff;
  }
  return b.id - a.id;
};

interface HotspotState {
  posts: TimelinePost[];
  page: number;
  size: number;
  hasMore: boolean;
  loading: boolean;
  error: string | null;
  initialized: boolean;
}

export const useHotspotStore = defineStore("hotspot", {
  state: (): HotspotState => ({
    posts: [],
    page: 0,
    size: 10,
    hasMore: true,
    loading: false,
    error: null,
    initialized: false,
  }),
  actions: {
    async loadInitial(force = false) {
      if (this.loading) return;
      if (this.initialized && !force) return;
      this.page = 0;
      this.hasMore = true;
      this.error = null;
      await this.fetchMore();
      this.initialized = true;
    },
    async fetchMore() {
      if (this.loading || !this.hasMore) return;
      this.loading = true;
      this.error = null;
      const requestedPage = this.page;
      try {
        const { data } = await api.get("/posts/hotspot", {
          params: { page: requestedPage, size: this.size },
        });
        const fetched: TimelinePost[] = data.content ?? [];
        if (requestedPage === 0 && fetched.length === 0) {
          this.posts = [];
        } else {
          this.syncPosts(fetched);
        }
        this.hasMore = !data.last;
        this.page = requestedPage + 1;
      } catch (error: any) {
        this.error = error?.response?.data?.message ?? "加载失败";
      } finally {
        this.loading = false;
        this.initialized = true;
      }
    },
    async refresh() {
      await this.loadInitial(true);
    },
    syncPost(post: TimelinePost) {
      this.syncPosts([post]);
    },
    syncPosts(posts: TimelinePost[]) {
      const map = new Map<number, TimelinePost>();
      this.posts.forEach((existing) => {
        map.set(existing.id, existing);
      });
      posts.forEach((post) => {
        if (post.heat >= HOTSPOT_THRESHOLD) {
          const existing = map.get(post.id);
          map.set(post.id, existing ? { ...existing, ...post } : { ...post });
        } else {
          map.delete(post.id);
        }
      });
      this.posts = Array.from(map.values()).sort(sortHotspotPosts);
    },
    removePost(postId: number) {
      const next = this.posts.filter((post) => post.id !== postId);
      if (next.length !== this.posts.length) {
        this.posts = next;
      }
    },
  },
});
