<script setup>
import { ref } from "vue"
import { useRouter, useRoute } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const ui = useUiStore()
const menuOpen = ref(false)

const navLinks = [
  { name: "리바이브닝", to: "/revibening" },
  { name: "라디오 만들기", to: "/radio/story" },
  { name: "노래", to: "/songs" },
  { name: "플레이리스트", to: "/playlists" },
  { name: "마이", to: "/me" },
]

function isActive(to) {
  return route.path === to || route.path.startsWith(to + "/")
}

async function handleLogout() {
  await auth.logout()
  ui.success("로그아웃되었어요.")
  router.push("/")
}
</script>

<template>
  <header v-if="auth.isAuthenticated" class="app-header">
    <div class="container header-inner">
      <router-link to="/" class="brand" aria-label="RevibeK 홈">
        <span class="brand-mark" aria-hidden="true"></span>
        <span class="brand-name text-gradient">RevibeK</span>
      </router-link>

      <nav class="nav-desktop" aria-label="주요 메뉴">
        <router-link
          v-for="l in navLinks"
          :key="l.to"
          :to="l.to"
          class="nav-link"
          :class="{ 'is-active': isActive(l.to) }"
        >
          {{ l.name }}
        </router-link>
      </nav>

      <div class="header-actions">
        <span class="hello text-muted">{{ auth.user?.nickname || "게스트" }}님</span>
        <button class="ghost-btn" @click="handleLogout">로그아웃</button>
        <button
          class="menu-toggle"
          aria-label="메뉴 열기"
          @click="menuOpen = !menuOpen"
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>

    <transition name="fade">
      <nav v-if="menuOpen" class="nav-mobile" aria-label="모바일 메뉴">
        <router-link
          v-for="l in navLinks"
          :key="l.to"
          :to="l.to"
          class="nav-link-mobile"
          @click="menuOpen = false"
        >
          {{ l.name }}
        </router-link>
      </nav>
    </transition>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(7, 6, 15, 0.78);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--card-border);
}
.header-inner {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  font-size: 20px;
}
.brand-mark {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--grad-neon);
  box-shadow: var(--glow-magenta);
}
.nav-desktop {
  display: flex;
  gap: 4px;
  margin-left: auto;
}
.nav-link {
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.15s ease;
}
.nav-link:hover {
  color: var(--text-primary);
}
.nav-link.is-active {
  color: var(--text-primary);
  background: var(--neon-purple-soft);
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hello {
  font-size: 13px;
}
.ghost-btn {
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  color: var(--text-secondary);
  font-size: 14px;
  border: 1px solid transparent;
  background: transparent;
}
.ghost-btn:hover {
  color: var(--text-primary);
}
.solid-btn {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: var(--grad-neon);
  color: #0a0a18;
  font-weight: 600;
  font-size: 14px;
}
.menu-toggle {
  display: none;
  flex-direction: column;
  gap: 4px;
  background: transparent;
  border: none;
  padding: 6px;
}
.menu-toggle span {
  width: 22px;
  height: 2px;
  background: var(--text-primary);
  border-radius: 2px;
}
.nav-mobile {
  display: none;
  flex-direction: column;
  padding: 8px 20px 16px;
  gap: 4px;
  border-top: 1px solid var(--card-border);
}
.nav-link-mobile {
  padding: 12px 10px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
}
.nav-link-mobile:hover {
  background: var(--neon-purple-soft);
  color: var(--text-primary);
}

@media (max-width: 820px) {
  .nav-desktop {
    display: none;
  }
  .hello {
    display: none;
  }
  .menu-toggle {
    display: flex;
  }
  .nav-mobile {
    display: flex;
  }
}
</style>
