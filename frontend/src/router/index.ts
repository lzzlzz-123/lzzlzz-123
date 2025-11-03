import { createRouter, createWebHistory } from "vue-router";
import FeedView from "@/views/FeedView.vue";
import LoginView from "@/views/LoginView.vue";
import RegisterView from "@/views/RegisterView.vue";
import ProfileView from "@/views/ProfileView.vue";
import PostDetailView from "@/views/PostDetailView.vue";
import HotspotView from "@/views/HotspotView.vue";
import TopicsView from "@/views/TopicsView.vue";
import TopicDetailView from "@/views/TopicDetailView.vue";
import AdminHotspotView from "@/views/AdminHotspotView.vue";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: FeedView,
    },
    {
      path: "/hotspot",
      name: "hotspot",
      component: HotspotView,
    },
    {
      path: "/topics",
      name: "topics",
      component: TopicsView,
    },
    {
      path: "/topics/:id",
      name: "topic-detail",
      component: TopicDetailView,
      props: true,
    },
    {
      path: "/admin/hotspot",
      name: "admin-hotspot",
      component: AdminHotspotView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { requiresGuest: true },
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView,
      meta: { requiresGuest: true },
    },
    {
      path: "/profile/:id",
      name: "profile",
      component: ProfileView,
      props: true,
    },
    {
      path: "/post/:id",
      name: "post",
      component: PostDetailView,
      props: true,
    },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if ((to.meta.requiresAuth || to.meta.requiresAdmin) && authStore.isAuthenticated && !authStore.user) {
    try {
      await authStore.fetchCurrentUser();
    } catch {
      return { name: "login", query: { redirect: to.fullPath } };
    }
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    return { name: "home" };
  }
  if (to.meta.requiresAdmin && !authStore.user?.admin) {
    return { name: "home" };
  }
  return true;
});

export default router;
