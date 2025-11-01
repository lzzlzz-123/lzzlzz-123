import { defineStore } from "pinia";
import api, { setAuthToken } from "@/api/client";

export interface AuthUser {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
  admin: boolean;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  status: "idle" | "loading" | "error";
  error: string | null;
}

const TOKEN_KEY = "weiboblog/token";
const USER_KEY = "weiboblog/user";

const normalizeUserSummary = (input: any): AuthUser => {
  const idValue = typeof input?.id === "number" ? input.id : Number(input?.id);
  return {
    id: Number.isFinite(idValue) ? idValue : 0,
    username: typeof input?.username === "string" ? input.username : "",
    displayName: typeof input?.displayName === "string" ? input.displayName : "",
    avatarUrl:
      typeof input?.avatarUrl === "string" ? input.avatarUrl : input?.avatarUrl ?? null,
    admin: Boolean(input?.admin),
  };
};

const parseStoredUser = (raw: string | null): AuthUser | null => {
  if (!raw) {
    return null;
  }
  try {
    return normalizeUserSummary(JSON.parse(raw));
  } catch {
    return null;
  }
};

const loadInitialState = (): Pick<AuthState, "token" | "user"> => {
  if (typeof window === "undefined") {
    return { token: null, user: null };
  }
  const token = localStorage.getItem(TOKEN_KEY);
  const userRaw = localStorage.getItem(USER_KEY);
  const user = parseStoredUser(userRaw);
  if (token) {
    setAuthToken(token);
  }
  return { token, user };
};

const initialAuthState = loadInitialState();

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: initialAuthState.token,
    user: initialAuthState.user,
    status: "idle",
    error: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    setCredentials(token: string, userLike: unknown) {
      const user = normalizeUserSummary(userLike as any);
      this.token = token;
      this.user = user;
      setAuthToken(token);
      if (typeof window !== "undefined") {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      }
    },
    clearCredentials() {
      this.token = null;
      this.user = null;
      setAuthToken(null);
      if (typeof window !== "undefined") {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      }
    },
    async login(payload: { usernameOrEmail: string; password: string }) {
      this.status = "loading";
      this.error = null;
      try {
        const { data } = await api.post("/auth/login", payload);
        this.setCredentials(data.token, data.user);
        this.status = "idle";
      } catch (error: any) {
        this.status = "error";
        this.error = error?.response?.data?.message ?? "登录失败";
        throw error;
      }
    },
    async register(payload: { username: string; displayName: string; email: string; password: string }) {
      this.status = "loading";
      this.error = null;
      try {
        const { data } = await api.post("/auth/register", payload);
        this.setCredentials(data.token, data.user);
        this.status = "idle";
      } catch (error: any) {
        this.status = "error";
        this.error = error?.response?.data?.message ?? "注册失败";
        throw error;
      }
    },
    async fetchCurrentUser() {
      if (!this.token) return;
      try {
        const { data } = await api.get("/users/me");
        const user = normalizeUserSummary(data);
        this.user = user;
        if (typeof window !== "undefined") {
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        }
      } catch (error) {
        this.clearCredentials();
        throw error;
      }
    },
    logout() {
      this.clearCredentials();
    },
  },
});
