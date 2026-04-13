<template>
  <div class="workspace-page admin-command-center">
    <section class="admin-hero" :class="`admin-hero--${viewModel.hero.statusTone}`">
      <div class="admin-hero__copy">
        <p class="admin-hero__eyebrow">{{ viewModel.hero.eyebrow }}</p>
        <h1>{{ viewModel.hero.title }}</h1>
        <p class="admin-hero__description">{{ viewModel.hero.description }}</p>

        <div class="admin-hero__inline-stats">
          <article
            v-for="item in viewModel.hero.inlineStats"
            :key="item.key"
            class="admin-inline-stat"
          >
            <span class="admin-inline-stat__label">{{ item.label }}</span>
            <strong class="admin-inline-stat__value">{{ item.value }}</strong>
          </article>
        </div>
      </div>

      <div class="admin-hero__status">
        <div class="admin-hero__status-top">
          <span class="admin-hero__status-label">当前状态</span>
          <span class="admin-hero__status-chip">{{ viewModel.hero.statusChip }}</span>
        </div>

        <div class="admin-hero__status-main">
          <div class="admin-hero__signal">
            <span />
            <span />
            <span />
          </div>
          <div>
            <div class="admin-hero__status-value">{{ viewModel.hero.statusLabel }}</div>
            <div class="admin-hero__status-meta">{{ viewModel.hero.statusMeta }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="admin-primary-grid">
      <article
        v-for="card in viewModel.primaryCards"
        :key="card.key"
        class="admin-primary-card"
        :class="`admin-primary-card--${card.key}`"
      >
        <template v-if="card.key === 'feedback'">
          <div class="admin-primary-card__top">
            <div>
              <p class="admin-primary-card__label">{{ card.title }}</p>
              <h2>{{ card.subtitle }}</h2>
            </div>

            <div class="admin-feedback-ring" :style="{ '--feedback-percent': `${card.ringPercent}%` }">
              <div class="admin-feedback-ring__inner">{{ card.ringPercent }}%</div>
            </div>
          </div>

          <div class="admin-feedback-main">
            <strong class="admin-feedback-main__value">{{ card.value }}</strong>
            <span class="admin-feedback-main__suffix">{{ card.valueSuffix }}</span>
          </div>

          <div class="admin-feedback-highlights">
            <article
              v-for="item in card.highlights"
              :key="item.key"
              class="admin-feedback-highlight"
              :class="`admin-feedback-highlight--${item.tone}`"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <p class="admin-primary-card__footnote">{{ card.footnote }}</p>
        </template>

        <template v-else>
          <div class="admin-primary-card__top">
            <div>
              <p class="admin-primary-card__label">{{ card.title }}</p>
              <h2>{{ card.subtitle }}</h2>
            </div>
          </div>

          <div class="admin-quick-actions">
            <button
              v-for="action in card.actions"
              :key="action.key"
              type="button"
              class="admin-quick-action"
              :class="`admin-quick-action--${action.tone}`"
              @click="go(action.route)"
            >
              <span class="admin-quick-action__dot" />
              <div class="admin-quick-action__copy">
                <strong>{{ action.label }}</strong>
                <span>{{ action.meta }}</span>
              </div>
            </button>
          </div>
        </template>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { buildAdminCommandCenterViewModel } from './adminCommandCenterHelpers'

const router = useRouter()
const stats = ref({
  userCount: 0,
  studentCount: 0,
  teacherCount: 0,
  adminCount: 0,
  noticeCount: 0,
  status: '运行正常',
  weeklyVisits: []
})
const feedbackSummary = ref({ total: 0, resolved: 0, pending: 0 })

const go = (path) => router.push(path)

const fetchAdminStats = async () => {
  const data = await request.get('/admin/stats')
  stats.value = { ...stats.value, ...data }
}

const fetchFeedbackSummary = async () => {
  const page = await request.get('/feedback/list', { params: { page: 1, size: 200 } })
  const records = Array.isArray(page?.records) ? page.records : []
  const resolved = records.filter((item) => String(item.status || '').toUpperCase() === 'RESOLVED').length
  feedbackSummary.value = { total: records.length, resolved, pending: records.length - resolved }
}

const viewModel = computed(() => buildAdminCommandCenterViewModel(stats.value, feedbackSummary.value))

onMounted(async () => {
  await Promise.all([fetchAdminStats(), fetchFeedbackSummary()])
})
</script>

<style scoped>
.admin-command-center {
  --admin-ink: #182234;
  --admin-muted: #718095;
  --admin-surface: rgba(255, 255, 255, 0.88);
  --admin-line: rgba(24, 34, 52, 0.08);
  --admin-shadow: 0 26px 70px rgba(119, 136, 162, 0.13);
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.admin-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 380px);
  gap: 18px;
  min-height: 196px;
  padding: 24px 26px;
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.76);
  box-shadow: var(--admin-shadow);
  background:
    radial-gradient(circle at 10% 24%, rgba(144, 183, 255, 0.38), transparent 30%),
    linear-gradient(135deg, rgba(245, 250, 255, 0.98), rgba(255, 255, 255, 0.92));
}

