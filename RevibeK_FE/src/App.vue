<script setup>
import { onMounted } from "vue"
import { useAuthStore } from "@/stores/auth"
import ToastHost from "@/components/common/ToastHost.vue"

const auth = useAuthStore()

onMounted(() => {
  auth.restoreSession()
})
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <ToastHost />
</template>
