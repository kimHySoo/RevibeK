<script setup>
import { computed } from "vue"

const props = defineProps({
  variant: { type: String, default: "primary" }, // primary | ghost | outline | danger
  type: { type: String, default: "button" },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  size: { type: String, default: "md" }, // sm | md | lg
  to: { type: [String, Object], default: null },
})

const isLink = computed(() => !!props.to)
</script>

<template>
  <RouterLink
    v-if="isLink"
    :to="to"
    class="btn"
    :class="[`btn-${variant}`, `btn-${size}`, { 'btn-block': block }]"
  >
    <slot />
  </RouterLink>
  <button
    v-else
    :type="type"
    :disabled="disabled || loading"
    class="btn"
    :class="[`btn-${variant}`, `btn-${size}`, { 'btn-block': block }]"
  >
    <span v-if="loading" class="btn-spinner" aria-hidden="true"></span>
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  font-weight: 600;
  font-size: 15px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease,
    background 0.18s ease, border-color 0.18s ease, color 0.18s ease, opacity 0.18s ease;
  white-space: nowrap;
}
.btn:focus-visible {
  outline: 2px solid var(--neon-cyan);
  outline-offset: 3px;
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.btn-sm {
  padding: 7px 14px;
  font-size: 13px;
}
.btn-md {
  padding: 12px 22px;
}
.btn-lg {
  padding: 15px 30px;
  font-size: 16px;
}
.btn-block {
  width: 100%;
}
.btn-primary {
  background: var(--grad-neon);
  color: #ffffff;
  box-shadow: 0 0 18px rgba(255, 60, 172, 0.25);
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 0 28px rgba(255, 60, 172, 0.45);
  filter: brightness(1.1);
}
.btn-primary:active:not(:disabled) {
  transform: translateY(0);
  filter: brightness(0.95);
}
.btn-outline {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
  color: #f5f5ff;
}
.btn-outline:hover:not(:disabled) {
  background: rgba(43, 223, 219, 0.12);
  border-color: rgba(43, 223, 219, 0.65);
  color: #ffffff;
}
.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
}
.btn-ghost:hover:not(:disabled) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}
.btn-danger {
  background: transparent;
  border-color: var(--danger);
  color: var(--danger);
}
.btn-danger:hover:not(:disabled) {
  background: rgba(255, 92, 122, 0.12);
}
.btn-spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(0, 0, 0, 0.25);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
.btn-primary .btn-spinner {
  border-color: rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
}
</style>
