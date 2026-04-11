<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CategoryFilterPanel from '../components/category/CategoryFilterPanel.vue';
import CategoryProductSection from '../components/category/CategoryProductSection.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import {
  buildProductCategoryPath,
  buildProductDetailPath,
  resolveCategoryRoute,
} from '../constants/routes';
import {
  FILTER_TITLES,
  createDefaultOpenGroups,
  createEmptyFilterState,
  getFilterFieldIds,
  getFilterOptions,
  getProductFilterValue,
} from '../constants/categoryFilters';
import { decorateStorefrontItems } from '../services/storefrontStockService';
import { useCatalogStore } from '../stores/catalog';
import { useWishlistStore } from '../stores/wishlist';

const route = useRoute();
const router = useRouter();
const catalogStore = useCatalogStore();
const wishlistStore = useWishlistStore();

onMounted(() => {
  void catalogStore.ensureCatalogLoaded();
  wishlistStore.ensureHydrated();
});

const selectedSort = ref('인기순');
const selectedPageSize = ref(20);
const currentPage = ref(1);
const selectedTypeSlug = ref('all');
const priceRange = ref({ min: 0, max: 0 });
const priceInputs = ref({ min: '0', max: '0' });
const filterState = ref(createEmptyFilterState());
const openGroups = ref(createDefaultOpenGroups());

const currentCategory = computed(() => catalogStore.getCategoryBySlug(route.params.categorySlug));
const currentCards = computed(() => currentCategory.value.cards ?? []);
const selectedCategoryCard = computed(() =>
  currentCards.value.find((item) => item.slug === selectedTypeSlug.value) ?? null,
);
const currentHeading = computed(() => selectedCategoryCard.value?.label ?? currentCategory.value.label);
const categoryTabItems = computed(() => [{ slug: 'all', label: '전체' }, ...currentCards.value]);
const categoryProducts = computed(() => catalogStore.getCatalogProductsByCategory(currentCategory.value.slug));
const visibleCategoryProducts = computed(() => {
  if (selectedTypeSlug.value === 'all') {
    return categoryProducts.value;
  }

  return categoryProducts.value.filter((product) => product.typeSlug === selectedTypeSlug.value);
});

const defaultPrice = computed(() => {
  const prices = visibleCategoryProducts.value
    .map((product) => Number(product.price))
    .filter((price) => Number.isFinite(price));

  if (!prices.length) {
    return { min: 0, max: 0 };
  }

  return {
    min: 0,
    max: Math.max(...prices),
  };
});

const categoryFilterFields = computed(() => getFilterFieldIds(currentCategory.value.slug, selectedTypeSlug.value));
const priceStep = computed(() => {
  const prices = visibleCategoryProducts.value
    .map((product) => Number(product.price))
    .filter((price) => Number.isFinite(price) && price > 0);

  if (!prices.length) {
    return 100;
  }

  return prices.every((price) => price % 100 === 0) ? 100 : 1;
});

const filterGroups = computed(() => {
  return categoryFilterFields.value
    .map((groupId) => {
      const options = getFilterOptions(visibleCategoryProducts.value, groupId);

      if (!options.length) {
        return null;
      }

      return {
        id: groupId,
        title: FILTER_TITLES[groupId],
        options,
      };
    })
    .filter(Boolean);
});

const isPriceChanged = computed(() => (
  priceRange.value.min !== defaultPrice.value.min || priceRange.value.max !== defaultPrice.value.max
));

const activeFilterCount = computed(() => {
  const optionCount = Object.values(filterState.value).reduce((sum, items) => sum + items.length, 0);
  return optionCount + (isPriceChanged.value ? 1 : 0);
});

