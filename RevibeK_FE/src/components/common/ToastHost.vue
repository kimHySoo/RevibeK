<script setup>
import { useUiStore } from "@/stores/ui"
const ui = useUiStore()
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <transition-group name="fade">
      <div
        v-for="t in ui.toasts"
        :key="t.id"
        class="toast"
        :class="`toast-${t.type}`"
        @click="ui.dismiss(t.id)"
      >
        {{ t.message }}
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 200;
  width: calc(100% - 40px);
  max-width: 420px;
}
.toast {
  padding: 13px 18px;
  border-radius: var(--radius-sm);
  background: var(--card-bg-solid);
  border: 1px solid var(--card-border);
  color: var(--text-primary);
  font-size: 14px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}
.toast-success {
  border-color: var(--success);
}
.toast-error {
  border-color: var(--danger);
}
</style>
