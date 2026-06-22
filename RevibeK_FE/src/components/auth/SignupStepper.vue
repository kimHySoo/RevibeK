<script setup>
defineProps({
  steps: { type: Array, required: true },
  current: { type: Number, default: 0 },
})
</script>

<template>
  <ol class="stepper">
    <li
      v-for="(s, i) in steps"
      :key="i"
      class="step"
      :class="{ 'is-done': i < current, 'is-active': i === current }"
    >
      <span class="step-dot">{{ i < current ? "✓" : i + 1 }}</span>
      <span class="step-label">{{ s }}</span>
    </li>
  </ol>
</template>

<style scoped>
.stepper {
  display: flex;
  gap: 8px;
  list-style: none;
  margin-bottom: 8px;
}
.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
}
.step::after {
  content: "";
  position: absolute;
  top: 13px;
  left: 60%;
  width: 80%;
  height: 2px;
  background: var(--card-border);
}
.step:last-child::after {
  display: none;
}
.step.is-done::after {
  background: var(--neon-purple);
}
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid var(--card-border);
  background: var(--bg-deep);
  color: var(--text-muted);
  z-index: 1;
}
.step.is-active .step-dot {
  border-color: var(--neon-magenta);
  color: var(--text-primary);
  box-shadow: var(--glow-magenta);
}
.step.is-done .step-dot {
  background: var(--grad-neon);
  color: #0a0a18;
  border-color: transparent;
}
.step-label {
  font-size: 12px;
  color: var(--text-muted);
}
.step.is-active .step-label {
  color: var(--text-primary);
}
</style>
