import { defineStore } from 'pinia';
import {
  getAdminMembers,
  getAdminOrderCount,
  getAdminOrders,
  getAdminPayments,
  getAdminProductCount,
  getAdminQnas,
  getAdminReviews,
  getProductCatalog,
} from '../services/adminService';
import {
  buildAdminDashboard,
} from '../mappers/adminDashboardMapper';
import { resolveAdminActionErrorMessage } from '../utils/apiErrorMessage';

function normalizeArrayPayload(payload, fallback = []) {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.data)) {
    return payload.data;
  }

  if (Array.isArray(payload?.content)) {
    return payload.content;
  }

  if (Array.isArray(payload?.items)) {
    return payload.items;
  }

  return fallback;
}

function normalizeMembers(payload) {
  return normalizeArrayPayload(payload).map((item) => ({
    ...item,
    password: undefined,
  }));
}

function deriveCategoriesFromProducts(products = []) {
  const categoryMap = new Map();

  products.forEach((product) => {
    const slug = String(product.categorySlug ?? '').trim();
    const label = String(product.categoryLabel ?? product.categoryName ?? '').trim();

    if (!slug && !label) {
      return;
    }

    const key = slug || label;

    if (!categoryMap.has(key)) {
      categoryMap.set(key, {
        slug: slug || key,
        label: label || slug || key,
      });
    }
  });

  return Array.from(categoryMap.values());
}

function createEmptyDashboard() {
  return buildAdminDashboard({
    categories: [],
    products: [],
    orders: [],
    payments: [],
    members: [],
    reviews: [],
    qnas: [],
    productCount: 0,
    orderCount: 0,
  });
}

export const useAdminDashboardStore = defineStore('adminDashboard', {
  state: () => ({
    dashboard: createEmptyDashboard(),
    isDashboardLoading: false,
    loadErrorMessage: '',
    loaded: false,
  }),
  actions: {
    refreshSummaryCounts() {
      this.dashboard.summaryCards = this.dashboard.summaryCards.map((card) => {
        if (card.id === 'products') {
          return { ...card, value: `${this.dashboard.productRows.length}개` };
        }

        if (card.id === 'orders') {
          return { ...card, value: `${this.dashboard.orderRows.length}건` };
        }

        if (card.id === 'members') {
          return { ...card, value: `${this.dashboard.memberRows.length}명` };
        }

        if (card.id === 'reviews') {
          return { ...card, value: `${this.dashboard.reviewRows.length}건` };
        }

        return card;
      });
    },
    removeMember(memberId) {
      this.dashboard.memberRows = this.dashboard.memberRows.filter((member) => member.id !== memberId);
      this.refreshSummaryCounts();
    },
    async loadDashboard() {
      this.isDashboardLoading = true;
      this.loadErrorMessage = '';

      try {
        const [
          productCountResult,
          orderCountResult,
          productsResult,
          ordersResult,
          paymentsResult,
          membersResult,
          reviewsResult,
          qnasResult,
        ] = await Promise.allSettled([
          getAdminProductCount(),
          getAdminOrderCount(),
          getProductCatalog(),
          getAdminOrders(),
          getAdminPayments(),
          getAdminMembers(),
          getAdminReviews(),
          getAdminQnas(),
        ]);

        const products = productsResult.status === 'fulfilled'
          ? normalizeArrayPayload(productsResult.value)
          : [];
        const orders = ordersResult.status === 'fulfilled'
          ? normalizeArrayPayload(ordersResult.value)
          : [];
        const payments = paymentsResult.status === 'fulfilled'
          ? normalizeArrayPayload(paymentsResult.value)
          : [];
        const members = membersResult.status === 'fulfilled'
          ? normalizeMembers(membersResult.value)
          : [];
        const reviews = reviewsResult.status === 'fulfilled'
          ? normalizeArrayPayload(reviewsResult.value)
          : [];
        const qnas = qnasResult.status === 'fulfilled'
          ? normalizeArrayPayload(qnasResult.value)
          : [];
        const productCount = productCountResult.status === 'fulfilled'
          ? Number(productCountResult.value)
          : 0;
        const orderCount = orderCountResult.status === 'fulfilled'
          ? Number(orderCountResult.value)
          : 0;
        const categories = deriveCategoriesFromProducts(products);

        const hasRejectedRequest = [
          productCountResult,
          orderCountResult,
          productsResult,
          ordersResult,
          paymentsResult,
          membersResult,
          reviewsResult,
          qnasResult,
        ].some((result) => result.status === 'rejected');

        if (hasRejectedRequest) {
          this.loadErrorMessage = '일부 관리자 통계를 불러오지 못했습니다.';
        }

        this.dashboard = buildAdminDashboard({
          categories,
          products,
          orders,
          payments,
          members,
          reviews,
          qnas,
          productCount,
          orderCount,
        });
      } catch (error) {
        this.dashboard = createEmptyDashboard();
        this.loadErrorMessage = resolveAdminActionErrorMessage(
          error,
          '관리자 대시보드를 불러오지 못했습니다.',
        );
      } finally {
        this.isDashboardLoading = false;
        this.loaded = true;
      }
    },
  },
});
