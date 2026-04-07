<script setup>
import { computed, onMounted, shallowRef, watch } from 'vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import { getAdminReviews, removeAdminReview } from '../../services/adminService';
import {
  formatAdminDateTime,
  normalizeAdminReview,
  normalizeArrayPayload,
} from '../../mappers/adminManagementMapper';
import { useFeedback } from '../../composables/useFeedback';
import { resolveAdminActionErrorMessage } from '../../utils/apiErrorMessage';

const reviews = shallowRef([]);
const searchKeyword = shallowRef('');
const statusMessage = shallowRef('');
const loadErrorMessage = shallowRef('');
const isLoading = shallowRef(false);
const isRemoving = shallowRef(false);
const currentPage = shallowRef(1);
const pageSize = 5;
const { requestConfirm } = useFeedback();

const filteredReviews = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return reviews.value;
  }

  return reviews.value.filter((review) => {
    const haystacks = [
      review.productName,
      review.memberName,
      review.content,
    ]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());

    return haystacks.some((value) => value.includes(keyword));
  });
});

const pageCount = computed(() => Math.max(Math.ceil(filteredReviews.value.length / pageSize), 1));
const pagedReviews = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredReviews.value.slice(start, start + pageSize);
});

function applyReviews(items) {
  reviews.value = items
    .map((item) => normalizeAdminReview(item))
    .filter((item) => item.reviewId);
}

async function loadReviews() {
  isLoading.value = true;
  loadErrorMessage.value = '';

  try {
    const payload = await getAdminReviews();
    applyReviews(normalizeArrayPayload(payload, []));
  } catch (error) {
    applyReviews([]);
    loadErrorMessage.value = resolveAdminActionErrorMessage(
      error,
      '리뷰 목록을 불러오지 못했습니다.',
    );
    return false;
  } finally {
    isLoading.value = false;
  }

  return true;
}

async function removeReviewItem(review) {
  if (!review?.reviewId) {
    return;
  }

  const confirmed = await requestConfirm({
    title: '리뷰 삭제',
    message: `"${review.productName}" 리뷰를 삭제할까요?`,
    confirmLabel: '삭제',
  });

  if (!confirmed) {
    return;
  }

  isRemoving.value = true;
  statusMessage.value = '';

  try {
    await removeAdminReview(review.reviewId);
    const didLoadFromServer = await loadReviews();
    statusMessage.value = didLoadFromServer
      ? '리뷰를 삭제했습니다.'
      : '리뷰는 삭제됐지만 목록 재조회는 실패했습니다.';
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '리뷰 삭제에 실패했습니다.');
  } finally {
    isRemoving.value = false;
  }
}

watch(searchKeyword, () => {
  currentPage.value = 1;
});

watch(
  () => filteredReviews.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);

onMounted(loadReviews);
</script>

<template>
  <section class="admin-reviews-manager">
    <AdminPanel title="리뷰 목록" description="작성된 리뷰를 확인하거나 삭제할 수 있습니다.">
      <template #action>
        <input
          v-model="searchKeyword"
          type="text"
          class="admin-reviews-manager__search"
          placeholder="상품명, 작성자, 내용 검색"
        />
      </template>

      <div class="admin-reviews-manager__table">
        <div class="admin-reviews-manager__head">
          <span>상품</span>
          <span>작성자</span>
          <span>내용</span>
          <span>평점</span>
          <span>작성시각</span>
          <span>관리</span>
        </div>

        <article
          v-for="review in pagedReviews"
          :key="review.reviewId"
          class="admin-reviews-manager__row"
        >
          <strong>{{ review.productName }}</strong>
          <span>{{ review.memberName }}</span>
          <p>{{ review.content }}</p>
          <span>{{ review.rating }}점</span>
          <span>{{ formatAdminDateTime(review.createdAt) }}</span>
          <div class="admin-reviews-manager__actions">
            <button type="button" :disabled="isRemoving" @click="removeReviewItem(review)">
              삭제
            </button>
          </div>
        </article>

        <CommonStatePanel
          v-if="!filteredReviews.length"
          :tone="isLoading ? 'loading' : loadErrorMessage ? 'error' : 'neutral'"
          :title="isLoading ? '리뷰 목록을 불러오는 중입니다.' : loadErrorMessage ? '리뷰 목록을 불러오지 못했습니다.' : '표시할 리뷰가 없습니다.'"
          :description="loadErrorMessage"
          compact
        />
      </div>

      <p v-if="statusMessage" class="admin-reviews-manager__status">{{ statusMessage }}</p>
      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-reviews-manager__search {
  width: min(320px, 100%);
  height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  box-sizing: border-box;
}

.admin-reviews-manager__table {
  border-bottom: 1px solid #ededed;
}

.admin-reviews-manager__head,
.admin-reviews-manager__row {
  display: grid;
  grid-template-columns: 180px 120px minmax(0, 1fr) 70px 160px 92px;
  gap: 16px;
  align-items: center;
}

.admin-reviews-manager__head {
  padding: 0 0 14px;
  color: #666666;
  font-size: 13px;
}

.admin-reviews-manager__row {
  padding: 16px 0;
  border-top: 1px solid #efefef;
}

.admin-reviews-manager__row p {
  margin: 0;
  color: #333333;
  font-size: 14px;
  line-height: 1.6;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-reviews-manager__actions {
  display: flex;
  justify-content: flex-end;
}

.admin-reviews-manager__actions button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  cursor: pointer;
}

.admin-reviews-manager__status,
.admin-reviews-manager__empty {
  margin-top: 16px;
  color: #666666;
  font-size: 14px;
  line-height: 1.6;
}

.admin-reviews-manager__status {
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
}

@media (max-width: 1024px) {
  .admin-reviews-manager__head,
  .admin-reviews-manager__row {
    grid-template-columns: 1fr;
  }

  .admin-reviews-manager__head {
    display: none;
  }

  .admin-reviews-manager__row {
    gap: 8px;
    align-items: start;
  }
}

@media (max-width: 720px) {
  .admin-reviews-manager__search {
    width: 100%;
  }
}
</style>
