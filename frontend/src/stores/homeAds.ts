import { defineStore } from "pinia";
import { fetchHomeAds } from "@/api/homeAds";
import type { HomeAd } from "@/types/homeAd";

const MAX_HOME_ADS = 12;

const sanitizeAds = (items: HomeAd[]): HomeAd[] => {
  if (!Array.isArray(items) || !items.length) {
    return [];
  }
  const map = new Map<number, HomeAd>();
  items.forEach((raw) => {
    if (!raw) return;
    const id = Number((raw as HomeAd).id ?? (raw as any)?.id);
    if (!Number.isFinite(id)) return;
    const title = typeof raw.title === "string" ? raw.title.trim() : "";
    const imageUrl = typeof raw.imageUrl === "string" ? raw.imageUrl.trim() : "";
    const targetUrl = typeof raw.targetUrl === "string" ? raw.targetUrl.trim() : "";
    const rawOrder = Number((raw as any).displayOrder ?? raw.displayOrder ?? 0);
    const displayOrder = Number.isFinite(rawOrder) ? Math.max(0, Math.floor(rawOrder)) : 0;
    const active = Boolean((raw as any).active ?? raw.active ?? true);
    const createdAt = typeof raw.createdAt === "string" ? raw.createdAt : "";
    const updatedAt = typeof raw.updatedAt === "string" ? raw.updatedAt : createdAt;
    if (!title || !imageUrl || !targetUrl || !active) {
      return;
    }
    map.set(id, {
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
  const list = Array.from(map.values());
  list.sort((a, b) => {
    if (a.displayOrder !== b.displayOrder) {
      return a.displayOrder - b.displayOrder;
    }
    const updatedDiff = Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
    if (!Number.isNaN(updatedDiff) && updatedDiff !== 0) {
      return updatedDiff;
    }
    return a.id - b.id;
  });
  return list.slice(0, MAX_HOME_ADS);
};

interface HomeAdState {
  ads: HomeAd[];
  loading: boolean;
  loaded: boolean;
  error: string | null;
}

export const useHomeAdStore = defineStore("homeAds", {
  state: (): HomeAdState => ({
    ads: [],
    loading: false,
    loaded: false,
    error: null,
  }),
  actions: {
    async fetchAds(force = false) {
      if (this.loading) return;
      if (this.loaded && !force) return;
      this.loading = true;
      this.error = null;
      try {
        const data = await fetchHomeAds();
        this.ads = sanitizeAds(data);
        this.loaded = true;
        this.error = null;
      } catch (error: any) {
        this.error = error?.response?.data?.message ?? "广告加载失败";
      } finally {
        this.loading = false;
      }
    },
    setAds(items: HomeAd[]) {
      this.ads = sanitizeAds(items);
      this.loaded = true;
      this.error = null;
    },
    reset() {
      this.ads = [];
      this.loaded = false;
      this.error = null;
    },
  },
});
