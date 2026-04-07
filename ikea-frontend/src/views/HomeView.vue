<script setup>
import { computed, onMounted, shallowRef } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import HomeCategorySection from '../components/home/HomeCategorySection.vue';
import HomeEditorialSection from '../components/home/HomeEditorialSection.vue';
import HomeHeroSection from '../components/home/HomeHeroSection.vue';
import HomePickSection from '../components/home/HomePickSection.vue';
import HomeProductGridSection from '../components/home/HomeProductGridSection.vue';
import HomeShortcutStrip from '../components/home/HomeShortcutStrip.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import { useHeroCarousel } from '../composables/useHeroCarousel';
import { useHomeScrollBehavior } from '../composables/useHomeScrollBehavior';
import {
  buildProductCategoryPath,
  buildProductDetailPath,
} from '../constants/routes';
import { decorateStorefrontItems } from '../services/storefrontStockService';
import { useCatalogStore } from '../stores/catalog';
import { useHomeStore } from '../stores/home';
import { useWishlistStore } from '../stores/wishlist';

const route = useRoute();
const router = useRouter();
const catalogStore = useCatalogStore();
const homeStore = useHomeStore();
const wishlistStore = useWishlistStore();
const {
  categoryDealCollections,
  categoryDealFilters,
  curatedSpotlight,
  heroSlides,
  newItemCollections,
  newItemFilters,
  pickSection,
  topShortcutBoxes,
  weeklyDeals,
} = storeToRefs(homeStore);
const { catalogProducts } = storeToRefs(catalogStore);

const activeCategoryDealKey = shallowRef(categoryDealFilters.value[0]?.id ?? 'sofa');
const activeNewItemKey = shallowRef(newItemFilters.value[0]?.id ?? 'sofa');

const {
  currentSlide,
  displaySlides,
  heroCurrentLabel,
  heroTotalLabel,
  handleTrackTransitionEnd,
  nextSlide,
  previousSlide,
  selectSlide,
  startAutoSlide,
  stopAutoSlide,
  trackStyle,
} = useHeroCarousel(heroSlides);
const {
  scrollToSection,
} = useHomeScrollBehavior({ trackVisibility: false });

