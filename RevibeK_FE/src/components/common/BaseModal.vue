<script setup>
import { watch } from "vue"

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: "" },
  // "default" (440px) | "lg" (760px) | "xl" (960px)
  size: { type: String, default: "default" },
})
const emit = defineEmits(["update:modelValue"])

function close() {
  emit("update:modelValue", false)
}

watch(
  () => props.modelValue,
  (open) => {
    document.body.style.overflow = open ? "hidden" : ""
  }
)
</script>

<template>
  <transition name="fade">
    <div v-if="modelValue" class="modal-backdrop" @click.self="close">
      <div class="modal card" :class="`modal--${size}`" role="dialog" aria-modal="true">
        <div class="modal-head">
          <h3 class="modal-title">{{ title }}</h3>
          <button class="modal-close" aria-label="닫기" @click="close">×</button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div v-if="$slots.footer" class="modal-foot">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(4, 3, 10, 0.72);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 100;
}
.modal {
  width: 100%;
  max-width: 440px;
  padding: 22px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal--lg {
  max-width: 760px;
  width: min(92vw, 760px);
  max-height: 85vh;
}
.modal--xl {
  max-width: 960px;
  width: min(94vw, 960px);
  max-height: 85vh;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.modal-title {
  font-size: 18px;
}
.modal-close {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 26px;
  line-height: 1;
}
.modal-close:hover {
  color: var(--text-primary);
}
.modal-foot {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