const filteredProducts = computed(() => {
  const result = visibleCategoryProducts.value.filter((product) => {
    const matchesDynamicFilters = categoryFilterFields.value.every((groupId) =>
      matchesArrayFilter(filterState.value[groupId], getProductFilterValue(product, groupId)));

    return (
      product.price >= priceRange.value.min
      && product.price <= priceRange.value.max
      && matchesDynamicFilters
    );
  });

  switch (selectedSort.value) {
    case '낮은가격순':
      result.sort((a, b) => a.price - b.price);
      break;
    case '높은가격순':
      result.sort((a, b) => b.price - a.price);
      break;
    case '리뷰많은순':
      result.sort((a, b) => Number(b.reviews ?? 0) - Number(a.reviews ?? 0));
      break;
    case '할인율순':
      result.sort((a, b) => Number(b.discountRate ?? 0) - Number(a.discountRate ?? 0));
      break;
    default:
      result.sort((a, b) => (
        (Number(b.reviews ?? 0) * Number(b.rating ?? 0))
        - (Number(a.reviews ?? 0) * Number(a.rating ?? 0))
      ));
      break;
  }

  return decorateStorefrontItems(result);
});

const pageCount = computed(() => Math.max(Math.ceil(filteredProducts.value.length / selectedPageSize.value), 1));
const displayedProducts = computed(() => {
  const start = (currentPage.value - 1) * selectedPageSize.value;
  return filteredProducts.value.slice(start, start + selectedPageSize.value);
});
const priceMinLabel = computed(() => formatPrice(priceRange.value.min));
const priceMaxLabel = computed(() => formatPrice(priceRange.value.max));
const minPricePercent = computed(() => {
  const range = defaultPrice.value.max - defaultPrice.value.min;
  return range > 0 ? (((priceRange.value.min - defaultPrice.value.min) / range) * 100) : 0;
});
const maxPricePercent = computed(() => {
  const range = defaultPrice.value.max - defaultPrice.value.min;
  return range > 0 ? (((priceRange.value.max - defaultPrice.value.min) / range) * 100) : 100;
});
const priceTrackStyle = computed(() => ({
  '--min-percent': `${minPricePercent.value}%`,
  '--max-percent': `${maxPricePercent.value}%`,
}));
const priceFilterResetKey = computed(
  () => `${currentCategory.value.slug}:${selectedTypeSlug.value}:${defaultPrice.value.max}`,
);

function normalizeTypeSlug(type, categorySlug = currentCategory.value.slug) {
  if (typeof type !== 'string') {
    return 'all';
  }

  const cards = catalogStore.getCategoryBySlug(categorySlug)?.cards ?? [];
  return cards.some((item) => item.slug === type) ? type : 'all';
}

function goToCategory(typeSlug = 'all') {
  router.push({
    path: buildProductCategoryPath(currentCategory.value.slug),
    query: typeSlug && typeSlug !== 'all' ? { type: typeSlug } : {},
  });
}

function toggleGroup(groupId) {
  openGroups.value[groupId] = !openGroups.value[groupId];
}

function toggleFilter(groupId, option) {
  const current = filterState.value[groupId];

  if (current.includes(option)) {
    filterState.value[groupId] = current.filter((item) => item !== option);
    currentPage.value = 1;
    return;
  }

  filterState.value[groupId] = [...current, option];
  currentPage.value = 1;
}

function resetFilters() {
  priceRange.value = { ...defaultPrice.value };
  syncPriceInputs();
  filterState.value = createEmptyFilterState();
  openGroups.value = createDefaultOpenGroups();
  currentPage.value = 1;
}

function matchesArrayFilter(selectedItems, valueOrList) {
  if (!selectedItems.length) {
    return true;
  }

  if (Array.isArray(valueOrList)) {
    return selectedItems.every((item) => valueOrList.includes(item));
  }

  return selectedItems.includes(valueOrList);
}

function normalizePriceValue(value, fallback) {
  const number = Number(String(value).replace(/[^\d]/g, ''));

  if (!Number.isFinite(number)) {
    return fallback;
  }

  return Math.min(defaultPrice.value.max, Math.max(defaultPrice.value.min, number));
}

function clampPriceRange(changedSide) {
  priceRange.value.min = normalizePriceValue(priceRange.value.min, defaultPrice.value.min);
  priceRange.value.max = normalizePriceValue(priceRange.value.max, defaultPrice.value.max);

  if (changedSide === 'min' && priceRange.value.min > priceRange.value.max) {
    priceRange.value.max = priceRange.value.min;
  }

  if (changedSide === 'max' && priceRange.value.max < priceRange.value.min) {
    priceRange.value.min = priceRange.value.max;
  }
}

