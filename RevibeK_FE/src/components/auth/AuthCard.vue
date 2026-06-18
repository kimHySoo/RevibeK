<script setup>
import NeonWaveform from "@/components/common/NeonWaveform.vue"

defineProps({
  title: { type: String, default: "" },
  subtitle: { type: String, default: "" },
})
</script>

<template>
  <div class="auth-card card card-pad">
    <div class="auth-brand">
      <span class="brand-mark" aria-hidden="true"></span>
      <span class="text-gradient brand-name">RevibeK</span>
    </div>
    <NeonWaveform :bars="20" />
    <h1 class="auth-title">{{ title }}</h1>
    <p v-if="subtitle" class="auth-sub text-muted">{{ subtitle }}</p>
    <slot />
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  font-size: 22px;
}
.brand-mark {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--grad-neon);
  box-shadow: var(--glow-magenta);
}
.auth-title {
  font-size: 24px;
  margin-top: 4px;
}
.auth-sub {
  font-size: 14px;
  margin-top: -8px;
}
</style>
