import { ORDER_STATUS_LABELS } from './orderStatus';

export const ADMIN_ORDER_STATUS_LABELS = Object.freeze({
  PENDING: ORDER_STATUS_LABELS.PENDING,
  PAID: ORDER_STATUS_LABELS.PAID,
  ORDERED: ORDER_STATUS_LABELS.ORDERED,
  DELIVERING: ORDER_STATUS_LABELS.DELIVERING,
  COMPLETED: ORDER_STATUS_LABELS.COMPLETED,
  CANCELLED: ORDER_STATUS_LABELS.CANCELLED,
});

export const ADMIN_ORDER_FILTER_OPTIONS = Object.freeze([
  { value: 'ALL', label: '전체' },
  { value: 'PENDING', label: ADMIN_ORDER_STATUS_LABELS.PENDING },
  { value: 'PAID', label: ADMIN_ORDER_STATUS_LABELS.PAID },
  { value: 'ORDERED', label: ADMIN_ORDER_STATUS_LABELS.ORDERED },
  { value: 'DELIVERING', label: ADMIN_ORDER_STATUS_LABELS.DELIVERING },
  { value: 'COMPLETED', label: ADMIN_ORDER_STATUS_LABELS.COMPLETED },
  { value: 'CANCELLED', label: ADMIN_ORDER_STATUS_LABELS.CANCELLED },
]);

export const ADMIN_ORDER_STATUS_TRANSITIONS = Object.freeze({
  PENDING: Object.freeze(['PAID']),
  PAID: Object.freeze(['ORDERED']),
  ORDERED: Object.freeze(['DELIVERING']),
  DELIVERING: Object.freeze(['COMPLETED']),
  COMPLETED: Object.freeze([]),
  CANCELLED: Object.freeze([]),
});

export const ADMIN_ORDER_STATUS_COLORS = Object.freeze({
  PENDING: '#7a8295',
  PAID: '#2759c6',
  ORDERED: '#4f86f7',
  DELIVERING: '#88aef2',
  COMPLETED: '#111111',
  CANCELLED: '#d95f5f',
});

export const ADMIN_PAYMENT_METHODS = Object.freeze({
  CARD: { label: '신용카드', color: '#1c3f94' },
  BANK: { label: '무통장입금', color: '#4f86f7' },
  KAKAO: { label: '카카오페이', color: '#fae34d' },
  TOSS: { label: '토스페이', color: '#c7d7f7' },
});

export const ADMIN_PAYMENT_STATUS_LABELS = Object.freeze({
  PENDING: '결제 대기',
  OK: '결제 완료',
  CANCEL: '결제 취소',
  FAILED: '결제 실패',
});

export function getAdminOrderStatusLabel(status) {
  return ADMIN_ORDER_STATUS_LABELS[status] ?? status ?? '-';
}

export function getNextAdminOrderStatuses(status) {
  return [...(ADMIN_ORDER_STATUS_TRANSITIONS[status] ?? [])];
}

export function normalizeAdminPaymentMethodCode(paymentMethod) {
  const code = String(paymentMethod ?? '')
    .trim()
    .toUpperCase();

  if (code === 'KAKAOPAY' || code === 'KAKAO_PAY') {
    return 'KAKAO';
  }

  if (code === 'TOSSPAY' || code === 'TOSS_PAY') {
    return 'TOSS';
  }

  if (code === 'ACCOUNT_TRANSFER' || code === 'BANK_TRANSFER' || code === 'VIRTUAL_ACCOUNT' || code === 'VBANK') {
    return 'BANK';
  }

  if (code in ADMIN_PAYMENT_METHODS) {
    return code;
  }

  return code;
}

export function getAdminPaymentMethodLabel(paymentMethod) {
  const normalizedCode = normalizeAdminPaymentMethodCode(paymentMethod);
  return ADMIN_PAYMENT_METHODS[normalizedCode]?.label ?? '';
}

export function normalizeAdminPaymentStatusCode(paymentStatus) {
  return String(paymentStatus ?? '')
    .trim()
    .toUpperCase();
}

export function getAdminPaymentStatusLabel(paymentStatus) {
  const normalizedCode = normalizeAdminPaymentStatusCode(paymentStatus);
  return ADMIN_PAYMENT_STATUS_LABELS[normalizedCode] ?? '';
}
