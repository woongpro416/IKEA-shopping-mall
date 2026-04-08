<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CommonStatePanel from '../components/common/CommonStatePanel.vue';
import GuestCheckoutPromptDialog from '../components/common/GuestCheckoutPromptDialog.vue';
import WishlistToggleButton from '../components/common/WishlistToggleButton.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import ProductCartAddedDialog from '../components/product/ProductCartAddedDialog.vue';
import ProductDimensionDiagram from '../components/product/ProductDimensionDiagram.vue';
import { useFeedback } from '../composables/useFeedback';
import { useProductGallery } from '../composables/useProductGallery';
import { useProductReviews } from '../composables/useProductReviews';
import {
  buildProductDeliveryMessage,
  buildProductOptionSummary,
} from '../constants/productAttributeConfig';
import {
  ROUTE_PATHS,
  buildProductCategoryPath,
  buildProductDetailPath,
} from '../constants/routes';
import { storeToRefs } from 'pinia';
import { useAccountStore } from '../stores/account';
import { useCartStore } from '../stores/cart';
import { useCatalogStore } from '../stores/catalog';
import { useWishlistStore } from '../stores/wishlist';
import {
  decorateStorefrontItems,
  resolveStorefrontAvailability,
} from '../services/storefrontStockService';
import {
  recordRecentViewProduct,
  resolveRecentViewSessionKey,
} from '../services/recentViewService';
import { hasAuthenticatedSession } from '../utils/accessControl';
import { resolveCartActionErrorMessage } from '../utils/apiErrorMessage';

const route = useRoute();
const router = useRouter();
const accountStore = useAccountStore();
const cartStore = useCartStore();
const catalogStore = useCatalogStore();
const wishlistStore = useWishlistStore();
const { showError } = useFeedback();
const { catalogProducts } = storeToRefs(catalogStore);

const EMPTY_PRODUCT = Object.freeze({
  id: '',
  productId: '',
  categorySlug: '',
  categoryLabel: '',
  label: '',
  badge: '',
  brand: '',
  name: '',
  image: '',
  imageAlt: '',
  altImage: '',
  price: 0,
  reviews: 0,
  rating: 0,
  typeSlug: 'all',
});

const quantity = ref(1);
const isCartDialogOpen = ref(false);
const isGuestCheckoutPromptOpen = ref(false);
const isResolvingProduct = ref(true);
const didFailToResolveProduct = ref(false);
let latestResolveToken = 0;
const LOW_STOCK_THRESHOLD = 5;

const requestedProductId = computed(() => String(route.params.productId ?? '').trim());

function normalizeLookupValue(value) {
  return String(value ?? '').trim().toLowerCase();
}

function pickNumericProductId(...candidates) {
  for (const candidate of candidates) {
    const normalizedCandidate = String(candidate ?? '').trim();

    if (/^\d+$/.test(normalizedCandidate)) {
      return normalizedCandidate;
    }
  }

  return '';
}

function resolveCatalogNumericProductId(product = EMPTY_PRODUCT, { includeReviewProductId = true } = {}) {
  const directProductId = pickNumericProductId(
    product.backendProductId,
    includeReviewProductId ? product.reviewProductId : '',
    product.productId,
    product.id,
  );

  if (directProductId) {
    return directProductId;
  }

  const normalizedProductName = normalizeLookupValue(product.name);

  if (normalizedProductName) {
    const matchedCatalogProduct = catalogProducts.value.find((catalogProduct) => (
      normalizeLookupValue(catalogProduct.name) === normalizedProductName
    ));
    const matchedProductId = pickNumericProductId(
      matchedCatalogProduct?.backendProductId,
      matchedCatalogProduct?.reviewProductId,
      matchedCatalogProduct?.productId,
      matchedCatalogProduct?.id,
    );

    if (matchedProductId) {
      return matchedProductId;
    }
  }

  return pickNumericProductId(requestedProductId.value);
}

function syncRecentViewHistory(productId = '') {
  const normalizedProductId = String(productId ?? '').trim();
  const sessionKey = resolveRecentViewSessionKey(accountStore);
  const product = catalogStore.findProductById(normalizedProductId);

  if (!normalizedProductId || !sessionKey || !product) {
    return;
  }

  recordRecentViewProduct({
    productId: normalizedProductId,
    brand: product.brand,
    title: product.name,
    subtitle: [product.categoryLabel, product.label].filter(Boolean).join(' / '),
    price: formatPrice(product.price),
    image: product.image,
  }, sessionKey);
}

