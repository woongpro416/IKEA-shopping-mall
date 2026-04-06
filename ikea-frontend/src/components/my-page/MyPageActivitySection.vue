<script setup>
import CommonStatePanel from '../common/CommonStatePanel.vue';
import { ROUTE_PATHS } from '../../constants/routes';

defineProps({
  wishListItems: {
    type: Array,
    required: true,
  },
  recentViewItems: {
    type: Array,
    required: true,
  },
  buildProductDetailPath: {
    type: Function,
    required: true,
  },
});
</script>

<template>
  <section id="activity" class="my-section">
    <div class="my-section__action-bar">
      <RouterLink :to="ROUTE_PATHS.home">계속 둘러보기</RouterLink>
    </div>

    <div class="my-panel-grid">
      <section class="my-panel">
        <header class="my-panel__head">
          <strong>찜한 상품</strong>
        </header>
        <div v-if="wishListItems.length" class="my-product-list">
          <RouterLink
            v-for="item in wishListItems"
            :key="item.id"
            :to="buildProductDetailPath(item.productId)"
            class="my-product-row"
          >
            <img :src="item.image" :alt="item.title" />
            <div class="my-product-row__copy">
              <span>{{ item.brand }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.subtitle }}</p>
            </div>
            <b>{{ item.price }}</b>
          </RouterLink>
        </div>
        <CommonStatePanel
          v-else
          title="찜한 상품이 없습니다."
          description="관심 상품을 저장하면 이 영역에서 다시 확인할 수 있습니다."
          align="left"
          compact
        />
      </section>

      <section class="my-panel">
        <header class="my-panel__head">
          <strong>최근 본 상품</strong>
        </header>
        <div v-if="recentViewItems.length" class="my-product-list">
          <RouterLink
            v-for="item in recentViewItems"
            :key="item.id"
            :to="item.to"
            class="my-product-row"
          >
            <img :src="item.image" :alt="item.title" />
            <div class="my-product-row__copy">
              <span>{{ item.brand }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.subtitle }}</p>
            </div>
            <b>{{ item.price }}</b>
          </RouterLink>
        </div>
        <CommonStatePanel
          v-else
          title="최근 본 상품이 없습니다."
          description="둘러본 상품이 생기면 최근 본 상품 목록에 자동으로 표시됩니다."
          align="left"
          compact
        />
      </section>
    </div>
  </section>
</template>

<style scoped>
.my-section {
  margin-top: 40px;
}

.my-section__action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-bottom: 18px;
}

.my-section__action-bar a {
  color: var(--text-strong);
  font-size: 14px;
  text-decoration: none;
}

.my-panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 44px;
  align-items: start;
}

.my-panel {
  border-top: 1px solid var(--border-strong);
}

.my-panel__head {
  padding: 18px 0 16px;
  border-bottom: 1px solid var(--border-soft);
}

.my-panel__head strong {
  display: block;
  color: var(--text-strong);
  font-size: 20px;
}

.my-product-list {
  display: grid;
}

.my-product-row {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr) 132px;
  gap: 24px;
  align-items: center;
  min-height: 122px;
  border-bottom: 1px solid var(--border-muted);
  color: var(--text-strong);
  text-decoration: none;
}

.my-product-row:last-child {
  border-bottom: 0;
}

.my-product-row img {
  width: 108px;
  height: 108px;
  object-fit: contain;
  border: 1px solid var(--border-soft);
  background: #ffffff;
}

.my-product-row__copy {
  min-width: 0;
}

.my-product-row__copy span {
  color: var(--text-faint);
  font-size: 12px;
}

.my-product-row__copy strong {
  display: block;
  margin-top: 8px;
  color: var(--text-strong);
  font-size: 17px;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.my-product-row__copy p {
  margin: 8px 0 0;
  color: var(--text-muted-strong);
  font-size: 13px;
  line-height: 1.5;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.my-product-row b {
  color: var(--text-strong);
  font-size: 17px;
  justify-self: end;
  text-align: right;
}

@media (max-width: 1080px) {
  .my-panel-grid {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .my-product-row {
    grid-template-columns: 96px minmax(0, 1fr);
    align-items: start;
    padding: 16px 0;
  }

  .my-product-row img {
    width: 96px;
    height: 96px;
  }

  .my-product-row b {
    grid-column: 2;
    justify-self: start;
    text-align: left;
  }
}

@media (max-width: 720px) {
  .my-section__action-bar {
    justify-content: flex-start;
  }

  .my-panel-grid {
    grid-template-columns: 1fr;
  }

  .my-product-row {
    grid-template-columns: 88px minmax(0, 1fr);
    gap: 14px;
    align-items: start;
    padding: 16px 0;
  }

  .my-product-row img {
    width: 88px;
    height: 88px;
  }

  .my-product-row b {
    grid-column: 2;
    justify-self: start;
    text-align: left;
  }
}
</style>
