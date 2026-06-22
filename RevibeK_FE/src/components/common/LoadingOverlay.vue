<script setup>
defineProps({
  message: { type: String, default: "불러오는 중..." },
  inline: { type: Boolean, default: false },
})
</script>

<template>
  <div class="loader" :class="{ 'loader-inline': inline }">
    <div class="spinner" aria-hidden="true"></div>
    <p class="loader-text">{{ message }}</p>
  </div>
</template>

<style scoped>
.loader {
  position: fixed;
  inset: 0;
  background: rgba(4, 3, 10, 0.7);
  backdrop-filter: blur(6px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 150;
  color: var(--text-secondary);
}
.loader-inline {
  position: static;
  inset: auto;
  background: transparent;
  backdrop-filter: none;
  padding: 80px 0;
  z-index: auto;
}
.spinner {
  width: 44px;
  height: 44px;
  border: 3px solid var(--neon-purple-soft);
  border-top-color: var(--neon-magenta);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.loader-text {
  font-size: 14px;
}
</style>
