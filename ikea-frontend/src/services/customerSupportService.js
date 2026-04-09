import httpRequester from '../libs/httpRequester';
import { COMMERCE_SESSION_KEYS } from '../constants/commerce';

function unwrapArrayPayload(payload) {
  const source = payload?.data ?? payload;

  if (Array.isArray(source)) {
    return source;
  }

  if (Array.isArray(source?.content)) {
    return source.content;
  }

  if (Array.isArray(source?.items)) {
    return source.items;
  }

  if (Array.isArray(source?.orders)) {
    return source.orders;
  }

  if (Array.isArray(source?.qnas)) {
    return source.qnas;
  }

  return [];
}

function normalizeDigits(value) {
  return String(value ?? '').replace(/\D+/g, '');
}

function normalizeText(value) {
  return String(value ?? '').trim();
}

function normalizeEmail(value) {
  return normalizeText(value).toLowerCase();
}

function normalizePhoneNumber(value) {
  return normalizeDigits(value).slice(0, 11);
}

function normalizeOrderNumber(value) {
  return normalizeText(value).toUpperCase();
}

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined';
}

function formatDisplayDate(value) {
  const normalizedValue = String(value ?? '').trim();

  if (!normalizedValue) {
    return '-';
  }

  const date = new Date(normalizedValue);

  if (Number.isNaN(date.getTime())) {
    if (/^\d{4}\.\d{2}\.\d{2}$/.test(normalizedValue)) {
      return normalizedValue;
    }

    if (/^\d{4}-\d{2}-\d{2}/.test(normalizedValue)) {
      return normalizedValue.slice(0, 10).replace(/-/g, '.');
    }

    return normalizedValue;
  }

  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('.');
}

function normalizeQnaStatus(source = {}) {
  if (source.status) {
    return String(source.status).trim();
  }

  if (source.answerStatus) {
    return String(source.answerStatus).trim();
  }

  return source.answered || source.answerContent ? '답변 완료' : '답변 대기';
}

function buildNormalizedQnaRow(question = {}, answer = null, fallback = {}) {
  const normalizedAnswerContent = normalizeText(
    answer?.content
    ?? answer?.answer
    ?? fallback.answerContent
    ?? fallback.answer
    ?? '',
  );

  return {
    id: String(question.qnaId ?? question.id ?? fallback.qnaId ?? fallback.id ?? ''),
    title: normalizeText(question.title ?? fallback.title ?? '문의 제목'),
    writer: normalizeText(
      question.writer
      ?? question.author
      ?? question.memberName
      ?? fallback.writer
      ?? fallback.author
      ?? '',
    ),
    email: normalizeEmail(
      question.email
      ?? question.emailAddress
      ?? question.contactEmail
      ?? fallback.email
      ?? fallback.emailAddress
      ?? '',
    ),
    phoneNumber: normalizePhoneNumber(
      question.phoneNumber
      ?? question.phone
      ?? question.tel
      ?? fallback.phoneNumber
      ?? fallback.phone
      ?? '',
    ),
    status: normalizeQnaStatus({
      ...fallback,
      ...question,
      answerContent: normalizedAnswerContent,
    }),
    date: formatDisplayDate(
      question.createdAt
      ?? question.regDate
      ?? question.date
      ?? fallback.createdAt
      ?? fallback.regDate
      ?? fallback.date,
    ),
    questionContent: normalizeText(
      question.content
      ?? question.question
      ?? fallback.content
      ?? fallback.question
      ?? '',
    ),
    answerContent: normalizedAnswerContent,
    answerDate: formatDisplayDate(
      answer?.createdAt
      ?? answer?.regDate
      ?? fallback.answerCreatedAt
      ?? fallback.answeredAt,
    ),
  };
}

function normalizeDirectQnaRows(items = []) {
  return items.map((item) => {
    const question = item.question && typeof item.question === 'object'
      ? item.question
      : item;
    const answer = item.answer && typeof item.answer === 'object'
      ? item.answer
      : null;

    return buildNormalizedQnaRow(question, answer, item);
  });
}

