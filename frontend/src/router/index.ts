import { createRouter, createWebHistory } from "vue-router";
import FeedView from "@/views/FeedView.vue";
import LoginView from "@/views/LoginView.vue";
import RegisterView from "@/views/RegisterView.vue";
import ProfileView from "@/views/ProfileView.vue";
import PostDetailView from "@/views/PostDetailView.vue";
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

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    return { name: "home" };
  }
  return true;
});

export default router;
