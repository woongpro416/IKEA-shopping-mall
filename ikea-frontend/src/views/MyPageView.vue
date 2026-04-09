<script setup>
import { computed, ref } from 'vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import MyPageActivitySection from '../components/my-page/MyPageActivitySection.vue';
import MyPageOrdersSection from '../components/my-page/MyPageOrdersSection.vue';
import MyPageOverviewSection from '../components/my-page/MyPageOverviewSection.vue';
import MyPageReviewDialog from '../components/my-page/MyPageReviewDialog.vue';
import MyPageSidebar from '../components/my-page/MyPageSidebar.vue';
import MyPageSupportSection from '../components/my-page/MyPageSupportSection.vue';
import { useMemberWithdrawal } from '../composables/useMemberWithdrawal';
import { useMyPageOrderActions } from '../composables/useMyPageOrderActions';
import { useMyPage } from '../composables/useMyPage';
import { useMyPageReviewComposer } from '../composables/useMyPageReviewComposer';

const {
  accountHighlights,
  buildProductDetailPath,
  isProfileLoading,
  orderSteps,
  profile,
  profileError,
  quickLinks,
  recentOrders,
  recentViewItems,
  sectionLinks,
  summaryCards,
  supportCards,
  wishListItems,
} = useMyPage();

const activeSectionId = ref(sectionLinks[0]?.id ?? 'overview');

const sectionMetaMap = {
  overview: {
    title: '회원 요약',
  },
  orders: {
    title: '주문 관리',
  },
  activity: {
    title: '관심 활동',
  },
  support: {
    title: '고객 지원',
  },
};

const activeSectionMeta = computed(
  () => sectionMetaMap[activeSectionId.value] ?? sectionMetaMap.overview,
);
const {
  canWithdraw,
  isSubmitting: isWithdrawalSubmitting,
  submitWithdrawal,
  withdrawalGuideItems,
  withdrawalHintMessage,
  withdrawalHintTone,
} = useMemberWithdrawal();

const {
  clearStatus: clearReviewStatus,
  closeDialog: closeReviewDialog,
  getActionLabel: getReviewActionLabel,
  isActionDisabled: isReviewActionDisabled,
  isDialogOpen: isReviewDialogOpen,
  isSubmitting: isReviewSubmitting,
  openDialog: openReviewDialog,
  selectedOrder: selectedReviewOrder,
  shouldShowAction: shouldShowReviewAction,
  statusMessage: reviewStatusMessage,
  statusTone: reviewStatusTone,
  submitReview,
} = useMyPageReviewComposer();
const {
  clearStatus: clearOrderActionStatus,
  getActionLabel: getOrderActionLabel,
  isActionPending: isOrderActionPending,
  requestAction: requestOrderAction,
  shouldShowAction: shouldShowOrderAction,
  statusMessage: orderActionStatusMessage,
  statusTone: orderActionStatusTone,
} = useMyPageOrderActions();

const statusMessage = computed(() => {
  if (isProfileLoading.value) {
    return '회원 정보를 불러오는 중입니다.';
  }

  return '';
});

const profileStateTone = computed(() => (isProfileLoading.value ? 'loading' : 'error'));
const profileStateTitle = computed(() => (
  isProfileLoading.value
    ? '회원 정보를 불러오는 중입니다.'
    : '회원 정보를 확인할 수 없습니다.'
));
const profileStateDescription = computed(() => (
  isProfileLoading.value
    ? '저장된 회원 정보와 최근 주문 상태를 확인하고 있습니다.'
    : statusMessage.value
));
const orderBoardStatusMessage = computed(() => (
  orderActionStatusMessage.value || reviewStatusMessage.value
));
const orderBoardStatusTone = computed(() => (
  orderActionStatusMessage.value ? orderActionStatusTone.value : reviewStatusTone.value
));

function moveToSection(sectionId) {
  activeSectionId.value = sectionId;
  window.scrollTo({
    top: 0,
    left: 0,
    behavior: 'auto',
  });
}

function handleOpenReview(order) {
  clearOrderActionStatus();
  openReviewDialog(order);
}

async function handleRequestOrderAction(order) {
  clearReviewStatus();
  await requestOrderAction(order);
}
</script>

<template>
  <SiteChrome>
    <main class="my-page">
      <div class="my-page__inner">
        <div class="my-breadcrumb">
          <RouterLink to="/" class="my-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>〉</span>
          <span>마이페이지</span>
        </div>

        <section class="my-layout">
          <MyPageSidebar
            :section-links="sectionLinks"
            :active-section-id="activeSectionId"
            @select-section="moveToSection"
          />

          <section class="my-content">
            <header class="my-content__header">
              <h2>{{ activeSectionMeta.title }}</h2>
            </header>

            <MyPageOverviewSection
              v-if="activeSectionId === 'overview'"
              :profile="profile"
              :status-message="statusMessage"
              :profile-state-tone="profileStateTone"
              :profile-state-title="profileStateTitle"
              :profile-state-description="profileStateDescription"
              :summary-cards="summaryCards"
              :quick-links="quickLinks"
            />

            <MyPageOrdersSection
              v-else-if="activeSectionId === 'orders'"
              :order-steps="orderSteps"
              :recent-orders="recentOrders"
              :review-status-message="orderBoardStatusMessage"
              :review-status-tone="orderBoardStatusTone"
              :build-product-detail-path="buildProductDetailPath"
              :should-show-order-action="shouldShowOrderAction"
              :is-order-action-pending="isOrderActionPending"
              :get-order-action-label="getOrderActionLabel"
              :should-show-review-action="shouldShowReviewAction"
              :is-review-action-disabled="isReviewActionDisabled"
              :get-review-action-label="getReviewActionLabel"
              @open-review="handleOpenReview"
              @request-order-action="handleRequestOrderAction"
            />

            <MyPageActivitySection
              v-else-if="activeSectionId === 'activity'"
              :wish-list-items="wishListItems"
              :recent-view-items="recentViewItems"
              :build-product-detail-path="buildProductDetailPath"
            />

            <MyPageSupportSection
              v-else
              :account-highlights="accountHighlights"
              :profile="profile"
              :support-cards="supportCards"
              :withdrawal-guide-items="withdrawalGuideItems"
              :withdrawal-hint-message="withdrawalHintMessage"
              :withdrawal-hint-tone="withdrawalHintTone"
              :can-withdraw="canWithdraw"
              :is-withdrawal-submitting="isWithdrawalSubmitting"
              @withdraw="submitWithdrawal"
            />
          </section>
        </section>
      </div>
    </main>

    <MyPageReviewDialog
      :is-open="isReviewDialogOpen"
      :is-submitting="isReviewSubmitting"
      :order="selectedReviewOrder"
      :status-message="reviewStatusMessage"
      :status-tone="reviewStatusTone"
      @close="closeReviewDialog"
      @submit="submitReview"
    />
  </SiteChrome>
</template>

<style scoped>
.my-page {
  background: #ffffff;
}

.my-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 28px 0 96px;
}

.my-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.my-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.my-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.my-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 64px;
  margin-top: 40px;
}

.my-content {
  min-width: 0;
}

.my-content__header {
  padding-bottom: 18px;
  border-bottom: 2px solid #111111;
}

.my-content__header h2 {
  margin: 0;
  color: #111111;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .my-layout {
    grid-template-columns: 1fr;
    gap: 28px;
  }
}

@media (max-width: 720px) {
  .my-page__inner {
    width: calc(100% - 28px);
    padding: 24px 0 72px;
  }

  .my-content__header h2 {
    font-size: 28px;
  }
}
</style>
