<script setup>
import { GENERATION_OPTIONS } from "@/constants/radioOptions"

defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])
const eras = GENERATION_OPTIONS.map((option) => option.label)
</script>

<template>
  <div class="era-chips">
    <button
      v-for="e in eras"
      :key="e"
      type="button"
      class="chip"
      :class="{ 'is-active': modelValue === e }"
      @click="$emit('update:modelValue', e)"
    >
      {{ e }}
    </button>
  </div>
</template>

<style scoped>
.era-chips {
  display: flex;
  gap: 10px;
}
</style>