function handlePriceInput(changedSide) {
  priceRange.value[changedSide] = normalizePriceValue(
    priceInputs.value[changedSide],
    defaultPrice.value[changedSide],
  );
  clampPriceRange(changedSide);
  syncPriceInputs();
  currentPage.value = 1;
}

function handlePriceRangeInput(changedSide) {
  clampPriceRange(changedSide);
  syncPriceInputs();
  currentPage.value = 1;
}

function formatPrice(price) {
  return `${Number(price).toLocaleString('ko-KR')}원`;
}

function formatPriceInputValue(price) {
  return Number(price).toLocaleString('ko-KR');
}

function syncPriceInputs() {
  priceInputs.value = {
    min: formatPriceInputValue(priceRange.value.min),
    max: formatPriceInputValue(priceRange.value.max),
  };
}

function updatePriceInput({ side, value }) {
  priceInputs.value[side] = value;
  handlePriceInput(side);
}

function commitPriceInput(side) {
  handlePriceInput(side);
}

function updatePriceRange({ side, value }) {
  priceRange.value[side] = Number(value);
  handlePriceRangeInput(side);
}

function updateSort(value) {
  selectedSort.value = value;
  currentPage.value = 1;
}

function updatePageSize(value) {
  selectedPageSize.value = Number(value);
  currentPage.value = 1;
}

function updateCurrentPage(value) {
  currentPage.value = Number(value);
}

function clearPriceFilter() {
  priceRange.value = { ...defaultPrice.value };
  syncPriceInputs();
  currentPage.value = 1;
}

function clearGroupFilter(groupId) {
  filterState.value[groupId] = [];
  currentPage.value = 1;
}

function isProductWishlisted(productId) {
  return wishlistStore.isProductWishlisted(productId);
}

function toggleProductWishlist(product) {
  wishlistStore.toggleProduct(product, {
    redirectPath: route.fullPath,
  });
}

watch(
  () => defaultPrice.value,
  (nextPrice) => {
    priceRange.value = { ...nextPrice };
    syncPriceInputs();
  },
  { immediate: true, deep: true },
);

watch(
  () => pageCount.value,
  (nextPageCount) => {
    if (currentPage.value > nextPageCount) {
      currentPage.value = nextPageCount;
    }
  },
);

watch(
  () => [route.params.categorySlug, route.query.type],
  ([categorySlug, type]) => {
    const resolvedCategory = resolveCategoryRoute(categorySlug);
    const normalizedType = normalizeTypeSlug(type, resolvedCategory.slug);

    if (categorySlug !== resolvedCategory.slug || (type && normalizedType === 'all')) {
      router.replace({
        path: buildProductCategoryPath(resolvedCategory.slug),
        query: normalizedType !== 'all' ? { type: normalizedType } : {},
      });
      return;
    }

    selectedTypeSlug.value = normalizedType;
    resetFilters();
  },
  { immediate: true },
);
</script>

