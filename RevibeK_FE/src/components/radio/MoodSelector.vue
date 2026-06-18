<script setup>
defineProps({
  modelValue: { type: String, default: "" },
})
defineEmits(["update:modelValue"])

const moods = ["지침", "설렘", "회상", "자신감", "위로", "외로움", "신남"]
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
