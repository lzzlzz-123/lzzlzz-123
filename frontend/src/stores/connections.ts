import { defineStore } from "pinia";
import api from "@/api/client";
import type { UserConnections, UserSummary } from "@/types/user";

interface ConnectionsState {
  followers: UserSummary[];
  followees: UserSummary[];
  status: "idle" | "loading" | "error";
  error: string | null;
  loaded: boolean;
}

const normalizeUserSummary = (raw: any): UserSummary => {
  const idValue = typeof raw?.id === "number" ? raw.id : Number(raw?.id);
  return {
    id: Number.isFinite(idValue) ? idValue : 0,
    username: typeof raw?.username === "string" ? raw.username : "",
    displayName: typeof raw?.displayName === "string" ? raw.displayName : "",
    avatarUrl: typeof raw?.avatarUrl === "string" ? raw.avatarUrl : raw?.avatarUrl ?? null,
    admin: Boolean(raw?.admin),
  };
};

const normalizeConnections = (raw: any): UserConnections => {
  const followers = Array.isArray(raw?.followers) ? raw.followers.map(normalizeUserSummary) : [];
  const followees = Array.isArray(raw?.followees) ? raw.followees.map(normalizeUserSummary) : [];
  return { followers, followees };
};

export const useConnectionsStore = defineStore("connections", {
  state: (): ConnectionsState => ({
    followers: [],
    followees: [],
    status: "idle",
    error: null,
    loaded: false,
  }),
  actions: {
    async fetch(force = false) {
      if (this.status === "loading") return;
      if (this.loaded && !force) return;
      this.status = "loading";
      this.error = null;
      try {
        const { data } = await api.get<UserConnections>("/users/me/connections");
        const normalized = normalizeConnections(data);
        this.followers = normalized.followers;
        this.followees = normalized.followees;
        this.status = "idle";
        this.loaded = true;
      } catch (error: any) {
        this.status = "error";
        this.error = error?.response?.data?.message ?? "获取联系人失败";
        throw error;
      }
    },
    reset() {
      this.followers = [];
      this.followees = [];
      this.status = "idle";
      this.error = null;
      this.loaded = false;
    },
  },
});