<template>
  <SiteChrome>
    <main class="hs-category-main">
      <div class="hs-category-shell">
        <nav class="hs-breadcrumb" aria-label="breadcrumb">
          <RouterLink to="/" class="hs-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>〉</span>
          <RouterLink :to="buildProductCategoryPath(currentCategory.slug)">{{ currentCategory.label }}</RouterLink>
          <template v-if="selectedCategoryCard">
            <span>〉</span>
            <span>{{ selectedCategoryCard.label }}</span>
          </template>
        </nav>

        <section class="hs-category-hero">
          <h1>{{ currentHeading }}</h1>
        </section>

        <section class="hs-category-tabs" :aria-label="`${currentCategory.label} 세부 분류`">
          <button
            v-for="item in categoryTabItems"
            :key="item.slug"
            class="hs-category-tab"
            :class="{ 'is-active': selectedTypeSlug === item.slug }"
            type="button"
            @click="goToCategory(item.slug)"
          >
            {{ item.label }}
          </button>
        </section>

        <div class="hs-category-layout">
          <CategoryFilterPanel
            :open-groups="openGroups"
            :price-filter-reset-key="priceFilterResetKey"
            :price-inputs="priceInputs"
            :default-price="defaultPrice"
            :price-step="priceStep"
            :price-range="priceRange"
            :price-track-style="priceTrackStyle"
            :price-min-label="priceMinLabel"
            :price-max-label="priceMaxLabel"
            :filter-groups="filterGroups"
            :filter-state="filterState"
            :format-price="formatPrice"
            @reset-filters="resetFilters"
            @toggle-group="toggleGroup"
            @update-price-input="updatePriceInput"
            @commit-price-input="commitPriceInput"
            @update-price-range="updatePriceRange"
            @toggle-filter="toggleFilter($event.groupId, $event.option)"
          />

          <CategoryProductSection
            :filtered-products="filteredProducts"
            :current-heading="currentHeading"
            :selected-sort="selectedSort"
            :selected-page-size="selectedPageSize"
            :current-page="currentPage"
            :page-count="pageCount"
            :active-filter-count="activeFilterCount"
            :is-price-changed="isPriceChanged"
            :filter-groups="filterGroups"
            :filter-state="filterState"
            :price-min-label="priceMinLabel"
            :price-max-label="priceMaxLabel"
            :displayed-products="displayedProducts"
            :format-price="formatPrice"
            :build-product-detail-path="buildProductDetailPath"
            :is-product-wishlisted="isProductWishlisted"
            @update-sort="updateSort"
            @update-page-size="updatePageSize"
            @update:current-page="updateCurrentPage"
            @clear-price-filter="clearPriceFilter"
            @clear-group-filter="clearGroupFilter"
            @toggle-wishlist="toggleProductWishlist"
          />
        </div>
      </div>
    </main>
  </SiteChrome>
</template>

<style scoped>
.hs-category-main {
  padding: 28px 0 72px;
}

.hs-category-shell {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  display: grid;
  gap: 22px;
}

.hs-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.hs-breadcrumb a,
.hs-breadcrumb span {
  color: inherit;
  text-decoration: none;
}

.hs-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.hs-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.hs-category-hero {
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 16px;
}

.hs-category-hero h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(44px, 5vw, 56px);
  line-height: 1;
  letter-spacing: -0.04em;
}

.hs-category-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hs-category-tab {
  min-width: 104px;
  height: 44px;
  padding: 0 18px;
  border: 1px solid #d9dde3;
  background: #ffffff;
  color: #111827;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.hs-category-tab.is-active {
  border-color: #0058a3;
  background: #edf4ff;
  color: #0058a3;
}

.hs-category-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 32px;
  align-items: start;
}

@media (max-width: 960px) {
  .hs-category-main {
    padding: 20px 0 56px;
  }

  .hs-category-shell {
    width: calc(100% - 24px);
    gap: 18px;
  }

  .hs-breadcrumb {
    gap: 7px;
    font-size: 12px;
  }

  .hs-category-hero {
    padding-bottom: 14px;
  }

  .hs-category-hero h1 {
    font-size: clamp(26px, 9vw, 38px);
    line-height: 1.02;
    letter-spacing: -0.05em;
    word-break: keep-all;
    overflow-wrap: anywhere;
  }

  .hs-category-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
    padding-bottom: 2px;
  }

  .hs-category-tabs::-webkit-scrollbar {
    display: none;
  }

  .hs-category-tab {
    flex: 0 0 auto;
    min-width: max-content;
    padding: 0 16px;
    white-space: nowrap;
  }

  .hs-category-layout {
    grid-template-columns: 1fr;
    gap: 22px;
  }
}

@media (max-width: 420px) {
  .hs-category-shell {
    width: calc(100% - 20px);
    gap: 16px;
  }

  .hs-category-hero h1 {
    font-size: clamp(24px, 10vw, 34px);
  }

  .hs-category-tab {
    height: 42px;
    padding: 0 14px;
    font-size: 15px;
  }
}
</style>
