<script setup>
import { ref, computed, onMounted } from "vue"
import { useAuthStore } from "@/stores/auth"
import { useUiStore } from "@/stores/ui"
import challengeApi from "@/api/challengeApi"
import BaseButton from "@/components/common/BaseButton.vue"

const auth = useAuthStore()
const ui = useUiStore()

const loadingTemplates = ref(true)
const templatesError = ref(false)
const templates = ref([])

const myChallenges = ref([])
const startingId = ref(null)

const isLoggedIn = computed(() => auth.isAuthenticated)

// a template is "in progress" if a joined challenge shares its title
const myTitles = computed(
  () => new Set(myChallenges.value.map((c) => c.title))
)
function isStarted(tpl) {
  return myTitles.value.has(tpl.title)
}

function progressPct(c) {
  if (!c.targetCount) return 0
  return Math.min(100, Math.round((c.progressCount / c.targetCount) * 100))
}

function toISODate(d) {
  return d.toISOString().slice(0, 10)
}
// startDate = today, endDate = today + durationDays - 1
function dateRange(durationDays) {
  const start = new Date()
  const end = new Date()
  end.setDate(end.getDate() + Math.max(1, durationDays || 1) - 1)
  return { startDate: toISODate(start), endDate: toISODate(end) }
}

async function loadTemplates() {
  loadingTemplates.value = true
  templatesError.value = false
  try {
    templates.value = await challengeApi.getTemplates()
  } catch {
    templatesError.value = true
  } finally {
    loadingTemplates.value = false
  }
}

async function loadMine() {
  if (!isLoggedIn.value) {
    myChallenges.value = []
    return
  }
  try {
    myChallenges.value = await challengeApi.myChallenges()
  } catch {
    myChallenges.value = []
  }
}

async function startChallenge(tpl) {
  if (!isLoggedIn.value) {
    ui.notify("로그인이 필요합니다.", "info")
    return
  }
  if (startingId.value) return
  startingId.value = tpl.templateId
  const { startDate, endDate } = dateRange(tpl.durationDays)
  try {
    await challengeApi.create({
      title: tpl.title,
      description: tpl.description,
      startDate,
      endDate,
      targetCount: tpl.targetCount,
      challengeType: tpl.challengeType,
    })
    ui.success("챌린지가 시작되었습니다.")
    await loadMine()
  } catch {
    ui.error("요청 처리 중 오류가 발생했습니다.")
  } finally {
    startingId.value = null
  }
}

onMounted(async () => {
  await loadTemplates()
  await loadMine()
})
</script>

<template>
  <section class="section container challenge-section">
    <header class="section-head">
      <h2 class="ch-title">어떤 챌린지 할래요?</h2>
      <p class="text-muted">템플릿을 골라 나만의 K-POP 챌린지를 시작해보세요.</p>
    </header>

    <p v-if="loadingTemplates" class="state text-muted">불러오는 중입니다...</p>

    <p v-else-if="templatesError" class="state text-muted">
      데이터를 불러오지 못했습니다.<br />잠시 후 다시 시도해주세요.
    </p>

    <p v-else-if="!templates.length" class="state text-muted">
      아직 준비된 챌린지가 없습니다.
    </p>

    <div v-else class="tpl-grid">
      <article
        v-for="tpl in templates"
        :key="tpl.templateId"
        class="tpl-card card card-pad"
      >
        <span v-if="tpl.badgeText" class="badge badge-neon tpl-badge">
          {{ tpl.badgeText }}
        </span>
        <h3 class="tpl-title">{{ tpl.title }}</h3>
        <p class="tpl-desc text-muted">{{ tpl.description }}</p>

        <dl class="tpl-meta">
          <div><dt>목표</dt><dd>{{ tpl.targetCount }}회</dd></div>
          <div><dt>기간</dt><dd>{{ tpl.durationDays }}일</dd></div>
          <div v-if="tpl.recommendedMood"><dt>추천 감정</dt><dd>{{ tpl.recommendedMood }}</dd></div>
          <div v-if="tpl.recommendedGeneration"><dt>추천 세대</dt><dd>{{ tpl.recommendedGeneration }}</dd></div>
        </dl>

        <BaseButton
          v-if="!isStarted(tpl)"
          size="sm"
          :loading="startingId === tpl.templateId"
          @click="startChallenge(tpl)"
        >
          이 챌린지 시작하기
        </BaseButton>
        <span v-else class="badge done-badge tpl-started">진행 중</span>
      </article>
    </div>

    <!-- 내 챌린지 진행 현황 -->
    <div v-if="isLoggedIn && myChallenges.length" class="mine">
      <h3 class="mine-title">내 챌린지</h3>
      <div class="mine-list">
        <article
          v-for="c in myChallenges"
          :key="c.id"
          class="mine-card card card-pad"
        >
          <div class="mine-top">
            <h4 class="mine-name">{{ c.title }}</h4>
            <span class="mine-count text-muted">{{ c.progressCount }} / {{ c.targetCount }}</span>
          </div>
          <div class="progress-bar">
            <span class="progress-fill" :style="{ width: progressPct(c) + '%' }"></span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.challenge-section {
  padding-top: 16px;
}
.section-head {
  margin-bottom: 22px;
}
.ch-title {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 4px;
}
.state {
  text-align: center;
  padding: 48px 0;
  line-height: 1.7;
}
.tpl-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 760px) {
  .tpl-grid {
    grid-template-columns: 1fr;
  }
}
.tpl-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}
.tpl-badge {
  margin-bottom: 2px;
}
.tpl-title {
  font-size: 17px;
  font-weight: 700;
}
.tpl-desc {
  font-size: 13px;
  line-height: 1.5;
}
.tpl-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;
  margin: 4px 0 8px;
  width: 100%;
}
.tpl-meta > div {
  display: flex;
  gap: 8px;
  font-size: 13px;
}
.tpl-meta dt {
  color: var(--text-muted);
  min-width: 56px;
}
.tpl-meta dd {
  font-weight: 600;
}
.tpl-started {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
}
.done-badge {
  border-color: var(--neon-cyan);
  color: var(--neon-cyan);
}
.mine {
  margin-top: 36px;
}
.mine-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
}
.mine-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
@media (max-width: 760px) {
  .mine-list {
    grid-template-columns: 1fr;
  }
}
.mine-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mine-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.mine-name {
  font-size: 15px;
  font-weight: 700;
}
.mine-count {
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.progress-bar {
  height: 8px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.progress-fill {
  display: block;
  height: 100%;
  background: var(--grad-neon);
  border-radius: var(--radius-pill);
  transition: width 0.3s ease;
}
</style>
