import { computed, shallowRef, watch } from 'vue';
import { getProductReviews } from '../services/reviewService';
import { resolveReviewLookupErrorMessage } from '../utils/apiErrorMessage';

function normalizeText(value) {
  return String(value ?? '').trim();
}

function resolveReviewAuthor(item = {}) {
  return normalizeText(item?.loginId ?? item?.memberName ?? item?.writer);
}

function normalizeReviewCollection(payload) {
  const source = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.data)
      ? payload.data
      : Array.isArray(payload?.content)
        ? payload.content
        : Array.isArray(payload?.items)
          ? payload.items
          : [];

  return source.map((item) => {
    const rating = Number(item?.rating ?? 0);
    const createdAt = normalizeText(item?.createdAt);
    const author = resolveReviewAuthor(item);
    const date = createdAt ? new Date(createdAt) : null;
    const createdLabel = date && !Number.isNaN(date.getTime())
      ? [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
      ].join('.')
      : '';

    return {
      id: String(item?.reviewId ?? item?.id ?? `${author}-${createdAt}`),
      author: author || '익명',
      content: normalizeText(item?.content) || '등록된 리뷰 내용을 확인할 수 없습니다.',
      rating,
      ratingLabel: rating > 0 ? rating.toFixed(1) : '',
      meta: createdLabel ? `${createdLabel} 작성` : '',
    };
  });
}

export function useProductReviews(productId) {
  const reviewItems = shallowRef([]);
  const isLoadingReviews = shallowRef(false);
  const reviewLoadErrorMessage = shallowRef('');
  const hasLoadedReviews = shallowRef(false);
  let latestRequestToken = 0;

  const reviewCount = computed(() => reviewItems.value.length);
  const averageRating = computed(() => {
    if (!reviewItems.value.length) {
      return 0;
    }

    const totalRating = reviewItems.value.reduce((sum, review) => sum + Number(review.rating ?? 0), 0);
    return totalRating / reviewItems.value.length;
  });

  async function loadProductReviews() {
    const normalizedProductId = String(productId?.value ?? productId ?? '').trim();

    if (!normalizedProductId) {
      reviewItems.value = [];
      reviewLoadErrorMessage.value = '';
      hasLoadedReviews.value = false;
      isLoadingReviews.value = false;
      return;
    }

    const requestToken = ++latestRequestToken;
    const previousReviews = Array.isArray(reviewItems.value) ? [...reviewItems.value] : [];
    isLoadingReviews.value = true;
    reviewLoadErrorMessage.value = '';

    try {
      const payload = await getProductReviews(normalizedProductId);

      if (requestToken !== latestRequestToken) {
        return;
      }

      reviewItems.value = normalizeReviewCollection(payload);
      hasLoadedReviews.value = true;
    } catch (error) {
      if (requestToken !== latestRequestToken) {
        return;
      }

      reviewItems.value = previousReviews;
      reviewLoadErrorMessage.value = resolveReviewLookupErrorMessage(
        error,
        '상품 리뷰를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
      );
    } finally {
      if (requestToken === latestRequestToken) {
        isLoadingReviews.value = false;
      }
    }
  }

  watch(
    () => String(productId?.value ?? productId ?? '').trim(),
    (nextProductId, previousProductId) => {
      if (nextProductId !== previousProductId) {
        reviewItems.value = [];
        reviewLoadErrorMessage.value = '';
        hasLoadedReviews.value = false;
      }

      if (!nextProductId) {
        reviewItems.value = [];
        reviewLoadErrorMessage.value = '';
        hasLoadedReviews.value = false;
        isLoadingReviews.value = false;
        return;
      }

      void loadProductReviews();
    },
    { immediate: true },
  );

  return {
    averageRating,
    hasLoadedReviews,
    isLoadingReviews,
    loadProductReviews,
    reviewCount,
    reviewItems,
    reviewLoadErrorMessage,
  };
}
