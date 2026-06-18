<script setup>
defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])
const eras = ["2세대", "3세대", "전체"]
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
