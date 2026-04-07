<script setup>
defineProps({
  spotlight: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['featured-activate', 'product-activate']);

function handleKeydown(event, payload, eventName) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    emit(eventName, payload);
  }
}
</script>

<template>
  <section class="hs-editorial">
    <div class="hs-section__title-wrap hs-section__title-wrap--simple">
      <h2>{{ spotlight?.title || '공간별 추천 셀렉션' }}</h2>
    </div>
    <div class="hs-editorial__grid">
      <article
        v-if="spotlight?.featured"
        class="hs-editorial__featured"
        role="button"
        tabindex="0"
        @click="emit('featured-activate', spotlight.featured)"
        @keydown="handleKeydown($event, spotlight.featured, 'featured-activate')"
      >
        <img
          :src="spotlight.featured.image"
          :alt="spotlight.featured.title"
          loading="lazy"
          decoding="async"
        />
        <div class="hs-editorial__featured-copy">
          <span>{{ spotlight.featured.label }}</span>
          <strong>{{ spotlight.featured.title }}</strong>
          <p>{{ spotlight.featured.description }}</p>
        </div>
      </article>
      <div class="hs-editorial__side">
        <article
          v-for="item in spotlight?.items ?? []"
          :key="item.id"
          class="hs-mini-product"
          :class="{ 'is-soldout': item.isSoldOut }"
          role="button"
          tabindex="0"
          @click="emit('product-activate', item)"
          @keydown="handleKeydown($event, item, 'product-activate')"
        >
          <img :src="item.image" :alt="item.title" loading="lazy" decoding="async" />
          <div class="hs-mini-product__copy">
            <span
              class="hs-mini-product__badge"
              :class="{ 'is-soldout': item.isSoldOut }"
            >
              {{ item.isSoldOut ? '품절' : item.badge }}
            </span>
            <h3>{{ item.title }}</h3>
            <p class="hs-product-card__meta">{{ item.metaText }}</p>
            <strong>{{ item.price }}</strong>
            <p v-if="item.isSoldOut" class="hs-mini-product__stock">
              품절 · 재입고 후 구매할 수 있습니다.
            </p>
            <div class="hs-mini-product__tags">
              <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hs-editorial {
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
}

.hs-section__title-wrap--simple {
  padding-bottom: 0;
  border-bottom: 0;
}

.hs-section__title-wrap h2 {
  margin: 0;
  color: var(--hs-ink);
  font-size: 34px;
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.hs-editorial__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 0.75fr);
  gap: 18px;
  align-items: stretch;
}

.hs-editorial__featured,
.hs-mini-product {
  border: 1px solid var(--hs-line);
  background: #ffffff;
}

.hs-editorial__featured {
  position: relative;
  overflow: hidden;
  min-height: 380px;
  height: 100%;
  cursor: pointer;
}

.hs-editorial__featured:focus-visible,
.hs-mini-product:focus-visible {
  outline: 2px solid var(--hs-blue);
  outline-offset: 2px;
}

.hs-editorial__featured img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hs-editorial__featured-copy {
  position: absolute;
  right: 22px;
  bottom: 22px;
  left: 22px;
  display: grid;
  gap: 10px;
  padding: 20px 22px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
}

.hs-editorial__featured-copy span {
  color: var(--hs-blue);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hs-editorial__featured-copy strong {
  color: var(--hs-ink);
  font-size: 30px;
  line-height: 1.22;
  letter-spacing: -0.03em;
}

.hs-editorial__featured-copy p {
  margin: 0;
  color: #374151;
  font-size: 15px;
  line-height: 1.7;
}

.hs-editorial__side {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: stretch;
  align-self: stretch;
  min-height: 0;
}

.hs-mini-product {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  align-items: stretch;
  gap: 14px;
  min-height: 0;
  height: 100%;
  padding: 14px;
  cursor: pointer;
}

.hs-mini-product.is-soldout {
  border-color: #ead3d3;
  background: #fff9f9;
}

.hs-mini-product img {
  width: 118px;
  height: 118px;
  object-fit: contain;
  background: #f7f9fb;
}

.hs-mini-product__copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.hs-mini-product__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--hs-badge-blue);
  color: var(--hs-badge-blue-ink);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.hs-mini-product__badge.is-soldout {
  background: var(--hs-badge-danger);
  color: var(--hs-badge-danger-ink);
}

.hs-mini-product__copy h3 {
  margin: 0;
  color: var(--hs-ink);
  font-size: 20px;
  line-height: 1.45;
  letter-spacing: -0.02em;
}

.hs-mini-product__copy strong {
  color: var(--hs-ink);
  font-size: 20px;
}

.hs-mini-product__stock {
  margin: -2px 0 0;
  color: #b42318;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.hs-product-card__meta {
  margin: 0;
  color: var(--hs-muted);
  font-size: 14px;
  line-height: 1.55;
}

.hs-mini-product__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hs-mini-product__tags span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--hs-soft);
  color: var(--hs-muted);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .hs-editorial {
    gap: 56px;
  }

  .hs-editorial__grid {
    grid-template-columns: 1fr;
  }

  .hs-editorial__side {
    grid-template-rows: none;
  }
}

@media (max-width: 720px) {
  .hs-editorial {
    width: calc(100% - 24px);
  }

  .hs-mini-product {
    grid-template-columns: 1fr;
  }

  .hs-mini-product img {
    width: 100%;
    height: 180px;
  }

  .hs-section__title-wrap h2 {
    font-size: 28px;
  }
}
</style>
