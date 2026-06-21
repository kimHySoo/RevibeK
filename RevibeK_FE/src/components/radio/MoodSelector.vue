<script setup>
import { MOOD_OPTIONS } from "@/constants/radioOptions"

defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])

const moods = MOOD_OPTIONS.map((option) => option.label)
</script>

<template>
  <div class="mood-grid">
    <button
      v-for="m in moods"
      :key="m"
      type="button"
      class="chip"
      :class="{ 'is-active': modelValue === m }"
      @click="$emit('update:modelValue', m)"
    >
      {{ m }}
    </button>
  </div>
</template>

<style scoped>
.mood-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
