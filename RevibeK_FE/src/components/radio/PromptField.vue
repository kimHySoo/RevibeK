<script setup>
defineProps({
  modelValue: { type: String, default: "" },
  maxlength: { type: Number, default: 200 },
})
defineEmits(["update:modelValue"])
</script>

<template>
  <div class="prompt-field">
    <textarea
      :value="modelValue"
      :maxlength="maxlength"
      rows="3"
      class="prompt-input"
      placeholder="예) 비 오는 새벽, 드라이브하며 듣고 싶은 시티팝"
      @input="$emit('update:modelValue', $event.target.value)"
    ></textarea>
    <span class="prompt-count">{{ modelValue.length }}/{{ maxlength }}</span>
  </div>
</template>

<style scoped>
.prompt-field {
  position: relative;
}
.prompt-input {
  width: 100%;
  resize: none;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.5;
  font-family: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.prompt-input:focus {
  outline: none;
  border-color: var(--neon-cyan);
  box-shadow: 0 0 0 3px rgba(0, 224, 255, 0.12);
}
.prompt-input::placeholder {
  color: var(--text-muted);
}
.prompt-count {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