async function resolveCurrentProduct() {
  const resolveToken = ++latestResolveToken;
  const normalizedProductId = requestedProductId.value;

  isResolvingProduct.value = true;
  didFailToResolveProduct.value = false;

  accountStore.hydrate();
  wishlistStore.ensureHydrated();

  if (!normalizedProductId) {
    isResolvingProduct.value = false;
    didFailToResolveProduct.value = true;
    return;
  }

  const hasPreviewProduct = Boolean(catalogStore.findProductById(normalizedProductId));
  isResolvingProduct.value = !hasPreviewProduct;

  await catalogStore.ensureCatalogLoaded().catch(() => {});

  if (!catalogStore.productsLoadedFromApi) {
    if (resolveToken !== latestResolveToken) {
      return;
    }

    if (catalogStore.findProductById(normalizedProductId)) {
      syncRecentViewHistory(normalizedProductId);
    }

    isResolvingProduct.value = false;
    return;
  }

  try {
    await catalogStore.loadProductDetail(normalizedProductId);
  } catch {
    if (resolveToken !== latestResolveToken) {
      return;
    }

    didFailToResolveProduct.value = !catalogStore.findProductById(normalizedProductId);
  } finally {
    if (resolveToken !== latestResolveToken) {
      return;
    }

    if (catalogStore.findProductById(normalizedProductId)) {
      syncRecentViewHistory(normalizedProductId);
    }

    isResolvingProduct.value = false;
  }
}

const currentProduct = computed(() => (
  catalogStore.findProductById(requestedProductId.value) ?? EMPTY_PRODUCT
));
const hasCurrentProduct = computed(() => Boolean(String(currentProduct.value?.id ?? '').trim()));
const currentBackendProductId = computed(() => resolveCatalogNumericProductId(currentProduct.value, {
  includeReviewProductId: false,
}));
const currentReviewProductId = computed(() => resolveCatalogNumericProductId(currentProduct.value));

const detailContent = computed(() => catalogStore.getProductDetailContent(currentProduct.value));
const {
  averageRating,
  hasLoadedReviews,
  isLoadingReviews,
  loadProductReviews,
  reviewCount,
  reviewItems,
  reviewLoadErrorMessage,
} = useProductReviews(currentReviewProductId);

const {
  galleryImages,
  imageStyle,
  isZoomed,
  selectedImage,
  selectedIndex,
  selectImage,
  toggleZoom,
  updateZoomOrigin,
  zoomLabel,
  zoomSymbol,
} = useProductGallery(computed(() => detailContent.value.galleryImages));

const resolvedRatingValue = computed(() => {
  if (reviewItems.value.length) {
    return averageRating.value;
  }

  const rating = Number(currentProduct.value.rating ?? 0);
  if (rating > 0) {
    return rating;
  }

  return fallbackAverageRating.value;
});

const ratingLabel = computed(() => (
  resolvedRatingValue.value === null ? '-' : resolvedRatingValue.value.toFixed(1)
));

const resolvedReviewCount = computed(() => (
  reviewItems.value.length
    ? reviewCount.value
    : Math.max(
      Number(currentProduct.value.reviews ?? 0),
      fallbackReviewHighlights.value.length,
    )
));

const reviewCountLabel = computed(() => (
  Number(resolvedReviewCount.value ?? 0).toLocaleString('ko-KR')
));

const totalPrice = computed(() => currentProduct.value.price * quantity.value);
const isCurrentProductWishlisted = computed(() => wishlistStore.isProductWishlisted(currentProduct.value?.id));

const summaryFacts = computed(() => {
  const facts = detailContent.value.quickFacts ?? [];
  return facts.slice(0, 4);
});

