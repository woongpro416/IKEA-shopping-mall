<script setup>
import AdminPagination from '../admin/AdminPagination.vue';
import WishlistToggleButton from '../common/WishlistToggleButton.vue';

defineProps({
  filteredProducts: {
    type: Array,
    required: true,
  },
  currentHeading: {
    type: String,
    required: true,
  },
  selectedSort: {
    type: String,
    required: true,
  },
  selectedPageSize: {
    type: Number,
    required: true,
  },
  currentPage: {
    type: Number,
    required: true,
  },
  pageCount: {
    type: Number,
    required: true,
  },
  activeFilterCount: {
    type: Number,
    required: true,
  },
  isPriceChanged: {
    type: Boolean,
    required: true,
  },
  filterGroups: {
    type: Array,
    required: true,
  },
  filterState: {
    type: Object,
    required: true,
  },
  priceMinLabel: {
    type: String,
    required: true,
  },
  priceMaxLabel: {
    type: String,
    required: true,
  },
  displayedProducts: {
    type: Array,
    required: true,
  },
  formatPrice: {
    type: Function,
    required: true,
  },
  buildProductDetailPath: {
    type: Function,
    required: true,
  },
  isProductWishlisted: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits([
  'update-sort',
  'update-page-size',
  'update:currentPage',
  'clear-price-filter',
  'clear-group-filter',
  'toggle-wishlist',
]);

function updateSort(value) {
  emit('update-sort', value);
}

function updatePageSize(value) {
  emit('update-page-size', value);
}

function updateCurrentPage(value) {
  emit('update:currentPage', value);
}

function clearPriceFilter() {
  emit('clear-price-filter');
}

function clearGroupFilter(groupId) {
  emit('clear-group-filter', groupId);
}

function toggleWishlist(item) {
  emit('toggle-wishlist', item);
}
</script>

<template>
  <section class="hs-product-section">
    <div class="hs-toolbar">
      <div class="hs-result-summary">
        <strong>{{ filteredProducts.length }}개 상품</strong>
        <span>{{ currentHeading }} · {{ selectedSort }}</span>
      </div>

      <div class="hs-toolbar__right">
        <select :value="selectedSort" aria-label="정렬 기준 선택" @change="updateSort($event.target.value)">
          <option>인기순</option>
          <option>낮은가격순</option>
          <option>높은가격순</option>
          <option>리뷰많은순</option>
          <option>할인율순</option>
        </select>
        <select :value="selectedPageSize" aria-label="페이지 크기 선택" @change="updatePageSize($event.target.value)">
          <option :value="10">10개씩 보기</option>
          <option :value="20">20개씩 보기</option>
          <option :value="30">30개씩 보기</option>
        </select>
      </div>
    </div>

    <div v-if="activeFilterCount" class="hs-active-filters">
      <span>선택한 필터</span>
      <button v-if="isPriceChanged" class="hs-active-filter-pill" type="button" @click="clearPriceFilter">
        가격 · {{ priceMinLabel }} ~ {{ priceMaxLabel }}
      </button>
      <button
        v-for="group in filterGroups"
        :key="group.id"
        v-show="filterState[group.id].length"
        class="hs-active-filter-pill"
        type="button"
        @click="clearGroupFilter(group.id)"
      >
        {{ group.title }} · {{ filterState[group.id].join(', ') }}
      </button>
    </div>

    <div class="hs-product-grid">
      <article
        v-for="item in displayedProducts"
        :key="item.id"
        class="hs-product-card"
        :class="{ 'is-soldout': item.isSoldOut }"
      >
        <RouterLink :to="buildProductDetailPath(item.id)" class="hs-product-card__link" :aria-label="`${item.name} 상세 페이지로 이동`" />
        <div class="hs-product-card__image-wrap">
          <img :src="item.image" :alt="item.imageAlt ?? item.name" />
          <span v-if="item.isSoldOut" class="hs-product-card__badge hs-product-card__badge--soldout">품절</span>
          <span v-else-if="item.badge" class="hs-product-card__badge">{{ item.badge }}</span>
          <WishlistToggleButton
            class="hs-product-card__wishlist"
            :active="isProductWishlisted(item.id)"
            @toggle="toggleWishlist(item)"
          />
        </div>

        <div class="hs-product-card__copy">
          <p class="hs-product-card__brand">{{ item.brand }}</p>
          <h3>{{ item.name }}</h3>
          <div class="hs-product-price-block">
            <span
              v-if="Number(item.discountRate ?? 0) > 0 && Number(item.originalPrice ?? 0) > Number(item.price ?? 0)"
              class="hs-price__original hs-price__original--top"
            >
              {{ formatPrice(item.originalPrice) }}
            </span>
            <div class="hs-price">
              <span v-if="item.discountRate" class="hs-price__discount">{{ item.discountRate }}%</span>
              <strong>{{ formatPrice(item.price) }}</strong>
            </div>
          </div>
          <div v-if="item.rating !== null || item.reviews !== null" class="hs-product-meta">
            <span v-if="item.rating !== null">★ {{ item.rating }}</span>
            <span v-if="item.reviews !== null">후기 {{ Number(item.reviews ?? 0).toLocaleString('ko-KR') }}</span>
          </div>
          <p v-if="item.isSoldOut" class="hs-product-stock">품절 · 상세 페이지에서 재입고 여부를 확인해 주세요.</p>
          <div v-if="item.features?.length" class="hs-product-tags">
            <span v-for="tag in item.features" :key="tag">{{ tag }}</span>
          </div>
        </div>
      </article>
    </div>

    <div v-if="!displayedProducts.length" class="hs-empty-state">
      선택한 조건에 맞는 상품이 없습니다.
    </div>

    <AdminPagination
      :current-page="currentPage"
      :page-count="pageCount"
      @update:current-page="updateCurrentPage"
    />
  </section>
</template>

<style scoped>
.hs-active-filters,
.hs-product-tags,
.hs-product-meta,
.hs-toolbar,
.hs-toolbar__right {
  display: flex;
  align-items: center;
}

.hs-active-filters,
.hs-product-tags,
.hs-product-meta,
.hs-toolbar,
.hs-toolbar__right {
  flex-wrap: wrap;
}

.hs-active-filters,
.hs-product-tags,
.hs-product-meta,
.hs-toolbar__right {
  gap: 8px;
}

.hs-active-filter-pill {
  border: 1px solid var(--border-default);
  background: #edf4ff;
  border-color: var(--accent);
  color: var(--accent);
  border-radius: var(--radius-pill);
  padding: 10px 14px;
  font-size: 14px;
  cursor: pointer;
}

.hs-product-section {
  display: grid;
  gap: 18px;
}

.hs-toolbar {
  justify-content: space-between;
  gap: 16px;
}

.hs-result-summary {
  display: grid;
  gap: 4px;
}

.hs-result-summary strong {
  font-size: 18px;
}

.hs-result-summary span,
.hs-active-filters > span {
  color: #6b7280;
  font-size: 14px;
}

.hs-toolbar__right select {
  height: 42px;
  min-width: 140px;
  border-radius: 10px;
  border: 1px solid var(--border-default);
  padding: 0 14px;
  background: var(--surface-strong);
  font: inherit;
}

.hs-product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.hs-product-card {
  position: relative;
  display: grid;
  gap: 14px;
}

.hs-product-card.is-soldout {
  opacity: 0.78;
}

.hs-product-card__link {
  position: absolute;
  inset: 0;
  display: block;
  z-index: 1;
  border-radius: 18px;
  cursor: pointer;
}

.hs-product-card__link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 4px;
}

.hs-product-card__image-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  background: var(--surface-soft);
}

