function normalizeIdentifier(value) {
  return String(value ?? '').trim();
}

function normalizeNumber(value, fallback = 0) {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : fallback;
}

function buildOrderItems(items = []) {
  return items.map((item) => ({
    productId: normalizeIdentifier(
      item.backendProductId
      ?? item.reviewProductId
      ?? item.productId,
    ),
    quantity: Math.max(1, normalizeNumber(item.quantity, 1)),
  }));
}

export function buildDeliveryAddress(payload = {}) {
  const zoneCode = normalizeIdentifier(payload.zoneCode);
  const addressMain = normalizeIdentifier(payload.addressMain);
  const addressDetail = normalizeIdentifier(payload.addressDetail);

  return [
    zoneCode ? `(${zoneCode})` : '',
    addressMain,
    addressDetail,
  ]
    .filter(Boolean)
    .join(' ')
    .trim();
}

export function buildCheckoutOrderRequest(payload = {}) {
  return {
    address: buildDeliveryAddress(payload),
    ordererName: normalizeIdentifier(payload.ordererName),
    ordererPhone: normalizeIdentifier(payload.ordererPhone),
    receiverName: normalizeIdentifier(payload.receiverName),
    receiverPhone: normalizeIdentifier(payload.receiverPhone),
    zoneCode: normalizeIdentifier(payload.zoneCode),
    addressMain: normalizeIdentifier(payload.addressMain),
    addressDetail: normalizeIdentifier(payload.addressDetail),
    deliveryRequest: normalizeIdentifier(payload.deliveryRequest),
    scheduleText: normalizeIdentifier(payload.scheduleText),
    paymentMethod: normalizeIdentifier(payload.paymentMethod),
    paymentMethodLabel: normalizeIdentifier(payload.paymentMethodLabel),
    productTotal: normalizeNumber(payload.productTotal),
    discountTotal: normalizeNumber(payload.discountTotal),
    couponDiscount: normalizeNumber(payload.couponDiscount),
    pointApplied: normalizeNumber(payload.pointApplied),
    shippingTotal: normalizeNumber(payload.shippingTotal),
    finalTotal: normalizeNumber(payload.finalTotal),
    orderItems: buildOrderItems(payload.orderItems),
  };
}

export function buildMemberOrderRequest(payload = {}) {
  return buildCheckoutOrderRequest(payload);
}

export function buildGuestOrderRequest(payload = {}) {
  return {
    guestCartKey: normalizeIdentifier(payload.guestCartKey),
    guestName: normalizeIdentifier(payload.guestName ?? payload.ordererName),
    guestPhone: normalizeIdentifier(payload.guestPhone ?? payload.ordererPhone),
    address: buildDeliveryAddress(payload),
  };
}