const descriptionParagraphs = computed(() => detailContent.value.description ?? []);
const highlightItems = computed(() => detailContent.value.highlights ?? []);
const measurementItems = computed(() => detailContent.value.measurements ?? []);
const fallbackReviewHighlights = computed(() => detailContent.value.reviewHighlights ?? []);
const dimensionImage = computed(() => detailContent.value.dimensionImage ?? '');
const shouldUseDimensionImage = computed(() => Boolean(detailContent.value.useDimensionImage && dimensionImage.value));
const fallbackAverageRating = computed(() => {
  const ratings = fallbackReviewHighlights.value
    .map((item) => Number(item?.rating ?? item?.ratingLabel ?? 0))
    .filter((value) => value > 0);

  if (!ratings.length) {
    return null;
  }

  return ratings.reduce((sum, value) => sum + value, 0) / ratings.length;
});
const reviewSummaryMessage = computed(() => {
  if (reviewItems.value.length) {
    if (reviewCount.value === 1) {
      return '등록된 리뷰 1개를 표시하고 있습니다.';
    }

    return `등록된 리뷰 ${reviewCountLabel.value}개를 표시하고 있습니다.`;
  }

  return detailContent.value.reviewIntro ?? '고객 리뷰를 확인해 보세요.';
});
const reviewStatusNote = computed(() => {
  return '';
});
const displayedReviewItems = computed(() => (
  reviewItems.value.length ? reviewItems.value : fallbackReviewHighlights.value
));

const deliveryMessage = computed(() => buildProductDeliveryMessage(currentProduct.value));
const purchaseOptionCopy = computed(() => buildProductOptionSummary(currentProduct.value));
const availability = computed(() => resolveStorefrontAvailability({
  ...currentProduct.value,
  backendProductId: currentBackendProductId.value,
}));
const isSoldOut = computed(() => availability.value.isSoldOut);
const soldOutMessage = computed(() => availability.value.stockMessage);
const availableStockCount = computed(() => {
  if (!availability.value.isTracked) {
    return null;
  }

  const stock = Number(availability.value.availableStock);
  return Number.isFinite(stock) ? stock : null;
});
const isLowStock = computed(() => (
  !isSoldOut.value
  && availability.value.isTracked
  && availableStockCount.value !== null
  && availableStockCount.value > 0
  && availableStockCount.value <= LOW_STOCK_THRESHOLD
));
const lowStockLabel = computed(() => {
  if (!isLowStock.value) {
    return '';
  }
  return `[품절임박] 잔여 ${availableStockCount.value}개`;
});

const relatedProducts = computed(() => {
  const currentId = currentProduct.value.id;
  const sameType = catalogProducts.value.filter((product) => (
    product.id !== currentId
    && product.categorySlug === currentProduct.value.categorySlug
    && product.typeSlug === currentProduct.value.typeSlug
  ));
  const sameCategory = catalogProducts.value.filter((product) => (
    product.id !== currentId
    && product.categorySlug === currentProduct.value.categorySlug
    && product.typeSlug !== currentProduct.value.typeSlug
  ));
  const others = catalogProducts.value.filter((product) => (
    product.id !== currentId && product.categorySlug !== currentProduct.value.categorySlug
  ));

  return decorateStorefrontItems([...sameType, ...sameCategory, ...others].slice(0, 4));
});

const sectionTabs = [
  { id: 'description', label: '제품 설명' },
  { id: 'dimensions', label: '치수' },
  { id: 'reviews', label: '고객 리뷰' },
];

watch(
  requestedProductId,
  () => {
    void resolveCurrentProduct();
  },
  { immediate: true },
);

watch(
  () => currentProduct.value.id,
  () => {
    quantity.value = 1;
    isCartDialogOpen.value = false;
    isGuestCheckoutPromptOpen.value = false;

    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  },
  { immediate: true },
);

