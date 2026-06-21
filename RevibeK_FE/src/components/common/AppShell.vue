<script setup>
import AppHeader from "./AppHeader.vue"

defineProps({
  hero: { type: Boolean, default: false },
  hideFooter: { type: Boolean, default: false },
})
</script>

<template>
  <div class="shell" :class="{ 'shell-hero': hero }">
    <AppHeader />
    <main class="shell-main">
      <slot />
    </main>
    <footer v-if="!hideFooter" class="shell-footer">
      <div class="container footer-inner">
        <span class="text-muted">RevibeK · 감정으로 시작하는 AI 커버 라디오</span>
        <span class="text-muted">© 2026 RevibeK</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
  position: relative;
}
.shell-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background: var(--grad-hero);
  pointer-events: none;
  z-index: 0;
}
.shell-main {
  flex: 1;
  position: relative;
  z-index: 1;
}
.shell-footer {
  border-top: 1px solid var(--card-border);
  padding: 24px 0;
  position: relative;
  z-index: 1;
}
.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  flex-wrap: wrap;
}
</style>