.admin-hero::after {
  content: '';
  position: absolute;
  right: -90px;
  bottom: -132px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: rgba(203, 238, 221, 0.36);
  filter: blur(24px);
}

.admin-hero--warning {
  background:
    radial-gradient(circle at 10% 24%, rgba(255, 210, 138, 0.34), transparent 30%),
    linear-gradient(135deg, rgba(255, 249, 239, 0.98), rgba(255, 255, 255, 0.92));
}

.admin-hero__copy,
.admin-hero__status {
  position: relative;
  z-index: 1;
}

.admin-hero__eyebrow {
  margin: 0 0 10px;
  color: #6d7f99;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.22em;
}

.admin-hero h1 {
  margin: 0;
  color: var(--admin-ink);
  font-size: clamp(30px, 3.3vw, 46px);
  line-height: 1;
}

.admin-hero__description {
  max-width: 620px;
  margin: 12px 0 0;
  color: var(--admin-muted);
  font-size: 15px;
  line-height: 1.65;
}

.admin-hero__inline-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.admin-inline-stat {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.64);
}

.admin-inline-stat__label {
  display: block;
  color: #7b8898;
  font-size: 12px;
  font-weight: 700;
}

.admin-inline-stat__value {
  display: block;
  margin-top: 6px;
  color: var(--admin-ink);
  font-size: 24px;
  line-height: 1;
}

.admin-hero__status {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(22, 33, 49, 0.96), rgba(31, 42, 59, 0.94));
  color: #f6fbff;
  box-shadow: 0 26px 54px rgba(16, 24, 39, 0.24);
}

.admin-hero--warning .admin-hero__status {
  background: linear-gradient(180deg, rgba(77, 48, 11, 0.96), rgba(95, 62, 14, 0.94));
}

.admin-hero__status-top,
.admin-hero__status-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.admin-hero__status-label {
  color: rgba(241, 247, 255, 0.74);
  font-size: 12px;
  font-weight: 700;
}

.admin-hero__status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.84);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.admin-hero__signal {
  display: grid;
  grid-template-columns: repeat(3, 12px);
  align-items: end;
  gap: 6px;
  min-width: 48px;
}

.admin-hero__signal span {
  display: block;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(171, 222, 205, 1), rgba(113, 196, 160, 0.64));
}

.admin-hero__signal span:nth-child(1) { height: 22px; }
.admin-hero__signal span:nth-child(2) { height: 30px; }
.admin-hero__signal span:nth-child(3) { height: 38px; }

.admin-hero--warning .admin-hero__signal span {
  background: linear-gradient(180deg, rgba(255, 220, 155, 1), rgba(244, 162, 69, 0.7));
}