function formatPrice(value) {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`;
}

function decreaseQuantity() {
  if (quantity.value > 1) {
    quantity.value -= 1;
  }
}

function increaseQuantity() {
  if (isSoldOut.value) {
    return;
  }

  quantity.value += 1;
}

async function syncCurrentProductToCart() {
  if (isSoldOut.value || !hasCurrentProduct.value) {
    return null;
  }

  try {
    return await cartStore.addCartItem(currentProduct.value?.id, {
      quantity: quantity.value,
      backendProductId: currentBackendProductId.value,
      productName: currentProduct.value?.name,
    });
  } catch (error) {
    showError(resolveCartActionErrorMessage(error, '장바구니 처리 중 오류가 발생했습니다.'));
    return null;
  }
}

async function openCartDialog() {
  const cartItem = await syncCurrentProductToCart();

  if (!cartItem) {
    return;
  }

  isCartDialogOpen.value = true;
}

function closeCartDialog() {
  isCartDialogOpen.value = false;
}

function viewCartFromDialog() {
  closeCartDialog();
  router.push(ROUTE_PATHS.cart);
}

async function goToCheckout() {
  if (isSoldOut.value || !hasCurrentProduct.value) {
    return;
  }

  const cartItem = await syncCurrentProductToCart();

  router.push({
    path: ROUTE_PATHS.orderCheckout,
    query: {
      mode: 'single',
      itemId: cartItem?.productId ?? String(currentProduct.value?.id ?? ''),
    },
  });
}

function openGuestCheckoutPrompt() {
  isGuestCheckoutPromptOpen.value = true;
}

function closeGuestCheckoutPrompt() {
  isGuestCheckoutPromptOpen.value = false;
}

function moveToMemberLogin() {
  closeGuestCheckoutPrompt();
  router.push({
    path: ROUTE_PATHS.memberLogin,
    query: {
      redirect: route.fullPath,
    },
  });
}

async function handleBuyNow() {
  if (!hasAuthenticatedSession(accountStore)) {
    openGuestCheckoutPrompt();
    return;
  }

  await goToCheckout();
}

async function continueGuestCheckout() {
  closeGuestCheckoutPrompt();
  await goToCheckout();
}

function toggleCurrentProductWishlist() {
  if (!hasCurrentProduct.value) {
    return;
  }

  wishlistStore.toggleProduct(currentProduct.value, {
    redirectPath: route.fullPath,
  });
}

function scrollToSection(sectionId) {
  document.getElementById(sectionId)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  });
}

function goToProduct(productId) {
  router.push(buildProductDetailPath(productId));
}

function handleDialogProductSelect(productId) {
  closeCartDialog();
  goToProduct(productId);
}
</script>

<template>
  <SiteChrome>
    <main class="detail-page">
      <div class="detail-page__inner">
        <section v-if="isResolvingProduct" class="detail-empty-state">
          <CommonStatePanel
            title="상품 정보를 불러오고 있습니다."
            description="선택한 상품의 상세 정보를 확인하고 있습니다."
            layout="boxed"
            compact
          />
        </section>

        <section v-else-if="!hasCurrentProduct && didFailToResolveProduct" class="detail-empty-state">
          <CommonStatePanel
            title="상품 정보를 찾지 못했습니다."
            description="다른 상품을 선택해 다시 확인해 주세요."
            layout="boxed"
            compact
          >
            <template #actions>
              <button class="detail-empty-state__action detail-empty-state__action--secondary" type="button" @click="resolveCurrentProduct">
                다시 시도
              </button>
              <RouterLink class="detail-empty-state__action" :to="ROUTE_PATHS.home">홈으로 이동</RouterLink>
            </template>
          </CommonStatePanel>
        </section>

        <template v-else>
        <nav class="detail-breadcrumb" aria-label="breadcrumb">
          <RouterLink to="/" class="detail-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>〉</span>
          <RouterLink :to="buildProductCategoryPath(currentProduct.categorySlug)">{{ currentProduct.categoryLabel }}</RouterLink>
          <span>〉</span>
          <span>{{ currentProduct.label }}</span>
        </nav>

        <section class="detail-hero">
          <div class="detail-gallery">
            <div
              class="detail-gallery__stage"
              :class="{ 'is-zoomed': isZoomed }"
              @click="toggleZoom"
              @mousemove="updateZoomOrigin"
            >
              <img
                :src="selectedImage"
                :alt="currentProduct.imageAlt ?? currentProduct.name"
                :style="imageStyle"
              />
              <span class="detail-gallery__badge">{{ currentProduct.badge }}</span>
              <span class="detail-gallery__caption">{{ currentProduct.label }} 대표</span>
              <span class="detail-gallery__zoom">
                <strong>{{ zoomSymbol }}</strong>
                {{ zoomLabel }}
              </span>
            </div>

            <div class="detail-gallery__thumbs" aria-label="상품 이미지 썸네일">
              <button
                v-for="(image, index) in galleryImages"
                :key="image"
                class="detail-gallery__thumb"
                :class="{ 'is-active': selectedIndex === index }"
                type="button"
                @click.stop="selectImage(index)"
              >
                <img :src="image" :alt="`${currentProduct.name} 이미지 ${index + 1}`" />
              </button>
            </div>
          </div>

        <section class="detail-summary">
            <p class="detail-summary__brand">{{ currentProduct.brand }}</p>
            <h1>{{ currentProduct.name }}</h1>

            <div class="detail-summary__rating">
              <strong>{{ ratingLabel }}</strong>
              <span>리뷰 {{ reviewCountLabel }}개</span>
            </div>

            <div class="detail-summary__price">
              <strong>{{ formatPrice(currentProduct.price) }}</strong>
              <span v-if="isLowStock" class="detail-summary__low-stock">
                {{ lowStockLabel }}
              </span>
            </div>

            <p v-if="isSoldOut" class="detail-summary__soldout">
              {{ soldOutMessage }}
            </p>

            <div class="detail-summary__delivery">
              <span>배송정보</span>
              <p>{{ deliveryMessage }}</p>
            </div>

            <div class="detail-summary__benefits">
              <span>카드 무이자 혜택</span>
              <span>추가 적립금 혜택</span>
            </div>

            <div class="detail-summary__facts">
              <article v-for="fact in summaryFacts" :key="fact.label">
                <span>{{ fact.label }}</span>
                <strong>{{ fact.value }}</strong>
              </article>
            </div>
          </section>

          <aside class="detail-purchase-panel">
            <div class="detail-purchase-panel__option">
              <span>선택 옵션</span>
              <strong>{{ purchaseOptionCopy }}</strong>
            </div>

            <div class="detail-purchase-panel__option">
              <span>배송 일정</span>
              <strong>주문서작성 단계에서 확인</strong>
            </div>

            <div class="detail-purchase-panel__meta">
              <strong>예상 배송비 0원</strong>
              <span>혜택은 결제 단계에서 확인</span>
            </div>

            <section class="detail-purchase-panel__card">
              <strong class="detail-purchase-panel__title">{{ currentProduct.name }}</strong>
              <p>{{ purchaseOptionCopy }}</p>

              <div class="detail-purchase-panel__line">
                <div class="detail-purchase-panel__qty">
                  <button type="button" :disabled="isSoldOut" @click="decreaseQuantity">-</button>
                  <span>{{ quantity }}</span>
                  <button type="button" :disabled="isSoldOut" @click="increaseQuantity">+</button>
                </div>
                <strong>{{ formatPrice(totalPrice) }}</strong>
              </div>
            </section>

            <div class="detail-purchase-panel__total">
              <span>총 구매가</span>
              <strong>{{ formatPrice(totalPrice) }}</strong>
            </div>

            <div class="detail-purchase-panel__actions">
              <WishlistToggleButton
                class="detail-purchase-panel__wish"
                variant="panel"
                :active="isCurrentProductWishlisted"
                @toggle="toggleCurrentProductWishlist"
              />
              <button class="detail-purchase-panel__cart" type="button" :disabled="isSoldOut" @click="openCartDialog">
                {{ isSoldOut ? '품절' : '장바구니' }}
              </button>
              <button class="detail-purchase-panel__buy" type="button" :disabled="isSoldOut" @click="handleBuyNow">
                {{ isSoldOut ? '품절' : '바로구매' }}
              </button>
            </div>
          </aside>
        </section>

        <nav class="detail-section-nav" aria-label="상세 정보 이동">
          <button
            v-for="tab in sectionTabs"
            :key="tab.id"
            type="button"
            @click="scrollToSection(tab.id)"
          >
            {{ tab.label }}
          </button>
        </nav>

        <section id="description" class="detail-section detail-section--lined">
          <div class="detail-section__head">
            <h2>제품 설명</h2>
          </div>

          <div class="detail-section__content detail-section__content--split">
            <article class="detail-copy-card">
              <p v-for="paragraph in descriptionParagraphs" :key="paragraph">
                {{ paragraph }}
              </p>
            </article>

            <aside class="detail-note-card">
              <strong>이 상품의 핵심 포인트</strong>
              <ul>
                <li v-for="item in highlightItems" :key="item">{{ item }}</li>
              </ul>
            </aside>
          </div>
        </section>

        <section id="dimensions" class="detail-section detail-section--lined">
          <div class="detail-section__head">
            <h2>치수</h2>
          </div>

            <div class="detail-section__content detail-section__content--split detail-section__content--measure">
            <div class="detail-dimension-panel">
              <div v-if="shouldUseDimensionImage" class="detail-dimension-panel__image-box">
                <img
                  class="detail-dimension-panel__image"
                  :src="dimensionImage"
                  :alt="`${currentProduct.name} 치수 이미지`"
                />
              </div>
              <ProductDimensionDiagram
                v-else
                :measurements="measurementItems"
              />
            </div>

            <div class="detail-measure-list">
              <article
                v-for="item in measurementItems"
                :key="item.label"
                class="detail-measure-list__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
          </div>
        </section>

        <section id="reviews" class="detail-section detail-section--lined">
          <div class="detail-section__head">
            <h2>고객 리뷰</h2>
          </div>

          <div class="detail-review-summary">
            <div class="detail-review-summary__score">
              <strong>{{ ratingLabel }}</strong>
              <span>평균 평점</span>
            </div>
            <div class="detail-review-summary__copy">
              <strong>리뷰 {{ reviewCountLabel }}개 기준 요약</strong>
            </div>
          </div>

          <div v-if="reviewStatusNote" class="detail-review-feedback">
            <p>{{ reviewStatusNote }}</p>
            <button
              v-if="reviewLoadErrorMessage"
              class="detail-review-feedback__action"
              type="button"
              @click="loadProductReviews"
            >
              다시 불러오기
            </button>
          </div>

          <div v-if="displayedReviewItems.length" class="detail-review-grid">
            <article
              v-for="review in displayedReviewItems"
              :key="review.id ?? review.title"
              class="detail-review-card"
            >
              <div class="detail-review-card__head">
                <strong>{{ review.author ?? review.title }}</strong>
                <span v-if="review.ratingLabel ?? review.rating">★ {{ (review.ratingLabel ?? review.rating?.toFixed?.(1) ?? '') }}</span>
              </div>
              <p>{{ review.content ?? review.body }}</p>
              <small v-if="review.meta">{{ review.meta }}</small>
            </article>
          </div>
        </section>

        <section v-if="relatedProducts.length" class="detail-related detail-section--lined">
          <div class="detail-section__head">
            <h2>다른 사람들이 함께 본 상품</h2>
          </div>

          <div class="detail-related__grid">
            <article
              v-for="item in relatedProducts"
              :key="item.id"
              class="detail-related-card"
              :class="{ 'is-soldout': item.isSoldOut }"
            >
              <button
                class="detail-related-card__link"
                type="button"
                @click="goToProduct(item.id)"
                :aria-label="`${item.name} 상세 페이지로 이동`"
              />
              <div class="detail-related-card__image">
                <img :src="item.image" :alt="item.imageAlt ?? item.name" />
                <span
                  class="detail-related-card__badge"
                  :class="{ 'detail-related-card__badge--soldout': item.isSoldOut }"
                >
                  {{ item.isSoldOut ? '품절' : item.badge }}
                </span>
              </div>
              <div class="detail-related-card__copy">
                <p>{{ item.brand }}</p>
                <h3>{{ item.name }}</h3>
                <strong>{{ formatPrice(item.price) }}</strong>
                <p v-if="item.isSoldOut" class="detail-related-card__stock">
                  품절 · 상세 페이지에서 재입고 여부를 확인해 주세요.
                </p>
              </div>
            </article>
          </div>
        </section>
        </template>
      </div>
    </main>
    <ProductCartAddedDialog
      :open="isCartDialogOpen"
      :product="currentProduct"
      :recommendations="relatedProducts"
      @close="closeCartDialog"
      @view-cart="viewCartFromDialog"
      @select-product="handleDialogProductSelect"
    />
    <GuestCheckoutPromptDialog
      :open="isGuestCheckoutPromptOpen"
      @close="closeGuestCheckoutPrompt"
      @guest-order="continueGuestCheckout"
      @member-order="moveToMemberLogin"
    />
  </SiteChrome>
</template>

<style scoped>
.detail-summary__price {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-summary__low-stock {
  display: inline-flex;
  align-items: center;
  color: #b45a2b;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.detail-empty-state {
  padding-top: 24px;
}

.detail-empty-state__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  background: #111827;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
}

.detail-empty-state__action--secondary {
  border: 1px solid #d5d8dd;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
}

.detail-review-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}

.detail-review-feedback p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

.detail-review-feedback__action {
  flex-shrink: 0;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #d5d8dd;
  border-radius: 999px;
  background: #ffffff;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 720px) {
  .detail-summary__price {
    gap: 8px;
  }

  .detail-summary__low-stock {
    font-size: 12px;
  }

  .detail-review-feedback {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
