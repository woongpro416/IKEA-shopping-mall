import { createRouter, createWebHistory } from 'vue-router';
import {
  buildCustomerServiceNoticeDetailPath,
  buildProductCategoryPath,
  buildProductDetailPath,
  DEFAULT_CATEGORY_ID,
  ROUTE_PATHS,
} from '../constants/routes';
import { useAccountStore } from '../stores/account';
import { hasAdminAccess, hasAuthenticatedSession } from '../utils/accessControl';

const HomeView = () => import('../views/HomeView.vue');
const AccessDeniedView = () => import('../views/AccessDeniedView.vue');
const AdminDashboardView = () => import('../views/AdminDashboardView.vue');
const AdminProductsView = () => import('../views/AdminProductsView.vue');
const AdminInventoryView = () => import('../views/AdminInventoryView.vue');
const AdminMembersView = () => import('../views/AdminMembersView.vue');
const AdminOrdersView = () => import('../views/AdminOrdersView.vue');
const AdminQnaView = () => import('../views/AdminQnaView.vue');
const AdminReviewsView = () => import('../views/AdminReviewsView.vue');
const AdminNoticesView = () => import('../views/AdminNoticesView.vue');
const ProductCategoryView = () => import('../views/ProductCategoryView.vue');
const CartView = () => import('../views/CartView.vue');
const CheckoutView = () => import('../views/CheckoutView.vue');
const OrderCompleteView = () => import('../views/OrderCompleteView.vue');
const KakaoPaymentView = () => import('../views/KakaoPaymentView.vue');
const TossPaymentView = () => import('../views/TossPaymentView.vue');
const CustomerServiceView = () => import('../views/CustomerServiceView.vue');
const CustomerServiceQnaWriteView = () => import('../views/CustomerServiceQnaWriteView.vue');
const CustomerServiceNoticeDetailView = () => import('../views/CustomerServiceNoticeDetailView.vue');
const GuestOrderLookupView = () => import('../views/GuestOrderLookupView.vue');
const LoginView = () => import('../views/LoginView.vue');
const MyPageView = () => import('../views/MyPageView.vue');
const JoinTermsView = () => import('../views/JoinTermsView.vue');
const JoinFormView = () => import('../views/JoinFormView.vue');
const JoinCompleteView = () => import('../views/JoinCompleteView.vue');
const ProductDetailView = () => import('../views/ProductDetailView.vue');
const SearchResultsView = () => import('../views/SearchResultsView.vue');
const LegalDocumentView = () => import('../views/LegalDocumentView.vue');

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }

    return {
      top: 0,
      left: 0,
    };
  },
  routes: [
    {
      path: ROUTE_PATHS.home,
      name: 'home',
      component: HomeView,
    },
    {
      path: ROUTE_PATHS.accessDenied,
      name: 'access-denied',
      component: AccessDeniedView,
    },
    {
      path: ROUTE_PATHS.adminDashboard,
      name: 'admin-dashboard',
      component: AdminDashboardView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminProducts,
      name: 'admin-products',
      component: AdminProductsView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminInventory,
      name: 'admin-inventory',
      component: AdminInventoryView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminMembers,
      name: 'admin-members',
      component: AdminMembersView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminOrders,
      name: 'admin-orders',
      component: AdminOrdersView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminQna,
      name: 'admin-qna',
      component: AdminQnaView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminReviews,
      name: 'admin-reviews',
      component: AdminReviewsView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.adminNotices,
      name: 'admin-notices',
      component: AdminNoticesView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
      },
    },
    {
      path: ROUTE_PATHS.productCategoryBase,
      redirect: buildProductCategoryPath(),
    },
    {
      path: `${ROUTE_PATHS.productCategoryBase}/:categoryId(\\d+)`,
      redirect: (to) => buildProductCategoryPath(to.params.categoryId ?? DEFAULT_CATEGORY_ID),
    },
    {
      path: `${ROUTE_PATHS.productCategoryBase}/:categorySlug`,
      name: 'product-category',
      component: ProductCategoryView,
    },
    {
      path: ROUTE_PATHS.cart,
      name: 'cart',
      component: CartView,
    },
    {
      path: ROUTE_PATHS.orderCheckout,
      name: 'order-checkout',
      component: CheckoutView,
    },
    {
      path: ROUTE_PATHS.orderComplete,
      name: 'order-complete',
      component: OrderCompleteView,
    },
    {
      path: ROUTE_PATHS.paymentKakaoSuccess,
      name: 'payment-kakao-success',
      component: KakaoPaymentView,
      props: { status: 'success' },
    },
    {
      path: ROUTE_PATHS.paymentKakaoCancel,
      name: 'payment-kakao-cancel',
      component: KakaoPaymentView,
      props: { status: 'cancel' },
    },
    {
      path: ROUTE_PATHS.paymentKakaoFail,
      name: 'payment-kakao-fail',
      component: KakaoPaymentView,
      props: { status: 'fail' },
    },
    {
      path: ROUTE_PATHS.paymentTossSuccess,
      name: 'payment-toss-success',
      component: TossPaymentView,
      props: { status: 'success' },
    },
    {
      path: ROUTE_PATHS.paymentTossFail,
      name: 'payment-toss-fail',
      component: TossPaymentView,
      props: { status: 'fail' },
    },
    {
      path: `${ROUTE_PATHS.customerServiceNotice}/:noticeId`,
      name: 'customer-service-notice-detail',
      component: CustomerServiceNoticeDetailView,
    },
    {
      path: ROUTE_PATHS.customerServiceNotice,
      name: 'customer-service-notice',
      component: CustomerServiceView,
    },
    {
      path: ROUTE_PATHS.customerServiceFaq,
      name: 'customer-service-faq',
      component: CustomerServiceView,
    },
    {
      path: ROUTE_PATHS.customerServiceQnaLookup,
      redirect: ROUTE_PATHS.customerServiceQna,
    },
    {
      path: ROUTE_PATHS.customerServiceQnaWrite,
      name: 'customer-service-qna-write',
      component: CustomerServiceQnaWriteView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: ROUTE_PATHS.customerServiceQna,
      name: 'customer-service-qna',
      component: CustomerServiceView,
    },
    {
      path: ROUTE_PATHS.guestOrderLookup,
      name: 'guest-order-lookup',
      component: GuestOrderLookupView,
    },
    {
      path: ROUTE_PATHS.policyTerms,
      name: 'policy-terms',
      component: LegalDocumentView,
      props: { documentId: 'terms' },
    },
    {
      path: ROUTE_PATHS.policyPrivacy,
      name: 'policy-privacy',
      component: LegalDocumentView,
      props: { documentId: 'privacy' },
    },
    {
      path: ROUTE_PATHS.policyLocation,
      name: 'policy-location',
      component: LegalDocumentView,
      props: { documentId: 'location' },
    },
    {
      path: ROUTE_PATHS.memberLogin,
      name: 'member-login',
      component: LoginView,
    },
    {
      path: ROUTE_PATHS.memberMyPage,
      name: 'member-my-page',
      component: MyPageView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: ROUTE_PATHS.memberJoin,
      name: 'member-join',
      component: JoinTermsView,
    },
    {
      path: ROUTE_PATHS.memberJoinForm,
      name: 'member-join-form',
      component: JoinFormView,
    },
    {
      path: ROUTE_PATHS.memberJoinComplete,
      name: 'member-join-complete',
      component: JoinCompleteView,
    },
    {
      path: `${ROUTE_PATHS.productBase}/:productId`,
      name: 'product-detail',
      component: ProductDetailView,
    },
    {
      path: ROUTE_PATHS.search,
      name: 'search-results',
      component: SearchResultsView,
    },
    {
      path: `${ROUTE_PATHS.productCategoryLegacyBase}/:categoryValue`,
      redirect: (to) => buildProductCategoryPath(to.params.categoryValue ?? DEFAULT_CATEGORY_ID),
    },
    {
      path: ROUTE_PATHS.productCategoryLegacyBase,
      redirect: buildProductCategoryPath(),
    },
    {
      path: '/checkout',
      redirect: ROUTE_PATHS.orderCheckout,
    },
    {
      path: '/login',
      redirect: ROUTE_PATHS.memberLogin,
    },
    {
      path: '/mypage',
      redirect: ROUTE_PATHS.memberMyPage,
    },
    {
      path: '/signup',
      redirect: ROUTE_PATHS.memberJoin,
    },
    {
      path: '/signup/info',
      redirect: ROUTE_PATHS.memberJoinForm,
    },
    {
      path: '/signup/complete',
      redirect: ROUTE_PATHS.memberJoinComplete,
    },
    {
      path: '/goods/:productId',
      redirect: (to) => buildProductDetailPath(to.params.productId),
    },
    {
      path: '/customer-service',
      redirect: ROUTE_PATHS.customerServiceNotice,
    },
    {
      path: ROUTE_PATHS.adminBase,
      redirect: ROUTE_PATHS.adminDashboard,
    },
    {
      path: '/cs/notice/:noticeId',
      redirect: (to) => buildCustomerServiceNoticeDetailPath(to.params.noticeId),
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: ROUTE_PATHS.home,
    },
  ],
});

router.beforeEach((to) => {
  const accountStore = useAccountStore();
  accountStore.hydrate();

  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth);
  const requiresAdmin = to.matched.some((record) => record.meta?.requiresAdmin);
  const allowGuestPreview = to.matched.some((record) => record.meta?.allowGuestPreview);

  if (!requiresAuth && !requiresAdmin) {
    return true;
  }

  if (!hasAuthenticatedSession(accountStore) && !allowGuestPreview) {
    return {
      path: ROUTE_PATHS.memberLogin,
      query: {
        reason: 'auth-required',
        redirect: to.fullPath,
      },
    };
  }

  if (requiresAdmin && !hasAdminAccess(accountStore)) {
    return {
      path: ROUTE_PATHS.accessDenied,
      query: {
        reason: 'admin-required',
        from: to.fullPath,
      },
    };
  }

  return true;
});

export default router;