.admin-hero__status-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.admin-hero__status-meta {
  margin-top: 8px;
  color: rgba(240, 247, 255, 0.78);
  font-size: 13px;
  line-height: 1.6;
}

.admin-primary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  min-height: 0;
  flex: 1;
}

.admin-primary-card {
  min-height: 280px;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: var(--admin-surface);
  box-shadow: 0 22px 48px rgba(117, 133, 160, 0.1);
}

.admin-primary-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.admin-primary-card__label {
  margin: 0;
  color: #7b8898;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.admin-primary-card h2 {
  margin: 10px 0 0;
  color: var(--admin-ink);
  font-size: 28px;
  line-height: 1.15;
}

.admin-feedback-ring {
  --feedback-percent: 0%;
  position: relative;
  width: 94px;
  height: 94px;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.92) 0 58%, transparent 59%),
    conic-gradient(
      from -90deg,
      #80d9b2 0 var(--feedback-percent),
      rgba(255, 204, 110, 0.24) var(--feedback-percent) 100%
    );
}

.admin-feedback-ring__inner {
  position: absolute;
  inset: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: var(--admin-ink);
  font-size: 18px;
  font-weight: 800;
}

.admin-feedback-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 22px;
}

.admin-feedback-main__value {
  color: var(--admin-ink);
  font-size: 72px;
  line-height: 0.9;
}

.admin-feedback-main__suffix {
  color: #7b8898;
  font-size: 18px;
  font-weight: 700;
}

.admin-feedback-highlights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.admin-feedback-highlight {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 18px;
}

.admin-feedback-highlight--mint {
  background: linear-gradient(135deg, rgba(229, 247, 238, 0.96), rgba(243, 252, 247, 0.96));
}

.admin-feedback-highlight--amber {
  background: linear-gradient(135deg, rgba(255, 244, 214, 0.96), rgba(255, 250, 237, 0.96));
}

.admin-feedback-highlight span {
  color: #738094;
  font-size: 13px;
  font-weight: 700;
}

.admin-feedback-highlight strong {
  color: var(--admin-ink);
  font-size: 22px;
}

.admin-primary-card__footnote {
  margin: 18px 0 0;
  color: var(--admin-muted);
  font-size: 13px;
}

.admin-quick-actions {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.admin-quick-action {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid var(--admin-line);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.admin-quick-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 30px rgba(120, 134, 160, 0.14);
  border-color: rgba(74, 99, 140, 0.14);
}

.admin-quick-action--ice {
  background: linear-gradient(135deg, rgba(245, 249, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.admin-quick-action--amber {
  background: linear-gradient(135deg, rgba(255, 249, 237, 0.98), rgba(255, 255, 255, 0.94));
}

.admin-quick-action--violet {
  background: linear-gradient(135deg, rgba(248, 245, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.admin-quick-action__dot {
  flex: 0 0 12px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #8fb4ff;
  box-shadow: 0 0 0 6px rgba(143, 180, 255, 0.14);
}

.admin-quick-action--amber .admin-quick-action__dot {
  background: #f0b438;
  box-shadow: 0 0 0 6px rgba(240, 180, 56, 0.14);
}

.admin-quick-action--violet .admin-quick-action__dot {
  background: #b79dff;
  box-shadow: 0 0 0 6px rgba(183, 157, 255, 0.14);
}

.admin-quick-action__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-quick-action__copy strong {
  color: var(--admin-ink);
  font-size: 20px;
}

.admin-quick-action__copy span {
  color: var(--admin-muted);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .admin-command-center {
    overflow: auto;
  }

  .admin-hero,
  .admin-primary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-hero,
  .admin-primary-card {
    border-radius: 24px;
  }

  .admin-hero {
    padding: 22px;
  }

  .admin-hero__inline-stats,
  .admin-feedback-highlights {
    grid-template-columns: 1fr;
  }
}
</style>
