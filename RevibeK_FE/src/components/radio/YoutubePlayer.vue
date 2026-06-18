<script setup>
import { computed } from "vue"
import { getYoutubeId } from "@/utils/youtube"

const props = defineProps({
  // accepts a raw youtube id or any youtube url
  source: { type: String, default: "" },
  title: { type: String, default: "" },
  autoplay: { type: Boolean, default: false },
})

const youtubeId = computed(() => getYoutubeId(props.source))

const embedSrc = computed(() => {
  if (!youtubeId.value) return ""
  const params = new URLSearchParams({ rel: "0", modestbranding: "1" })
  if (props.autoplay) params.set("autoplay", "1")
  return `https://www.youtube.com/embed/${youtubeId.value}?${params.toString()}`
})
</script>

<template>
  <div class="yt-player">
    <div v-if="youtubeId" class="yt-frame">
      <iframe
        :key="embedSrc"
        :src="embedSrc"
        :title="title || 'YouTube video player'"
        frameborder="0"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
        allowfullscreen
      ></iframe>
    </div>
    <div v-else class="yt-empty">
      <p>재생할 영상을 찾을 수 없어요.</p>
    </div>
  </div>
</template>

<style scoped>
.yt-player {
  width: 100%;
}
.yt-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--card-border-strong);
  background: #000;
  box-shadow: 0 0 24px rgba(43, 223, 219, 0.18);
}
.yt-frame iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.yt-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius-md);
  border: 1px dashed var(--card-border-strong);
  background: rgba(255, 255, 255, 0.02);
  color: var(--text-muted);
  font-size: 14px;
}
</style>