.hs-product-card__image-wrap img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: contain;
  object-position: center;
}

.hs-product-card__badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 7px 10px;
  border-radius: var(--radius-pill);
  background: var(--hs-badge-dark);
  color: var(--hs-badge-dark-ink);
  font-size: 12px;
  font-weight: 700;
}

.hs-product-card__wishlist {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 2;
}

.hs-product-card__badge--soldout {
  background: var(--status-danger);
}

.hs-product-card__copy {
  position: relative;
  z-index: 0;
  display: grid;
  gap: 8px;
}

.hs-product-card__brand {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.hs-product-card__copy h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  line-height: 1.45;
}

.hs-product-price-block {
  display: grid;
  gap: 4px;
}

.hs-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.hs-price__discount {
  color: #e11d48;
  font-size: 20px;
  font-weight: 800;
}

.hs-price strong {
  font-size: 24px;
}

.hs-price__original,
.hs-product-meta,
.hs-product-tags span,
.hs-empty-state {
  color: #6b7280;
}

.hs-product-stock {
  margin: 0;
  color: #b42318;
  font-size: 13px;
  font-weight: 700;
}

.hs-price__original {
  font-size: 14px;
  text-decoration: line-through;
}

.hs-price__original--top {
  display: inline-block;
  line-height: 1.2;
}

.hs-product-tags span {
  padding: 8px 10px;
  border-radius: 999px;
  background: #f5f7fa;
  font-size: 12px;
}

.hs-empty-state {
  display: grid;
  place-items: center;
  min-height: 260px;
  border: 1px dashed #cfd8e3;
  border-radius: 18px;
}

@media (max-width: 1180px) {
  .hs-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hs-product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
