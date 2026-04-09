import { ORDER_STATUS_LABELS } from '../constants/orderStatus';

export function cloneCartItems(items = []) {
  return items.map((item) => ({ ...item }));
}

function formatDateParts(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return {
    compact: `${year}${month}${day}${hours}${minutes}`,
    label: `${year}.${month}.${day} ${hours}:${minutes}`,
  };
}

function buildOrderNumber(date = new Date()) {
  const { compact } = formatDateParts(date);
  const suffix = String(Math.floor(date.getTime() % 1000000)).padStart(6, '0');

  return `HM${compact}${suffix}`;
}

export function getCheckoutItems(items = [], mode = 'all', itemId = '') {
  const purchasableItems = items.filter((item) => !item.isSoldOut);
  const normalizedItemId = String(itemId ?? '');

  if (mode === 'single') {
    const singleItem = purchasableItems.find((item) => (
      item.productId === normalizedItemId
      || String(item.backendProductId ?? '') === normalizedItemId
    )) ?? null;

    return singleItem ? [singleItem] : [];
  }

  if (mode === 'selected') {
    const selectedItems = purchasableItems.filter((item) => item.selected);
    return selectedItems.length ? selectedItems : [];
  }

  return purchasableItems;
}

export function removeCheckoutItems(items = [], mode = 'all', itemId = '') {
  const normalizedItemId = String(itemId ?? '');

  if (mode === 'single') {
    return items.filter((item) => (
      item.productId !== normalizedItemId
      && String(item.backendProductId ?? '') !== normalizedItemId
    ));
  }

  if (mode === 'selected') {
    return items.filter((item) => !item.selected);
  }

  return items.filter((item) => item.isSoldOut);
}

export function buildCompletedOrderSnapshot(payload) {
  const createdAt = new Date();
  const orderNumber = buildOrderNumber(createdAt);
  const orderedAt = formatDateParts(createdAt).label;
  const completedItems = cloneCartItems(payload.orderItems ?? []);

  return {
    orderNumber,
    orderedAt,
    orderedAtIso: createdAt.toISOString(),
    status: 'pending',
    statusCode: 'PENDING',
    statusLabel: ORDER_STATUS_LABELS.PENDING,
    orderItems: completedItems,
    orderCount: completedItems.reduce((sum, item) => sum + item.quantity, 0),
    ordererName: payload.ordererName ?? '',
    ordererPhone: payload.ordererPhone ?? '',
    receiverName: payload.receiverName ?? '',
    receiverPhone: payload.receiverPhone ?? '',
    zoneCode: payload.zoneCode ?? '',
    addressMain: payload.addressMain ?? '',
    addressDetail: payload.addressDetail ?? '',
    deliveryRequest: payload.deliveryRequest ?? '',
    scheduleText: payload.scheduleText ?? '',
    paymentMethod: payload.paymentMethod ?? 'kakaopay',
    paymentMethodLabel: payload.paymentMethodLabel ?? '',
    productTotal: payload.productTotal ?? 0,
    discountTotal: payload.discountTotal ?? 0,
    couponDiscount: payload.couponDiscount ?? 0,
    pointApplied: payload.pointApplied ?? 0,
    shippingTotal: payload.shippingTotal ?? 0,
    finalTotal: payload.finalTotal ?? 0,
    virtualAccount: null,
  };
}
