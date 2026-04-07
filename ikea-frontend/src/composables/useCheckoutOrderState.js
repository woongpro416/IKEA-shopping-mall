import { computed, shallowRef, watch } from 'vue';

const PAYMENT_METHODS = [
  { id: 'kakaopay', label: '카카오페이' },
  { id: 'tosspay', label: '토스페이' },
  { id: 'bank', label: '무통장입금' },
];

export function useCheckoutOrderState(route, getCheckoutItemsForFlow) {
  const orderItems = shallowRef([]);
  const pointAmount = shallowRef('0');
  const paymentMethod = shallowRef('kakaopay');

  function syncOrderItems() {
    orderItems.value = getCheckoutItemsForFlow(
      String(route.query.mode ?? 'all'),
      String(route.query.itemId ?? ''),
    );
  }

  watch(
    () => [route.query.mode, route.query.itemId],
    () => {
      syncOrderItems();
      window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    },
    { immediate: true },
  );

  watch(pointAmount, (value) => {
    const normalized = String(value ?? '').replace(/[^\d]/g, '');
    pointAmount.value = normalized === '' ? '0' : String(Number(normalized));
  });

  const orderCount = computed(() =>
    orderItems.value.reduce((sum, item) => sum + item.quantity, 0));
  const productTotal = computed(() =>
    orderItems.value.reduce(
      (sum, item) => sum + ((item.originalPrice ?? item.price) * item.quantity),
      0,
    ));
  const catalogDiscountTotal = computed(() =>
    orderItems.value.reduce(
      (sum, item) => sum + (((item.originalPrice ?? item.price) - item.price) * item.quantity),
      0,
    ));
  const couponDiscount = computed(() => 0);
  const shippingTotal = computed(() => 0);
  const pointApplied = computed(() =>
    Math.min(
      Number(pointAmount.value || '0'),
      Math.max(0, productTotal.value - catalogDiscountTotal.value),
    ));
  const finalTotal = computed(() =>
    Math.max(
      0,
      productTotal.value
        - catalogDiscountTotal.value
        - couponDiscount.value
        - pointApplied.value
        + shippingTotal.value,
    ));
  const selectedPaymentMethod = computed(
    () => PAYMENT_METHODS.find((method) => method.id === paymentMethod.value) ?? PAYMENT_METHODS[0],
  );
  const paymentMethodNotice = computed(() =>
    paymentMethod.value === 'bank'
      ? '무통장입금 선택 시 주문완료 페이지에서 가상계좌와 입금기한을 확인할 수 있습니다.'
      : '선택한 결제수단으로 주문을 완료하면 결제 완료 화면으로 이동합니다.',
  );

  return {
    catalogDiscountTotal,
    couponDiscount,
    finalTotal,
    orderCount,
    orderItems,
    paymentMethod,
    paymentMethodNotice,
    paymentMethods: PAYMENT_METHODS,
    pointAmount,
    pointApplied,
    productTotal,
    selectedPaymentMethod,
    shippingTotal,
    syncOrderItems,
  };
}
