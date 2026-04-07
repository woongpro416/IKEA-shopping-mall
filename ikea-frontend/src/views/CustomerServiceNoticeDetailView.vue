<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import CommonStatePanel from '../components/common/CommonStatePanel.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import {
  buildCustomerServiceNoticeDetailPath,
  ROUTE_PATHS,
} from '../constants/routes';
import {
  getCustomerNoticeDetail,
  getCustomerNoticeRows,
} from '../services/noticeService';

const route = useRoute();

const sidebarLinks = [
  { id: 'faq', label: 'FAQ', to: ROUTE_PATHS.customerServiceFaq },
  { id: 'qna', label: 'QnA', to: ROUTE_PATHS.customerServiceQna },
  { id: 'notice', label: '공지사항', to: ROUTE_PATHS.customerServiceNotice },
];

const noticeRows = ref([]);
const noticeDetail = ref(null);
const isNoticeLoading = ref(false);
const noticeLoadError = ref('');

const fallbackNotice = computed(() => (
  noticeRows.value.find((row) => String(row.id) === String(route.params.noticeId ?? '')) ?? null
));
const currentNotice = computed(() => noticeDetail.value ?? fallbackNotice.value);
const noticeLines = computed(() => {
  if (Array.isArray(currentNotice.value?.lines) && currentNotice.value.lines.length) {
    return currentNotice.value.lines;
  }

  const content = String(currentNotice.value?.content ?? '').trim();
  return content ? [content] : [];
});
const currentNoticeIndex = computed(() => noticeRows.value.findIndex(
  (row) => String(row.id) === String(currentNotice.value?.id ?? route.params.noticeId ?? ''),
));
const previousNotice = computed(() => (
  currentNoticeIndex.value > 0 ? noticeRows.value[currentNoticeIndex.value - 1] : null
));
const nextNotice = computed(() => (
  currentNoticeIndex.value >= 0 && currentNoticeIndex.value < noticeRows.value.length - 1
    ? noticeRows.value[currentNoticeIndex.value + 1]
    : null
));
const shouldShowMissingState = computed(() => (
  !isNoticeLoading.value && !currentNotice.value && !noticeLoadError.value
));

async function loadNoticeDetailData() {
  const noticeId = String(route.params.noticeId ?? '').trim();

  if (!noticeId) {
    noticeDetail.value = null;
    noticeLoadError.value = '공지사항을 찾을 수 없습니다.';
    return;
  }

  isNoticeLoading.value = true;
  noticeLoadError.value = '';

  const [detailResult, listResult] = await Promise.allSettled([
    getCustomerNoticeDetail(noticeId),
    getCustomerNoticeRows(),
  ]);

  if (listResult.status === 'fulfilled') {
    noticeRows.value = listResult.value;
  }

  if (detailResult.status === 'fulfilled') {
    noticeDetail.value = detailResult.value;
  } else {
    noticeDetail.value = null;
    noticeLoadError.value = detailResult.reason?.message ?? '공지사항을 불러오지 못했습니다.';
  }

  isNoticeLoading.value = false;
}

onMounted(() => {
  void loadNoticeDetailData();
});

watch(
  () => route.params.noticeId,
  () => {
    void loadNoticeDetailData();
  },
);
</script>

