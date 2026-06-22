<script setup>
import AppShell from "@/components/common/AppShell.vue"
import BaseButton from "@/components/common/BaseButton.vue"
</script>

<template>
  <AppShell>
    <div class="nf">
      <p class="nf-code">404</p>
      <h1 class="nf-title">페이지를 찾을 수 없어요</h1>
      <p class="nf-sub text-secondary">주소가 바뀌었거나 사라진 페이지일 수 있어요.</p>
      <BaseButton :to="{ name: 'landing' }" size="lg">홈으로 돌아가기</BaseButton>
    </div>
  </AppShell>
</template>

<style scoped>
.nf {
  max-width: 480px;
  margin: 0 auto;
  padding: 120px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.nf-code {
  font-size: 72px;
  font-weight: 800;
  background: var(--grad-neon);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.nf-title {
  font-size: 26px;
  font-weight: 800;
}
.nf-sub {
  margin-bottom: 16px;
}
</style>
