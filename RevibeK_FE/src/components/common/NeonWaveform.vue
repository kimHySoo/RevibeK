<script setup>
defineProps({
  bars: { type: Number, default: 28 },
  active: { type: Boolean, default: true },
})
</script>

<template>
  <div class="wave" :class="{ paused: !active }" aria-hidden="true">
    <span
      v-for="n in bars"
      :key="n"
      class="wave-bar"
      :style="{
        animationDelay: `${(n % 7) * 0.09}s`,
        animationDuration: `${0.7 + (n % 5) * 0.12}s`,
      }"
    ></span>
  </div>
</template>

<style scoped>
.wave {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 56px;
}
.wave-bar {
  flex: 1;
  min-width: 3px;
  height: 100%;
  border-radius: 999px;
  background: var(--grad-neon);
  transform-origin: center;
  animation: wave 1s ease-in-out infinite;
}
.wave.paused .wave-bar {
  animation-play-state: paused;
  transform: scaleY(0.3);
}
</style>
