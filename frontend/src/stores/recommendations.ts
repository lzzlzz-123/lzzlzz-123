import { defineStore } from "pinia";
import api from "@/api/client";
import type { TimelinePost } from "@/types/post";

const MAX_SEEN_HISTORY = 60;
const MAX_FETCH_SIZE = 9;

const sanitizePosts = (items: TimelinePost[]): TimelinePost[] => {
  if (!Array.isArray(items) || !items.length) {
    return [];
  }
  const seen = new Set<number>();
  const sanitized: TimelinePost[] = [];
  items.forEach((raw) => {
    if (!raw) return;
    const id = Number((raw as any).id ?? raw.id);
    if (!Number.isFinite(id) || seen.has(id)) {
      return;
    }
    seen.add(id);
    const clone: TimelinePost = {
      ...raw,
      id,
      mediaUrls: Array.isArray(raw.mediaUrls)
        ? raw.mediaUrls.filter((url) => typeof url === "string" && url.trim())
        : [],
      allowedUserIds: Array.isArray((raw as any).allowedUserIds)
        ? Array.from(
            new Set(
              ((raw as any).allowedUserIds as Array<number | string | null | undefined>)
                .map((value) => Number(value))
                .filter((value) => Number.isFinite(value))
                .map((value) => Math.trunc(Number(value)))
            )
          )
        : [],
    };
    sanitized.push(clone);
  });
  return sanitized.slice(0, MAX_FETCH_SIZE);
};

const sanitizeExcludeIds = (values: number[]): number[] => {
  if (!Array.isArray(values) || !values.length) {
    return [];
  }
  const unique = new Set<number>();
  values.forEach((value) => {
    const numeric = Math.trunc(Number(value));
    if (Number.isFinite(numeric) && numeric > 0 && !unique.has(numeric)) {
      unique.add(numeric);
    }
  });
  return Array.from(unique).slice(0, MAX_SEEN_HISTORY);
};

interface RecommendationState {
  posts: TimelinePost[];
  loading: boolean;
  refreshing: boolean;
  initialized: boolean;
  error: string | null;
  seenQueue: number[];
  seenSet: Record<number, true>;
  size: number;
}

export const useRecommendationStore = defineStore("recommendations", {
  state: (): RecommendationState => ({
    posts: [],
    loading: false,
    refreshing: false,
    initialized: false,
    error: null,
    seenQueue: [],
    seenSet: {},
    size: 3,
  }),
  actions: {
    async loadInitial(force = false) {
      if (this.loading || this.refreshing) return;
      if (this.initialized && !force) return;
      if (force) {
        this.resetSeen();
      }
      this.loading = true;
      this.error = null;
      try {
        const posts = await this.fetchRecommendations(force ? [] : this.getExclusions());
        this.posts = posts;
        this.initialized = true;
      } catch (error: any) {
        this.error = error?.response?.data?.message ?? "推荐内容加载失败";
        if (!this.initialized) {
          this.posts = [];
        }
      } finally {
        this.loading = false;
      }
    },
    async refresh() {
      if (this.loading || this.refreshing) return;
      if (this.posts.length) {
        this.markSeen(this.posts);
      }
      this.refreshing = true;
      this.error = null;
      try {
        let posts = await this.fetchRecommendations(this.getExclusions());
        if (!posts.length && this.seenQueue.length) {
          this.resetSeen();
          posts = await this.fetchRecommendations([]);
        }
        this.posts = posts;
        if (!this.initialized) {
          this.initialized = true;
        }
      } catch (error: any) {
        this.error = error?.response?.data?.message ?? "刷新失败";
      } finally {
        this.refreshing = false;
      }
    },
    markSeen(posts: TimelinePost[]) {
      posts.forEach((post) => {
        const id = Math.trunc(Number(post?.id));
        if (!Number.isFinite(id) || id <= 0 || this.seenSet[id]) {
          return;
        }
        this.seenQueue.push(id);
        this.seenSet[id] = true;
        if (this.seenQueue.length > MAX_SEEN_HISTORY) {
          const removed = this.seenQueue.shift();
          if (typeof removed === "number") {
            delete this.seenSet[removed];
          }
        }
      });
    },
    resetSeen() {
      this.seenQueue = [];
      this.seenSet = {};
    },
    getExclusions(): number[] {
      return this.seenQueue.slice(-MAX_SEEN_HISTORY);
    },
    async fetchRecommendations(exclude: number[]): Promise<TimelinePost[]> {
      const sanitizedExclude = sanitizeExcludeIds(exclude);
      const params = new URLSearchParams();
      params.set("size", String(Math.max(1, Math.min(this.size, MAX_FETCH_SIZE))));
      sanitizedExclude.forEach((id) => params.append("exclude", String(id)));
      const { data } = await api.get<TimelinePost[]>("/posts/recommendations", { params });
      const sanitized = sanitizePosts(Array.isArray(data) ? data : []);
      const limit = Math.max(1, Math.min(this.size, sanitized.length || this.size));
      return sanitized.slice(0, limit);
    },
  },
});
