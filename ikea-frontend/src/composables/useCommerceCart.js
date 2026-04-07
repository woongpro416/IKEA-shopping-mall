import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useFeedback } from './useFeedback';
import { useCartStore } from '../stores/cart';
import { resolveCartActionErrorMessage } from '../utils/apiErrorMessage';

export function useCommerceCart() {
  const cartStore = useCartStore();
  const { showError } = useFeedback();
  cartStore.refreshAvailability();
  void cartStore.ensureCartLoaded().catch(() => {});
  const {
    cartItems,
    recommendations,
    selectedItems,
  } = storeToRefs(cartStore);

  const allSelected = computed({
    get() {
      return cartStore.allSelected;
    },
    set(value) {
      cartStore.setAllSelected(value);
    },
  });

  async function updateQuantity(itemId, delta) {
    try {
      return await cartStore.updateQuantity(itemId, delta);
    } catch (error) {
      showError(resolveCartActionErrorMessage(error, '장바구니 수량을 변경하지 못했습니다.'));
      return null;
    }
  }

  async function removeItem(itemId) {
    try {
      return await cartStore.removeItem(itemId);
    } catch (error) {
      showError(resolveCartActionErrorMessage(error, '장바구니 상품을 삭제하지 못했습니다.'));
      return null;
    }
  }

  async function removeSelected() {
    try {
      return await cartStore.removeSelected();
    } catch (error) {
      showError(resolveCartActionErrorMessage(error, '선택한 상품을 삭제하지 못했습니다.'));
      return null;
    }
  }

  function refreshCart(options = {}) {
    return cartStore.ensureCartLoaded(options);
  }

  return {
    cartItems,
    selectedItems,
    allSelected,
    recommendations,
    refreshCart,
    updateQuantity,
    removeItem,
    removeSelected,
  };
}

export function getCheckoutItemsForFlow(mode = 'all', itemId = '') {
  return useCartStore().resolveCheckoutItems(mode, itemId);
}

export function completeCheckout(payload) {
  return useCartStore().completeCheckout(payload);
}

export function getLatestCompletedOrder() {
  return useCartStore().getLatestCompletedOrder();
}

export function startKakaoCheckout(payload) {
  return useCartStore().startKakaoCheckout(payload);
}

export function startExternalCheckout(payload) {
  return useCartStore().startExternalCheckout(payload);
}

export function confirmPendingKakaoPayment(pgToken) {
  return useCartStore().confirmPendingKakaoPayment(pgToken);
}

export function confirmPendingTossPayment(payload) {
  return useCartStore().confirmPendingTossPayment(payload);
}

export function cancelPendingPaymentFlow() {
  return useCartStore().cancelPendingPaymentFlow();
}

export function getPendingPayment() {
  return useCartStore().getPendingPayment();
}
