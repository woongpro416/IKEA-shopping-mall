function normalizeText(value = '') {
  return String(value ?? '').trim();
}

export function resolveGuestLookupMode(value = 'order') {
  return value === 'phone' ? 'phone' : 'order';
}

export function normalizeGuestLookupPhone(value = '') {
  return String(value ?? '').replace(/\D+/g, '').slice(0, 11);
}

export function normalizeGuestLookupOrderNumber(value = '') {
  return String(value ?? '').trim().slice(0, 30);
}

export function validateGuestOrderLookupForm(form = {}, options = {}) {
  const { allowPartial = false } = options;
  const mode = resolveGuestLookupMode(form.inquiryType ?? form.mode);
  const buyerName = normalizeText(form.buyerName ?? form.name);
  const orderNumber = normalizeGuestLookupOrderNumber(form.orderNumber);
  const phoneNumberText = normalizeText(form.phoneNumber);
  const phoneDigits = normalizeGuestLookupPhone(phoneNumberText);
  const activeValue = mode === 'order' ? orderNumber : phoneNumberText;

  if (allowPartial && !buyerName && !activeValue) {
    return '';
  }

  if (!buyerName) {
    return '주문자 이름을 입력해 주세요.';
  }

  if (mode === 'order') {
    if (!orderNumber) {
      return '주문번호를 입력해 주세요.';
    }

    if (!/^[A-Za-z0-9_-]{6,30}$/.test(orderNumber)) {
      return '주문번호는 영문, 숫자, 하이픈, 밑줄(_)만 입력해 주세요.';
    }

    return '';
  }

  if (!phoneNumberText) {
    return '휴대전화번호를 입력해 주세요.';
  }

  if (!/^\d{10,11}$/.test(phoneDigits)) {
    return '휴대전화번호는 10~11자리 숫자로 입력해 주세요.';
  }

  return '';
}

export function buildGuestOrderLookupQuery(form = {}) {
  const mode = resolveGuestLookupMode(form.inquiryType ?? form.mode);
  const query = {
    name: normalizeText(form.buyerName ?? form.name),
    mode,
  };

  if (mode === 'order') {
    query.orderNumber = normalizeGuestLookupOrderNumber(form.orderNumber);
    return query;
  }

  query.phoneNumber = normalizeGuestLookupPhone(form.phoneNumber);
  return query;
}