<template>
  <SiteChrome>
    <main class="cs-page">
      <div class="cs-page__inner">
        <div class="cs-breadcrumb">
          <RouterLink to="/" class="cs-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>></span>
          <span>고객센터</span>
          <span>></span>
          <span>공지사항</span>
        </div>

        <section class="cs-layout">
          <aside class="cs-sidebar">
            <h1>고객센터</h1>
            <nav class="cs-sidebar__nav">
              <RouterLink
                v-for="link in sidebarLinks"
                :key="link.id"
                :to="link.to"
                class="cs-sidebar__link"
                :class="{ 'is-active': link.id === 'notice' }"
              >
                {{ link.label }}
              </RouterLink>
            </nav>
          </aside>

          <section class="cs-content">
            <header class="cs-content__header">
              <h2>공지사항</h2>
            </header>

            <CommonStatePanel
              v-if="noticeLoadError"
              tone="error"
              title="최신 공지 데이터를 확인할 수 없습니다."
              :description="noticeLoadError"
              align="left"
              compact
            />

            <CommonStatePanel
              v-else-if="shouldShowMissingState"
              tone="muted"
              title="공지사항을 찾을 수 없습니다."
              description="목록으로 돌아가 다른 공지를 확인해 주세요."
              align="left"
              compact
            />

            <article v-else-if="currentNotice" class="notice-detail">
              <div class="notice-detail__head">
                <strong>{{ currentNotice.title }}</strong>
                <span>{{ currentNotice.date }}</span>
              </div>

              <div class="notice-detail__body">
                <template v-if="noticeLines.length">
                  <p v-for="line in noticeLines" :key="line">{{ line }}</p>
                </template>
                <p v-else class="notice-detail__empty">
                  본문 내용은 목록에서 다시 확인해 주세요.
                </p>
              </div>

              <div class="notice-detail__nav">
                <div class="notice-detail__nav-row">
                  <span>이전글</span>
                  <RouterLink v-if="previousNotice" :to="buildCustomerServiceNoticeDetailPath(previousNotice.id)">
                    {{ previousNotice.title }}
                  </RouterLink>
                  <b v-else>이전글이 없습니다.</b>
                </div>
                <div class="notice-detail__nav-row">
                  <span>다음글</span>
                  <RouterLink v-if="nextNotice" :to="buildCustomerServiceNoticeDetailPath(nextNotice.id)">
                    {{ nextNotice.title }}
                  </RouterLink>
                  <b v-else>다음글이 없습니다.</b>
                </div>
              </div>

              <div class="notice-detail__actions">
                <RouterLink :to="ROUTE_PATHS.customerServiceNotice" class="notice-detail__list-button">
                  목록
                </RouterLink>
              </div>
            </article>
          </section>
        </section>
      </div>
    </main>
  </SiteChrome>
</template>

<style scoped>
.cs-page {
  background: #ffffff;
}

.cs-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 28px 0 96px;
}

.cs-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.cs-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.cs-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.cs-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 64px;
  margin-top: 40px;
}

.cs-sidebar h1 {
  margin: 0 0 28px;
  font-size: 36px;
  line-height: 1.2;
  font-weight: 700;
  color: #111111;
}

.cs-sidebar__nav {
  display: grid;
  border-top: 2px solid #111111;
}

.cs-sidebar__link {
  display: flex;
  align-items: center;
  min-height: 56px;
  border-bottom: 1px solid #e6e6e6;
  color: #222222;
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
}

.cs-sidebar__link.is-active {
  color: #111111;
  font-weight: 700;
}

.cs-content {
  min-width: 0;
}

.cs-content__header {
  padding-bottom: 18px;
  border-bottom: 2px solid #111111;
}

.cs-content__header h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
  color: #111111;
}

.notice-detail {
  padding-top: 24px;
}

.notice-detail__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  min-height: 74px;
  padding: 0 16px;
  border-top: 1px solid #111111;
  border-bottom: 1px solid #e6e6e6;
}

.notice-detail__head strong {
  font-size: 22px;
  line-height: 1.45;
  font-weight: 600;
  color: #111111;
}

.notice-detail__head span {
  flex: 0 0 auto;
  color: #666666;
  font-size: 14px;
}

.notice-detail__body {
  min-height: 260px;
  padding: 32px 18px 40px;
  border-bottom: 1px solid #e6e6e6;
}

.notice-detail__body p {
  margin: 0 0 16px;
  color: #333333;
  font-size: 15px;
  line-height: 1.95;
  white-space: pre-wrap;
}

.notice-detail__body p:last-child {
  margin-bottom: 0;
}

.notice-detail__empty {
  color: #666666;
}

.notice-detail__nav {
  margin-top: 24px;
  border-top: 1px solid #111111;
}

.notice-detail__nav-row {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  align-items: center;
  min-height: 54px;
  border-bottom: 1px solid #e6e6e6;
}

.notice-detail__nav-row span {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #555555;
  font-size: 14px;
  font-weight: 600;
}

.notice-detail__nav-row a,
.notice-detail__nav-row b {
  padding: 0 16px;
  color: #222222;
  text-decoration: none;
  font-size: 14px;
  font-weight: 400;
}

.notice-detail__actions {
  display: flex;
  justify-content: center;
  padding-top: 32px;
}

.notice-detail__list-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 140px;
  height: 48px;
  border: 1px solid #111111;
  color: #111111;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 960px) {
  .cs-layout {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .cs-sidebar h1 {
    font-size: 28px;
  }

  .cs-content__header h2 {
    font-size: 26px;
  }
}

@media (max-width: 720px) {
  .cs-page__inner {
    width: calc(100% - 28px);
    padding: 24px 0 72px;
  }

  .notice-detail__head {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px 16px;
  }

  .notice-detail__head strong {
    font-size: 18px;
  }

  .notice-detail__nav-row {
    grid-template-columns: 84px minmax(0, 1fr);
  }
}
</style>
