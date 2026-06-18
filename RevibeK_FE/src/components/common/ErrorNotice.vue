<script setup>
defineProps({
  message: { type: String, default: "문제가 발생했어요." },
})
</script>

<template>
  <div class="error-notice card" role="alert">
    <span class="dot" aria-hidden="true"></span>
    <p>{{ message }}</p>
  </div>
</template>

<style scoped>
.error-notice {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-color: var(--danger);
  color: var(--text-primary);
  font-size: 14px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--danger);
  box-shadow: 0 0 12px var(--danger);
  flex-shrink: 0;
}
</style>