function formatHomePrice(value) {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`;
}

function createHomeMetaText(product) {
  const reviewCount = Number(product.reviews ?? 0);
  const rating = Number(product.rating ?? 0);

  if (rating > 0) {
    return `평점 ${rating.toFixed(1)} · 후기 ${reviewCount.toLocaleString('ko-KR')}개`;
  }

  return `후기 ${reviewCount.toLocaleString('ko-KR')}개`;
}

function createHomeProductCard(product, overrides = {}) {
  if (!product) {
    return null;
  }

  return {
    id: overrides.id ?? `home-product-${product.id}`,
    productId: String(product.id ?? product.productId ?? ''),
    categorySlug: product.categorySlug ?? '',
    typeSlug: product.typeSlug ?? '',
    brand: product.brand ?? 'HOMiO',
    title: product.name ?? '',
    image: overrides.image ?? product.image ?? product.imgPath ?? '',
    badge: overrides.badge ?? product.badge ?? product.label ?? product.categoryLabel ?? '',
    price: formatHomePrice(product.price),
    originalPrice: Number(product.originalPrice ?? 0) > Number(product.price ?? 0)
      ? formatHomePrice(product.originalPrice)
      : '',
    discount: Number(product.discountRate ?? 0) > 0 ? `${product.discountRate}%` : '',
    metaText: overrides.metaText ?? createHomeMetaText(product),
    tags: overrides.tags ?? (product.features ?? []).slice(0, 2),
  };
}

function createHomePickCard(product, overrides = {}) {
  if (!product) {
    return null;
  }

  return {
    id: overrides.id ?? `home-pick-${product.id}`,
    productId: String(product.id ?? product.productId ?? ''),
    categorySlug: product.categorySlug ?? '',
    typeSlug: product.typeSlug ?? '',
    brand: product.brand ?? 'HOMiO',
    title: product.name ?? '',
    image: overrides.image ?? product.altImage ?? product.image ?? product.imgPath ?? '',
    badge: overrides.badge ?? product.categoryLabel ?? '',
    accent: overrides.accent ?? 'blue',
    price: formatHomePrice(product.price),
    discount: Number(product.discountRate ?? 0) > 0 ? `${product.discountRate}%` : '',
    rating: Number(product.rating ?? 0) > 0 ? Number(product.rating).toFixed(1) : null,
    tags: overrides.tags ?? (product.features ?? []).slice(0, 2),
  };
}

function sortProductsByPriority(products = []) {
  return [...products].sort((left, right) => {
    const reviewGap = Number(right.reviews ?? 0) - Number(left.reviews ?? 0);
    if (reviewGap !== 0) {
      return reviewGap;
    }

    const ratingGap = Number(right.rating ?? 0) - Number(left.rating ?? 0);
    if (ratingGap !== 0) {
      return ratingGap;
    }

    return Number(right.price ?? 0) - Number(left.price ?? 0);
  });
}

function sortProductsByLatest(products = []) {
  return [...products].sort((left, right) => {
    const leftTimestamp = Date.parse(left.createdAt ?? '') || 0;
    const rightTimestamp = Date.parse(right.createdAt ?? '') || 0;
    return rightTimestamp - leftTimestamp;
  });
}

function takeCategoryProducts(products, categorySlug, limit = 4, mode = 'popular') {
  const categoryProducts = products.filter((product) => product.categorySlug === categorySlug);
  const orderedProducts = mode === 'latest'
    ? sortProductsByLatest(categoryProducts)
    : sortProductsByPriority(categoryProducts);

  return orderedProducts.slice(0, limit);
}

const derivedWeeklyDeals = computed(() => {
  const preferredCategories = ['sofa', 'bed-mattress', 'dining', 'kitchenware'];
  const cards = preferredCategories
    .map((categorySlug) => takeCategoryProducts(catalogProducts.value, categorySlug, 1)[0] ?? null)
    .filter(Boolean)
    .map((product) => createHomeProductCard(product, {
      badge: product.categoryLabel ?? product.label ?? '',
    }))
    .filter(Boolean);

  return cards.length ? cards : weeklyDeals.value;
});

const derivedCategoryDealCollections = computed(() => Object.fromEntries(
  categoryDealFilters.value.map((filter) => [
    filter.id,
    takeCategoryProducts(catalogProducts.value, filter.id, 4)
      .map((product) => createHomeProductCard(product, {
        badge: product.label ?? filter.label,
      }))
      .filter(Boolean),
  ]),
));

const derivedNewItemCollections = computed(() => Object.fromEntries(
  newItemFilters.value.map((filter) => [
    filter.id,
    takeCategoryProducts(catalogProducts.value, filter.id, 4, 'latest')
      .map((product) => createHomeProductCard(product, {
        badge: product.label ?? filter.label,
      }))
      .filter(Boolean),
  ]),
));

const derivedPickItems = computed(() => {
  const picks = [
    { slug: 'sofa', accent: 'yellow', id: 'pick-sofa' },
    { slug: 'bed-mattress', accent: 'blue', id: 'pick-bed' },
    { slug: 'desk', accent: 'yellow', id: 'pick-desk' },
    { slug: 'plant', accent: 'blue', id: 'pick-plant' },
  ];

  return picks
    .map((item) => {
      const product = takeCategoryProducts(catalogProducts.value, item.slug, 1)[0] ?? null;

      return createHomePickCard(product, {
        id: item.id,
        accent: item.accent,
      });
    })
    .filter(Boolean);
});

const activeCategoryDealBanner = computed(
  () => categoryDealCollections.value[activeCategoryDealKey.value]?.banner ?? null,
);
const decoratedWeeklyDeals = computed(() => decorateStorefrontItems(derivedWeeklyDeals.value));
const visibleCategoryDeals = computed(
  () => decorateStorefrontItems(
    derivedCategoryDealCollections.value[activeCategoryDealKey.value]?.length
      ? derivedCategoryDealCollections.value[activeCategoryDealKey.value]
      : categoryDealCollections.value[activeCategoryDealKey.value]?.items ?? [],
  ),
);
const visibleNewItems = computed(
  () => decorateStorefrontItems(
    derivedNewItemCollections.value[activeNewItemKey.value]?.length
      ? derivedNewItemCollections.value[activeNewItemKey.value]
      : newItemCollections.value[activeNewItemKey.value] ?? [],
  ),
);
const decoratedSpotlight = computed(() => ({
  ...(curatedSpotlight.value ?? {}),
  items: decorateStorefrontItems(curatedSpotlight.value?.items ?? []),
}));
const decoratedPickSection = computed(() => ({
  ...pickSection.value,
  items: decorateStorefrontItems(
    derivedPickItems.value.length ? derivedPickItems.value : pickSection.value?.items ?? [],
  ),
}));
const weeklyMorePath = computed(() => buildProductCategoryPath('sofa'));
const activeCategoryMorePath = computed(() => buildProductCategoryPath(activeCategoryDealKey.value));
const activeNewItemMorePath = computed(() => buildProductCategoryPath(activeNewItemKey.value));

onMounted(() => {
  void catalogStore.ensureCatalogLoaded();
  wishlistStore.ensureHydrated();
});

function handleShortcutClick(item) {
  if (item.anchorId) {
    scrollToSection(item.anchorId);
    return;
  }

  if (item.categorySlug) {
    router.push(buildProductCategoryPath(item.categorySlug));
  }
}

function handleHeroSlideClick(slide) {
  if (slide?.categorySlug) {
    router.push(buildProductCategoryPath(slide.categorySlug));
  }
}

function handleProductCardClick(item) {
  if (item?.productId) {
    router.push(buildProductDetailPath(item.productId));
    return;
  }

  if (item?.categorySlug) {
    router.push(buildProductCategoryPath(item.categorySlug));
  }
}

function isProductWishlisted(productId) {
  return wishlistStore.isProductWishlisted(productId);
}

function toggleProductWishlist(product) {
  wishlistStore.toggleProduct(product, {
    redirectPath: route.fullPath,
  });
}

function handleCategoryDealFilterChange(filterId) {
  activeCategoryDealKey.value = filterId;
}

function handleNewItemFilterChange(filterId) {
  activeNewItemKey.value = filterId;
}

function handleFeaturedClick(featured) {
  if (featured?.categorySlug) {
    router.push(buildProductCategoryPath(featured.categorySlug));
  }
}
</script>

<template>
  <SiteChrome>
    <main class="hs-main">
      <HomeHeroSection
        :slides="heroSlides"
        :current-slide="currentSlide"
        :display-slides="displaySlides"
        :hero-current-label="heroCurrentLabel"
        :hero-total-label="heroTotalLabel"
        :track-style="trackStyle"
        @activate="handleHeroSlideClick"
        @next="nextSlide"
        @pause="stopAutoSlide"
        @previous="previousSlide"
        @resume="startAutoSlide"
        @select-slide="selectSlide"
        @track-transition-end="handleTrackTransitionEnd"
      />

      <HomeShortcutStrip :items="topShortcutBoxes" @activate="handleShortcutClick" />

      <HomeProductGridSection
        id="weekly-picks"
        title="이번 주 추천 상품"
        subtitle="지금 바로 두고 싶은 상품을 먼저 골라보세요."
        :items="decoratedWeeklyDeals"
        :more-to="weeklyMorePath"
        :is-product-wishlisted="isProductWishlisted"
        @product-activate="handleProductCardClick"
        @toggle-wishlist="toggleProductWishlist"
      />

      <HomeEditorialSection
        :spotlight="decoratedSpotlight"
        @featured-activate="handleFeaturedClick"
        @product-activate="handleProductCardClick"
      />

      <HomeCategorySection
        id="category-focus"
        title="카테고리별 인기상품"
        subtitle="카테고리를 바꿔가며 자주 보는 상품을 확인해 보세요."
        :filters="categoryDealFilters"
        :active-filter-id="activeCategoryDealKey"
        :banner="activeCategoryDealBanner"
        :items="visibleCategoryDeals"
        :more-to="activeCategoryMorePath"
        :is-product-wishlisted="isProductWishlisted"
        @banner-activate="handleFeaturedClick(activeCategoryDealBanner)"
        @filter-change="handleCategoryDealFilterChange"
        @product-activate="handleProductCardClick"
        @toggle-wishlist="toggleProductWishlist"
      />

      <HomeProductGridSection
        id="new-arrivals"
        title="카테고리 둘러보기"
        subtitle="카테고리별 주요 상품을 빠르게 비교할 수 있도록 정리했습니다."
        :items="visibleNewItems"
        :filters="newItemFilters"
        :active-filter-id="activeNewItemKey"
        :more-to="activeNewItemMorePath"
        :is-product-wishlisted="isProductWishlisted"
        badge-variant="yellow"
        @filter-change="handleNewItemFilterChange"
        @product-activate="handleProductCardClick"
        @toggle-wishlist="toggleProductWishlist"
      />

      <HomePickSection
        :title="pickSection.title"
        :items="decoratedPickSection.items"
        @product-activate="handleProductCardClick"
      />
    </main>
  </SiteChrome>
</template>

<style scoped>
.hs-main {
  display: grid;
  gap: 72px;
  padding: 0 0 92px;
}

@media (max-width: 1180px) {
  .hs-main {
    gap: 56px;
  }
}
</style>
