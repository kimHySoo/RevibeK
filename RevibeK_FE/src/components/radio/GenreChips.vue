<script setup>
import { GENRE_OPTIONS } from "@/constants/radioOptions"

defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])
const genres = GENRE_OPTIONS.map((option) => option.label)
</script>

<template>
  <div class="genre-chips">
    <button
      v-for="g in genres"
      :key="g"
      type="button"
      class="chip"
      :class="{ 'is-active': modelValue === g }"
      @click="$emit('update:modelValue', g)"
    >
      {{ g }}
    </button>
  </div>
</template>

<style scoped>
.genre-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
