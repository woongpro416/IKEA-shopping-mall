import { shallowRef } from 'vue';
import { createMyReview } from '../services/reviewService';
import { useAccountStore } from '../stores/account';
import { useMyPageStore } from '../stores/myPage';
import { resolveReviewErrorMessage } from '../utils/apiErrorMessage';

function createReviewKey(order = {}) {
  return [order.orderId, order.productId].map((value) => String(value ?? '').trim()).join(':');
}

function isDuplicateReviewError(message = '') {
  return /이미.*리뷰|review.*already|duplicate/i.test(String(message ?? ''));
}

export function useMyPageReviewComposer() {
  const accountStore = useAccountStore();
  const myPageStore = useMyPageStore();
  const selectedOrder = shallowRef(null);
  const isDialogOpen = shallowRef(false);
  const isSubmitting = shallowRef(false);
  const statusMessage = shallowRef('');
  const statusTone = shallowRef('neutral');
  const writtenReviewKeys = shallowRef(new Set());

  function markWritten(order) {
    const nextKeys = new Set(writtenReviewKeys.value);
    nextKeys.add(createReviewKey(order));
    writtenReviewKeys.value = nextKeys;
  }

  function clearStatus() {
    statusMessage.value = '';
    statusTone.value = 'neutral';
  }

  function isWritten(order) {
    return writtenReviewKeys.value.has(createReviewKey(order));
  }

  function shouldShowAction(order) {
    return Boolean(accountStore.accessToken) && Boolean(order);
  }

  function isActionDisabled(order) {
    return !accountStore.accessToken || !order?.canWriteReview || isWritten(order);
  }

  function getActionLabel(order) {
    if (isWritten(order)) {
      return '작성 완료';
    }

    if (!order?.canWriteReview) {
      return '배송 완료 후 작성';
    }

    return '리뷰 작성';
  }

  function openDialog(order) {
    if (!order || !accountStore.accessToken || !order.canWriteReview || isWritten(order)) {
      return;
    }

    selectedOrder.value = order;
    isDialogOpen.value = true;
    clearStatus();
  }

  function closeDialog(options = {}) {
    const { force = false } = options;

    if (isSubmitting.value && !force) {
      return;
    }

    isDialogOpen.value = false;
    selectedOrder.value = null;
  }

  async function submitReview(payload = {}) {
    if (!selectedOrder.value || isSubmitting.value) {
      return false;
    }

    isSubmitting.value = true;
    clearStatus();

    try {
      await createMyReview({
        orderId: Number(selectedOrder.value.orderId),
        productId: Number(selectedOrder.value.productId),
        content: String(payload.content ?? '').trim(),
        rating: Number(payload.rating ?? 0),
      });

      markWritten(selectedOrder.value);
      statusMessage.value = '리뷰를 등록했습니다.';
      statusTone.value = 'success';
      closeDialog({ force: true });
      await myPageStore.loadProfile();
      return true;
    } catch (error) {
      if (selectedOrder.value && isDuplicateReviewError(error?.message)) {
        markWritten(selectedOrder.value);
      }

      statusMessage.value = resolveReviewErrorMessage(error, '리뷰 등록에 실패했습니다.');
      statusTone.value = 'error';
      return false;
    } finally {
      isSubmitting.value = false;
    }
  }

  return {
    clearStatus,
    closeDialog,
    getActionLabel,
    isActionDisabled,
    isDialogOpen,
    isSubmitting,
    openDialog,
    selectedOrder,
    shouldShowAction,
    statusMessage,
    statusTone,
    submitReview,
  };
}
