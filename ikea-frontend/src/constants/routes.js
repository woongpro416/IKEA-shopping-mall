import {
  getDefaultFallbackCategory,
  getFallbackCategoryRouteMap,
} from '../services/catalogFallbackService';

const CATEGORY_ROUTE_STORAGE_KEY = 'homio-category-route-map';
const FALLBACK_CATEGORY_ROUTE_MAP = getFallbackCategoryRouteMap();
const DEFAULT_CATEGORY = getDefaultFallbackCategory();

export const DEFAULT_CATEGORY_ID = DEFAULT_CATEGORY?.backendCategoryId ?? '';
export const DEFAULT_CATEGORY_SLUG = DEFAULT_CATEGORY?.slug ?? '';

export const CATEGORY_ROUTE_MAP = FALLBACK_CATEGORY_ROUTE_MAP;

export const ROUTE_PATHS = {
  home: '/',
  accessDenied: '/access-denied',
  adminBase: '/admin',
  adminDashboard: '/admin/dashboard',
  adminProducts: '/admin/products',
  adminInventory: '/admin/inventory',
  adminMembers: '/admin/members',
  adminOrders: '/admin/orders',
  adminQna: '/admin/qna',
  adminReviews: '/admin/reviews',
  adminNotices: '/admin/notices',
  cart: '/cart',
  memberLogin: '/member/login',
  memberMyPage: '/member/mypage',
  memberJoin: '/member/join',
  memberJoinForm: '/member/join/form',
  memberJoinComplete: '/member/join/complete',
  orderCheckout: '/order/checkout',
  orderComplete: '/order/complete',
  paymentKakaoSuccess: '/payment/kakao/success',
  paymentKakaoCancel: '/payment/kakao/cancel',
  paymentKakaoFail: '/payment/kakao/fail',
  paymentTossSuccess: '/payment/toss/success',
  paymentTossFail: '/payment/toss/fail',
  guestOrderLookup: '/order/guest-lookup',
  policyTerms: '/policy/terms',
  policyPrivacy: '/policy/privacy',
  policyLocation: '/policy/location',
  customerServiceNotice: '/customer-service/notice',
  customerServiceFaq: '/customer-service/faq',
  customerServiceQna: '/customer-service/qna',
  customerServiceQnaLookup: '/customer-service/qna/lookup',
  customerServiceQnaWrite: '/customer-service/qna/write',
  productCategoryBase: '/product/category',
  productCategoryLegacyBase: '/category',
  productBase: '/product',
  search: '/search',
};

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined';
}

function normalizeCategoryRouteEntry(category = {}) {
  const slug = String(category.slug ?? '').trim().toLowerCase();

  if (!slug) {
    return null;
  }

  return {
    slug,
    backendCategoryId: String(category.backendCategoryId ?? category.id ?? '').trim(),
    label: String(category.label ?? category.name ?? '').trim(),
  };
}

function normalizeCategoryRouteMap(routeMap = {}) {
  return Object.fromEntries(
    Object.values(routeMap)
      .map((category) => normalizeCategoryRouteEntry(category))
      .filter(Boolean)
      .map((category) => [category.slug, category]),
  );
}

function readStoredCategoryRouteMap() {
  if (!canUseStorage()) {
    return {};
  }

  try {
    const raw = window.sessionStorage.getItem(CATEGORY_ROUTE_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return normalizeCategoryRouteMap(parsed);
  } catch {
    return {};
  }
}

function writeStoredCategoryRouteMap(routeMap = {}) {
  if (!canUseStorage()) {
    return;
  }

  window.sessionStorage.setItem(CATEGORY_ROUTE_STORAGE_KEY, JSON.stringify(routeMap));
}

function getCategoryRouteMap() {
  const storedCategoryRouteMap = readStoredCategoryRouteMap();

  if (!Object.keys(storedCategoryRouteMap).length) {
    return FALLBACK_CATEGORY_ROUTE_MAP;
  }

  return {
    ...FALLBACK_CATEGORY_ROUTE_MAP,
    ...storedCategoryRouteMap,
  };
}

export function syncCategoryRouteMap(categories = []) {
  const nextCategoryRouteMap = normalizeCategoryRouteMap(
    Object.fromEntries(
      categories.map((category) => [
        String(category.slug ?? '').trim().toLowerCase(),
        category,
      ]),
    ),
  );

  if (!Object.keys(nextCategoryRouteMap).length) {
    return getCategoryRouteMap();
  }

  writeStoredCategoryRouteMap(nextCategoryRouteMap);
  return getCategoryRouteMap();
}

export function resolveCategoryRoute(categoryValue = DEFAULT_CATEGORY_SLUG) {
  const categoryRouteMap = getCategoryRouteMap();
  const normalizedValue = String(categoryValue ?? '').trim().toLowerCase();

  if (normalizedValue && categoryRouteMap[normalizedValue]) {
    return categoryRouteMap[normalizedValue];
  }

  return (
    Object.values(categoryRouteMap).find(
      (category) => String(category.backendCategoryId) === String(categoryValue ?? '').trim(),
    ) ?? categoryRouteMap[DEFAULT_CATEGORY_SLUG] ?? null
  );
}

export function buildProductCategoryPath(categoryValue = DEFAULT_CATEGORY_SLUG) {
  const category = resolveCategoryRoute(categoryValue);
  return category?.slug
    ? `${ROUTE_PATHS.productCategoryBase}/${category.slug}`
    : ROUTE_PATHS.home;
}

export function getBackendCategoryId(categoryValue = DEFAULT_CATEGORY_SLUG) {
  return resolveCategoryRoute(categoryValue)?.backendCategoryId ?? '';
}

export function buildProductDetailPath(productId) {
  const normalizedProductId = String(productId ?? '').trim();
  return normalizedProductId
    ? `${ROUTE_PATHS.productBase}/${normalizedProductId}`
    : ROUTE_PATHS.home;
}

export function buildCustomerServiceNoticeDetailPath(noticeId) {
  const normalizedNoticeId = String(noticeId ?? '').trim();
  return normalizedNoticeId
    ? `${ROUTE_PATHS.customerServiceNotice}/${normalizedNoticeId}`
    : ROUTE_PATHS.customerServiceNotice;
}

export function buildSearchPath(keyword = '') {
  const normalizedKeyword = String(keyword ?? '').trim();

  if (!normalizedKeyword) {
    return ROUTE_PATHS.search;
  }

  return `${ROUTE_PATHS.search}?q=${encodeURIComponent(normalizedKeyword)}`;
}
