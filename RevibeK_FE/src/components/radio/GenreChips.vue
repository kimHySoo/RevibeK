<script setup>
defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])
const genres = ["댄스", "발라드", "R&B", "힙합", "아이돌", "OST"]
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
