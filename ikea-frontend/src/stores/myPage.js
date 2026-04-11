import { defineStore } from 'pinia';
import {
  buildOrderStatusSteps,
  getOrderStatusLabel,
  normalizeOrderStatusCode,
} from '../constants/orderStatus';
import { normalizeMemberProfile } from '../mappers/myPageMapper';
import { getCurrentMember } from '../services/memberService';
import { getMyPageStaticContent, getFallbackMyPageProfile } from '../services/myPageService';
import { getMyOrders } from '../services/orderService';
import { getMyReviews } from '../services/reviewService';
import { resolveProfileErrorMessage } from '../utils/apiErrorMessage';
import { resolveOrderDateValue } from '../utils/orderDate';
import { useAccountStore } from './account';
import { useCatalogStore } from './catalog';

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

  if (Array.isArray(source?.orderItems)) {
    return source.orderItems;
  }

  return [];
}

function unwrapObjectPayload(payload) {
  return payload?.data ?? payload ?? {};
}

function normalizeIdentifier(value) {
  return String(value ?? '').trim();
}

function normalizeLookupValue(value) {
  return normalizeIdentifier(value)
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function formatPriceLabel(value) {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`;
}

function formatDateLabel(value) {
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

function createReviewKey(orderId, productId) {
  return [orderId, productId].map((value) => normalizeIdentifier(value)).join(':');
}

function buildWrittenReviewKeys(reviews = []) {
  return new Set(
    reviews.map((review) => createReviewKey(review?.orderId, review?.productId)).filter(Boolean),
  );
}

function buildSummaryCards(orders = [], reviewCount = 0) {
  const inProgressStatuses = new Set(['PENDING', 'PAID', 'ORDERED', 'DELIVERING']);
  const inProgressCount = orders.filter((order) => (
    inProgressStatuses.has(normalizeOrderStatusCode(order.orderStatus ?? order.status))
  )).length;

  return [
    { id: 'orders', label: '진행중 주문', value: `${inProgressCount}건` },
    { id: 'coupon', label: '사용 가능 쿠폰', value: '-' },
    { id: 'point', label: '보유 포인트', value: '-' },
    { id: 'review', label: '내 리뷰', value: `${reviewCount}건` },
  ];
}

function buildOrderSteps(orders = []) {
  const counts = {
    PENDING: 0,
    PAID: 0,
    ORDERED: 0,
    DELIVERING: 0,
    COMPLETED: 0,
  };

  orders.forEach((order) => {
    const normalizedStatus = normalizeOrderStatusCode(order.orderStatus ?? order.status);

    if (Object.prototype.hasOwnProperty.call(counts, normalizedStatus)) {
      counts[normalizedStatus] += 1;
    }
  });

  return buildOrderStatusSteps(counts);
}

function buildOrderOption(product, orderItem = {}, order = {}) {
  const optionParts = [
    product?.color,
    product?.material,
    normalizeIdentifier(orderItem.option),
    normalizeIdentifier(order.option),
  ].filter(Boolean);

  return optionParts.length ? optionParts.join(' / ') : '-';
}

function buildRecentOrders(orders = [], findProduct, writtenReviewKeys = new Set()) {
  return orders.flatMap((order, orderIndex) => {
    const orderId = normalizeIdentifier(order.orderId ?? order.id);
    const orderNumber = normalizeIdentifier(order.orderNo ?? order.orderNumber);
    const orderStatusCode = normalizeOrderStatusCode(order.orderStatus ?? order.status);
    const orderDate = formatDateLabel(resolveOrderDateValue(order));
    const orderItems = unwrapArrayPayload(order?.orderItems);
    const sourceItems = orderItems.length ? orderItems : [order];

    return sourceItems.map((orderItem, itemIndex) => {
      const productId = normalizeIdentifier(orderItem.productId ?? order.productId);
      const productName = normalizeIdentifier(orderItem.productName ?? order.productName);
      const product = typeof findProduct === 'function'
        ? findProduct(productId, productName)
        : null;
      const quantity = Number(orderItem.quantity ?? 1) || 1;
      const unitPrice = (
        orderItem.orderPrice
        ?? orderItem.price
        ?? product?.price
        ?? null
      );
      const priceValue = (
        orderItem.totalPrice
        ?? (unitPrice === null ? null : Number(unitPrice) * quantity)
        ?? (sourceItems.length === 1 ? order.totalPrice ?? order.finalPrice : null)
        ?? 0
      );
      const reviewKey = createReviewKey(orderId, productId);
      const reviewWritten = writtenReviewKeys.has(reviewKey);

      return {
        id: normalizeIdentifier(
          orderItem.orderItemId
          ?? `${orderId || orderNumber || `order-${orderIndex}`}-${productId || itemIndex}`,
        ),
        orderId,
        orderItemId: normalizeIdentifier(orderItem.orderItemId),
        orderNumber,
        date: orderDate,
        title: orderItem.productName ?? product?.name ?? '주문 상품',
        status: getOrderStatusLabel(orderStatusCode),
        statusCode: orderStatusCode,
        option: buildOrderOption(product, orderItem, order),
        image: orderItem.imgPath ?? product?.image ?? '',
        price: formatPriceLabel(priceValue),
        productId: productId || normalizeIdentifier(product?.id),
        quantity,
        canWriteReview: orderStatusCode === 'COMPLETED' && !reviewWritten,
        reviewWritten,
      };
    });
  }).slice(0, 5);
}

export const useMyPageStore = defineStore('myPage', {
  state: () => {
    const accountStore = useAccountStore();

    return {
      ...getMyPageStaticContent(),
      profile: getFallbackMyPageProfile(accountStore),
      myReviews: [],
      localWrittenReviewKeys: [],
      isProfileLoading: false,
      profileError: '',
      loaded: false,
      loadedSessionKey: '',
    };
  },
  actions: {
    resetDynamicSections() {
      const baseContent = getMyPageStaticContent();
      this.summaryCards = baseContent.summaryCards;
      this.orderSteps = baseContent.orderSteps;
      this.recentOrders = baseContent.recentOrders;
      this.wishListItems = baseContent.wishListItems;
      this.recentViewItems = baseContent.recentViewItems;
      this.myReviews = [];
    },
    markReviewWritten(order) {
      const reviewKey = createReviewKey(order?.orderId, order?.productId);

      if (!reviewKey) {
        return;
      }

      if (!this.localWrittenReviewKeys.includes(reviewKey)) {
        this.localWrittenReviewKeys = [...this.localWrittenReviewKeys, reviewKey];
      }

      this.summaryCards = this.summaryCards.map((card) => {
        if (card.id !== 'review') {
          return card;
        }

        const currentCount = Number.parseInt(String(card.value ?? '0').replace(/[^\d]/g, ''), 10) || 0;
        const nextCount = Math.max(currentCount, this.localWrittenReviewKeys.length);
        return {
          ...card,
          value: `${nextCount}건`,
        };
      });

      this.recentOrders = this.recentOrders.map((item) => (
        createReviewKey(item.orderId, item.productId) === reviewKey
          ? { ...item, canWriteReview: false, reviewWritten: true }
          : item
      ));
    },
    async loadProfile() {
      const accountStore = useAccountStore();
      const catalogStore = useCatalogStore();
      accountStore.hydrate();
      const sessionKey = accountStore.accessToken
        ? String(accountStore.memberId ?? accountStore.loginId ?? 'member')
        : '';

      if (!accountStore.accessToken) {
        this.profile = getFallbackMyPageProfile(accountStore);
        this.resetDynamicSections();
        this.localWrittenReviewKeys = [];
        this.profileError = '';
        this.loaded = true;
        this.loadedSessionKey = '';
        return;
      }

      this.isProfileLoading = true;
      this.profileError = '';

      const [, memberResult, ordersResult, reviewsResult] = await Promise.allSettled([
        catalogStore.ensureCatalogLoaded(),
        getCurrentMember(),
        getMyOrders(),
        getMyReviews(),
      ]);

      try {
        if (memberResult.status === 'fulfilled') {
          const memberPayload = unwrapObjectPayload(memberResult.value);
          accountStore.setMemberSession(memberPayload);
          accountStore.setProfileHydrated(true);
          this.profile = normalizeMemberProfile(memberPayload, accountStore);
        } else {
          accountStore.setProfileHydrated(false);
          this.profile = getFallbackMyPageProfile(accountStore);
          this.profileError = resolveProfileErrorMessage(memberResult.reason);
        }

        const orders = ordersResult.status === 'fulfilled'
          ? unwrapArrayPayload(ordersResult.value)
          : [];
        const reviews = reviewsResult.status === 'fulfilled'
          ? unwrapArrayPayload(reviewsResult.value)
          : [];
        const writtenReviewKeys = buildWrittenReviewKeys(reviews);
        this.localWrittenReviewKeys.forEach((reviewKey) => {
          const normalizedKey = normalizeIdentifier(reviewKey);

          if (normalizedKey) {
            writtenReviewKeys.add(normalizedKey);
          }
        });

        this.myReviews = reviews;
        this.summaryCards = buildSummaryCards(orders, Math.max(reviews.length, writtenReviewKeys.size));
        this.orderSteps = buildOrderSteps(orders);
        this.recentOrders = buildRecentOrders(
          orders,
          (productId, productName) => (
            catalogStore.findProductById(productId)
            ?? catalogStore.catalogProducts.find(
              (product) => normalizeLookupValue(product.name) === normalizeLookupValue(productName),
            )
            ?? null
          ),
          writtenReviewKeys,
        );
        this.wishListItems = [];
        this.recentViewItems = [];

        if (ordersResult.status === 'rejected' || reviewsResult.status === 'rejected') {
          this.profileError = this.profileError || '마이페이지 데이터를 모두 불러오지 못했습니다.';
        }
      } catch (error) {
        accountStore.setProfileHydrated(false);
        this.profile = getFallbackMyPageProfile(accountStore);
        this.profileError = resolveProfileErrorMessage(error);
      } finally {
        this.isProfileLoading = false;
        this.loaded = true;
        this.loadedSessionKey = sessionKey;
      }
    },
  },
});
