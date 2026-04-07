<script setup>
import HomeProductCard from './HomeProductCard.vue';

defineProps({
  id: {
    type: String,
    default: undefined,
  },
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  filters: {
    type: Array,
    default: () => [],
  },
  activeFilterId: {
    type: String,
    default: '',
  },
  banner: {
    type: Object,
    default: null,
  },
  items: {
    type: Array,
    required: true,
  },
  moreTo: {
    type: String,
    default: '',
  },
  isProductWishlisted: {
    type: Function,
    default: () => false,
  },
});

const emit = defineEmits([
  'banner-activate',
  'filter-change',
  'product-activate',
  'toggle-wishlist',
]);

function handleBannerKeydown(event) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    emit('banner-activate');
  }
}
</script>

<template>
  <section :id="id" class="hs-section">
    <div class="hs-section__title-wrap">
      <h2>{{ title }}</h2>
      <RouterLink v-if="moreTo" :to="moreTo">더보기</RouterLink>
    </div>
    <p v-if="subtitle" class="hs-section__subtitle">{{ subtitle }}</p>
    <div v-if="filters.length" class="hs-filter-row">
      <button
        v-for="filter in filters"
        :key="filter.id"
        class="hs-filter-chip"
        :class="{ 'is-active': filter.id === activeFilterId }"
        type="button"
        @click="emit('filter-change', filter.id)"
      >
        {{ filter.label }}
      </button>
    </div>
    <article
      v-if="banner"
      class="hs-banner"
      role="button"
      tabindex="0"
      @click="emit('banner-activate')"
      @keydown="handleBannerKeydown"
    >
      <div class="hs-banner__copy">
        <strong>{{ banner.title }}</strong>
        <span>{{ banner.subtitle }}</span>
      </div>
      <img
        :src="banner.image"
        :alt="banner.title"
        :style="{ objectPosition: banner.imagePosition || 'center center' }"
        loading="lazy"
        decoding="async"
      />
    </article>
    <div class="hs-product-grid">
      <HomeProductCard
        v-for="item in items"
        :key="item.id"
        :item="item"
        :show-wishlist="true"
        :is-wishlisted="isProductWishlisted(item.productId)"
        image-loading="lazy"
        @activate="emit('product-activate', $event)"
        @toggle-wishlist="emit('toggle-wishlist', $event)"
      />
    </div>
  </section>
</template>

<style scoped>
.hs-section {
  display: grid;
  gap: 24px;
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
}

.hs-section__title-wrap {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--hs-line);
}

.hs-section__title-wrap h2 {
  margin: 0;
  color: var(--hs-ink);
  font-size: 34px;
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.hs-section__title-wrap a {
  color: var(--hs-muted);
  font-size: 14px;
  font-weight: 700;
}

.hs-section__subtitle {
  margin: -10px 0 0;
  color: var(--hs-muted);
  font-size: 15px;
  line-height: 1.65;
}

.hs-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hs-filter-chip {
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid var(--hs-line);
  border-radius: 999px;
  background: #ffffff;
  color: var(--hs-muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.hs-filter-chip.is-active {
  border-color: var(--hs-badge-dark);
  background: var(--hs-badge-dark);
  color: var(--hs-badge-dark-ink);
}

.hs-banner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border: 1px solid var(--hs-line);
  overflow: hidden;
  background: linear-gradient(135deg, #f7f9fc, #ffffff);
  cursor: pointer;
}

.hs-banner:focus-visible {
  outline: 2px solid var(--hs-blue);
  outline-offset: 2px;
}

.hs-banner__copy {
  display: grid;
  gap: 10px;
  align-content: center;
}

.hs-banner__copy strong {
  color: var(--hs-ink);
  font-size: 30px;
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.hs-banner__copy span {
  color: var(--hs-muted);
  font-size: 15px;
  line-height: 1.7;
}

.hs-banner img {
  width: 100%;
  height: 250px;
  object-fit: cover;
}

.hs-product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1180px) {
  .hs-section {
    gap: 56px;
  }

  .hs-banner {
    grid-template-columns: 1fr;
  }

  .hs-banner img {
    height: 280px;
  }

  .hs-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hs-section {
    width: calc(100% - 24px);
  }

  .hs-product-grid {
    grid-template-columns: 1fr;
  }

  .hs-section__title-wrap h2 {
    font-size: 28px;
  }
}
</style>