function normalizeGroupedQnaRows(items = []) {
  const questions = items.filter((item) => Number(item.level ?? 0) === 0);
  const answersByParent = new Map(
    items
      .filter((item) => Number(item.level ?? 0) === 1)
      .map((item) => [String(item.parentId ?? ''), item]),
  );

  return questions.map((question) => (
    buildNormalizedQnaRow(question, answersByParent.get(String(question.qnaId ?? question.id)), question)
  ));
}

function normalizeCustomerSupportQnaRows(payload) {
  const items = unwrapArrayPayload(payload);

  if (!items.length) {
    return [];
  }

  const normalizedRows = items.some((item) => Object.prototype.hasOwnProperty.call(item ?? {}, 'level'))
    ? normalizeGroupedQnaRows(items)
    : normalizeDirectQnaRows(items);

  return normalizedRows.sort((left, right) => String(right.date).localeCompare(String(left.date)));
}

function normalizeGuestOrderItem(source = {}) {
  return {
    name: String(source.productName ?? source.name ?? '주문 상품').trim(),
    quantity: Number(source.quantity ?? 1) || 1,
  };
}

function normalizeGuestOrder(source = {}) {
  return {
    orderNumber: normalizeOrderNumber(source.orderNo ?? source.orderNumber ?? source.id ?? '-'),
    orderedAt: formatDisplayDate(source.orderedAt ?? source.createdAt ?? source.orderDate),
    buyerName: normalizeText(source.buyerName ?? source.receiverName ?? source.ordererName),
    phoneNumber: normalizeDigits(source.phoneNumber ?? source.receiverPhone ?? source.ordererPhone),
    statusLabel: normalizeText(source.statusLabel ?? source.orderStatusLabel ?? source.status ?? '-'),
    paymentMethodLabel: normalizeText(source.paymentMethodLabel ?? source.payment ?? '-'),
    finalTotal: Number(
      source.finalPrice
      ?? source.finalTotal
      ?? source.totalPrice
      ?? source.productTotal
      ?? source.amount
      ?? 0
    ) || 0,
    orderItems: unwrapArrayPayload(source.orderItems).map((item) => normalizeGuestOrderItem(item)),
  };
}

function readStoredGuestOrderHistory() {
  if (!canUseStorage()) {
    return [];
  }

  try {
    const raw = window.sessionStorage.getItem(COMMERCE_SESSION_KEYS.guestOrderHistory);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function lookupGuestOrdersFromSession({ buyerName = '', orderNumber = '', phoneNumber = '' }) {
  const normalizedBuyerName = normalizeText(buyerName);
  const normalizedOrderNumber = normalizeOrderNumber(orderNumber);
  const normalizedPhoneNumber = normalizeDigits(phoneNumber);

  return readStoredGuestOrderHistory()
    .map((item) => normalizeGuestOrder(item))
    .filter((order) => {
      if (normalizedBuyerName && order.buyerName !== normalizedBuyerName) {
        return false;
      }

      if (normalizedOrderNumber) {
        return order.orderNumber === normalizedOrderNumber;
      }

      if (normalizedPhoneNumber) {
        return order.phoneNumber === normalizedPhoneNumber;
      }

      return false;
    });
}

export async function getCustomerSupportQnaRows(query = {}, options = {}) {
  const endpoint = options.includeAll ? '/admin/qna' : '/qna';
  const response = await httpRequester.get(endpoint, { params: query });
  return normalizeCustomerSupportQnaRows(response);
}

export async function createCustomerSupportQna(payload) {
  const requestPayload = {
    title: normalizeText(payload.title),
    content: normalizeText(payload.content),
  };

  return httpRequester.post('/qna', requestPayload);
}

export async function lookupGuestOrders({
  buyerName = '',
  name = '',
  orderNumber = '',
  phoneNumber = '',
} = {}) {
  const normalizedBuyerName = normalizeText(buyerName || name);

  return lookupGuestOrdersFromSession({
    buyerName: normalizedBuyerName,
    orderNumber,
    phoneNumber,
  });
}
