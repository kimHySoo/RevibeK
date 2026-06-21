<script setup>
const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  size: { type: Number, default: 18 },
})
const emit = defineEmits(["update:modelValue"])

const stars = [1, 2, 3, 4, 5]

function set(n) {
  if (props.readonly) return
  emit("update:modelValue", n)
}
</script>

<template>
  <div class="stars" :class="{ readonly }" role="img" :aria-label="`별점 ${modelValue}점`">
    <button
      v-for="n in stars"
      :key="n"
      type="button"
      class="star"
      :class="{ filled: n <= modelValue }"
      :disabled="readonly"
      :aria-label="`${n}점`"
      @click="set(n)"
    >
      <svg :width="size" :height="size" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 2.5l2.9 5.9 6.5.95-4.7 4.58 1.1 6.47L12 17.9 6.2 20.9l1.1-6.47L2.6 9.85l6.5-.95z" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.star {
  background: transparent;
  border: none;
  padding: 0;
  line-height: 0;
  color: rgba(255, 255, 255, 0.22);
  cursor: pointer;
  transition: color 0.12s ease, transform 0.12s ease;
}
.stars:not(.readonly) .star:hover {
  transform: scale(1.12);
}
.star.filled {
  color: #ffd24a;
}
.stars.readonly .star {
  cursor: default;
}
</style>
