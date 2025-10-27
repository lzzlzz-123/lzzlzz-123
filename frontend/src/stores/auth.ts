import { defineStore } from "pinia";
import api, { setAuthToken } from "@/api/client";

export interface AuthUser {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  status: "idle" | "loading" | "error";
  error: string | null;
}

const TOKEN_KEY = "weiboblog/token";
const USER_KEY = "weiboblog/user";

const loadInitialState = (): Pick<AuthState, "token" | "user"> => {
  if (typeof window === "undefined") {
    return { token: null, user: null };
  }
  const token = localStorage.getItem(TOKEN_KEY);
  const userRaw = localStorage.getItem(USER_KEY);
  const user = userRaw ? (JSON.parse(userRaw) as AuthUser) : null;
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
    setCredentials(token: string, user: AuthUser) {
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
        this.user = data;
        if (typeof window !== "undefined") {
          localStorage.setItem(USER_KEY, JSON.stringify(data));
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
