const currencyFormatter = new Intl.NumberFormat('ko-KR');

function formatPrice(value) {
  return `${currencyFormatter.format(Number(value ?? 0))}원`;
}

function hasValue(value) {
  return value !== undefined && value !== null && String(value).trim() !== '';
}

function pickValue(...values) {
  return values.find(hasValue);
}

function buildAddress(memberSession = {}) {
  return pickValue(
    memberSession.address,
    [memberSession.addressMain, memberSession.addressDetail].filter(hasValue).join(' '),
  );
}

export function createCatalogItem(productId, fallback, findProductById) {
  const product = typeof findProductById === 'function' ? findProductById(productId) : null;

  if (!product) {
    return fallback;
  }

  return {
    brand: product.brand,
    title: product.name,
    subtitle: [product.categoryLabel, product.label].filter(Boolean).join(' / '),
    price: formatPrice(product.price),
    image: product.image,
    productId: String(product.id),
  };
}

export function createFallbackProfile(memberSession) {
  return {
    memberName: memberSession.memberName || '',
    loginId: memberSession.loginId || '',
    address: buildAddress(memberSession) || '',
    phone: memberSession.phoneNumber || '',
    role: memberSession.role || '',
    membershipLabel: memberSession.membershipLabel || '',
  };
}

export function normalizeMemberProfile(payload, memberSession) {
  const source = payload?.data ?? payload ?? {};

  return {
    memberName: pickValue(source.memberName, source.name, source.userName, memberSession.memberName, '-'),
    loginId: pickValue(source.loginId, source.email, source.memberEmail, memberSession.loginId, '-'),
    address: pickValue(
      source.address,
      [source.addressMain, source.addressDetail].filter(hasValue).join(' '),
      source.baseAddress,
      source.defaultAddress,
      buildAddress(memberSession),
      '-',
    ),
    phone: pickValue(source.phone, source.phoneNumber, source.tel, memberSession.phoneNumber, '-'),
    role: pickValue(source.role, source.memberRole, memberSession.role, '-'),
    membershipLabel: pickValue(source.grade, source.membershipLabel, memberSession.membershipLabel, '-'),
  };
}
