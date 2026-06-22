import { createRouter, createWebHistory } from "vue-router"
import { useAuthStore } from "@/stores/auth"

const routes = [
  {
    path: "/",
    name: "landing",
    component: () => import("@/pages/LandingPage.vue"),
  },
  {
    path: "/login",
    name: "login",
    component: () => import("@/pages/LoginPage.vue"),
  },
  {
    path: "/signup",
    name: "signup",
    component: () => import("@/pages/SignupPage.vue"),
  },
  {
    path: "/oauth/callback",
    name: "oauth-callback",
    component: () => import("@/pages/OAuthCallbackPage.vue"),
  },
  {
    path: "/main",
    name: "main",
    component: () => import("@/pages/MainPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/radio/story",
    name: "radio-story",
    component: () => import("@/pages/RadioStoryInputPage.vue"),
  },
  {
    // 리바이브닝 — public radio story feed (no auth required to browse)
    path: "/revibening",
    name: "revibening",
    component: () => import("@/pages/RevibeningPage.vue"),
  },
  // aliases kept from earlier specs
  {
    path: "/radio/input",
    redirect: "/radio/story",
  },
  {
    path: "/radio/create",
    redirect: "/radio/story",
  },
  {
    path: "/radio/generating",
    name: "radio-generating",
    component: () => import("@/pages/RadioGeneratingPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/radio/result/:id",
    name: "radio-result",
    component: () => import("@/pages/RadioResultPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/playlist/result/:id",
    name: "playlist-result",
    component: () => import("@/pages/PlaylistResultPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/radio/history",
    redirect: "/me?tab=radio",
  },
  // alias /radio/:id from original spec
  {
    path: "/radio/:id",
    redirect: (to) => `/radio/result/${to.params.id}`,
  },
  {
    path: "/songs",
    name: "songs",
    component: () => import("@/pages/SongPage.vue"),
  },
  {
    path: "/playlists",
    name: "playlists",
    component: () => import("@/pages/PlaylistPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/playlists/:id",
    name: "playlist-detail",
    component: () => import("@/pages/PlaylistDetailPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/me",
    name: "me",
    component: () => import("@/pages/MyPage.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/pages/NotFoundPage.vue"),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.initialized) auth.restoreSession()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } }
  }
  return true
})

export default router
