<script setup>
import { computed, shallowRef, watch } from 'vue';
import { useAdminInventory } from '../../composables/useAdminInventory';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';

const {
  adjustmentStatusMessage,
  adjustmentForm,
  filteredItems,
  inventoryLoadErrorMessage,
  isInventoryLoading,
  resolveStockStateKey,
  resolveStockStateLabel,
  safeStockForm,
  safeStockStatusMessage,
  searchKeyword,
  selectedItem,
  selectedProductId,
  selectItem,
  stockStatusCounts,
  stockStatusFilter,
  submitAdjustment,
  submitSafeStockUpdate,
  summary,
} = useAdminInventory();

const currentPage = shallowRef(1);
const pageSize = 5;

const pageCount = computed(() => Math.max(Math.ceil(filteredItems.value.length / pageSize), 1));
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredItems.value.slice(start, start + pageSize);
});

watch([searchKeyword, stockStatusFilter], () => {
  currentPage.value = 1;
});

watch(
  () => filteredItems.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);
</script>

<template>
  <section class="admin-inventory-manager">
    <div class="admin-inventory-manager__summary">
      <article>
        <span>총 재고</span>
        <strong>{{ summary.totalStock.toLocaleString('ko-KR') }}개</strong>
      </article>
      <article>
        <span>주의 상품</span>
        <strong>{{ summary.cautionCount }}개</strong>
      </article>
      <article>
        <span>품절 상품</span>
        <strong>{{ summary.soldOutCount }}개</strong>
      </article>
    </div>

    <AdminPanel title="재고 목록" description="현재 수량, 안전 재고, 상태를 빠르게 확인합니다.">
      <template #action>
        <input
          v-model="searchKeyword"
          type="text"
          class="admin-inventory-manager__search"
          placeholder="상품명 또는 SKU 검색"
        />
      </template>

      <div class="admin-inventory-manager__chips">
        <button
          v-for="option in stockStatusCounts"
          :key="option.value"
          type="button"
          class="admin-inventory-manager__chip"
          :class="{ 'is-active': stockStatusFilter === option.value }"
          @click="stockStatusFilter = option.value"
        >
          <span>{{ option.label }}</span>
          <strong>{{ option.count }}</strong>
        </button>
      </div>

      <div class="admin-inventory-manager__table">
        <div class="admin-inventory-manager__head">
          <span>상품</span>
          <span>SKU</span>
          <span>현재 수량</span>
          <span>안전 재고</span>
          <span>상태</span>
        </div>

        <button
          v-for="item in pagedItems"
          :key="item.productId"
          type="button"
          class="admin-inventory-manager__row"
          :class="{ 'is-active': selectedProductId === item.productId }"
          @click="selectItem(item.productId)"
        >
          <div class="admin-inventory-manager__product">
            <img :src="item.image" :alt="item.name" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.categoryName }}</span>
            </div>
          </div>
          <span>{{ item.sku }}</span>
          <strong>{{ item.stock.toLocaleString('ko-KR') }}개</strong>
          <span>{{ item.safeStock.toLocaleString('ko-KR') }}개</span>
          <span class="admin-inventory-manager__state" :class="`is-${resolveStockStateKey(item)}`">
            {{ resolveStockStateLabel(item) }}
          </span>
        </button>

        <CommonStatePanel
          v-if="!filteredItems.length"
          :tone="isInventoryLoading ? 'loading' : inventoryLoadErrorMessage ? 'error' : 'neutral'"
          :title="isInventoryLoading ? '재고 목록을 불러오는 중입니다.' : inventoryLoadErrorMessage ? '재고 목록을 불러오지 못했습니다.' : '선택한 조건에 맞는 재고 상품이 없습니다.'"
          :description="inventoryLoadErrorMessage"
          compact
        >
          <template v-if="inventoryLoadErrorMessage" #actions>
            <button type="button" class="admin-inventory-manager__retry" @click="loadInventoryItems">
              다시 불러오기
            </button>
          </template>
        </CommonStatePanel>
      </div>

      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>

    <AdminPanel title="재고 조정" description="입고·차감 수량과 안전재고 기준을 분리해 관리합니다.">
      <div v-if="selectedItem" class="admin-inventory-manager__detail">
        <div class="admin-inventory-manager__detail-meta">
          <strong>{{ selectedItem.name }}</strong>
          <span>{{ selectedItem.sku }}</span>
          <p>현재 수량 {{ selectedItem.stock.toLocaleString('ko-KR') }}개 · 마지막 갱신 {{ selectedItem.updatedAt }}</p>
          <p>안전재고 기준 {{ selectedItem.safeStock.toLocaleString('ko-KR') }}개</p>
        </div>

        <div class="admin-inventory-manager__detail-grid">
          <form class="admin-inventory-manager__form-card" @submit.prevent="submitAdjustment">
            <div class="admin-inventory-manager__form-heading">
              <strong>재고 조정</strong>
              <p>현재 재고를 직접 증가하거나 차감합니다.</p>
            </div>

            <div class="admin-inventory-manager__form">
              <div class="admin-inventory-manager__field">
                <label for="inventory-adjustment-type">조정 방식</label>
                <select id="inventory-adjustment-type" v-model="adjustmentForm.type">
                  <option value="increase">재고 추가</option>
                  <option value="decrease">재고 차감</option>
                </select>
              </div>

              <div class="admin-inventory-manager__field">
                <label for="inventory-adjustment-quantity">수량</label>
                <input id="inventory-adjustment-quantity" v-model.number="adjustmentForm.quantity" type="number" min="1" />
              </div>

              <div class="admin-inventory-manager__actions">
                <button type="submit">재고 반영</button>
              </div>
            </div>

            <p v-if="adjustmentStatusMessage" class="admin-inventory-manager__status">{{ adjustmentStatusMessage }}</p>
          </form>

          <form class="admin-inventory-manager__form-card" @submit.prevent="submitSafeStockUpdate">
            <div class="admin-inventory-manager__form-heading">
              <strong>안전재고 기준</strong>
              <p>주의 상품 판정 기준 수량을 별도로 저장합니다.</p>
            </div>

            <div class="admin-inventory-manager__form admin-inventory-manager__form--single">
              <div class="admin-inventory-manager__field">
                <label for="inventory-safe-stock">안전재고 수량</label>
                <input id="inventory-safe-stock" v-model.number="safeStockForm.safeStock" type="number" min="0" step="1" />
              </div>

              <div class="admin-inventory-manager__actions">
                <button type="submit">안전재고 저장</button>
              </div>
            </div>

            <p v-if="safeStockStatusMessage" class="admin-inventory-manager__status">{{ safeStockStatusMessage }}</p>
          </form>
        </div>
      </div>

      <CommonStatePanel
        v-else
        :tone="inventoryLoadErrorMessage ? 'error' : 'neutral'"
        :title="inventoryLoadErrorMessage ? '재고 상세를 표시할 수 없습니다.' : '조정할 재고 상품을 먼저 선택해 주세요.'"
        :description="inventoryLoadErrorMessage"
        align="left"
        compact
      >
        <template v-if="inventoryLoadErrorMessage" #actions>
          <button type="button" class="admin-inventory-manager__retry" @click="loadInventoryItems">
            다시 불러오기
          </button>
        </template>
      </CommonStatePanel>
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-inventory-manager {
  display: grid;
  gap: 40px;
}

.admin-inventory-manager__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.admin-inventory-manager__summary article {
  padding: 20px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-inventory-manager__summary span,
.admin-inventory-manager__detail-meta span,
.admin-inventory-manager__detail-meta p,
.admin-inventory-manager__status {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #556070;
  font-size: 14px;
  line-height: 1.6;
}

.admin-inventory-manager__summary strong,
.admin-inventory-manager__detail-meta strong {
  display: block;
  margin-top: 10px;
  color: #111111;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.admin-inventory-manager__search {
  width: min(320px, 100%);
  height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  box-sizing: border-box;
}

.admin-inventory-manager__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-inventory-manager__chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #444444;
  cursor: pointer;
}

.admin-inventory-manager__chip span,
.admin-inventory-manager__chip strong {
  font-size: 13px;
}

.admin-inventory-manager__chip strong {
  color: #111111;
}

.admin-inventory-manager__chip.is-active {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.admin-inventory-manager__chip.is-active strong {
  color: #ffffff;
}

.admin-inventory-manager__table {
  border-bottom: 1px solid #ededed;
}

.admin-inventory-manager__head,
.admin-inventory-manager__row {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) 120px 120px 120px 100px;
  gap: 16px;
  align-items: center;
}

.admin-inventory-manager__head {
  padding: 0 0 14px;
  color: #666666;
  font-size: 13px;
}

.admin-inventory-manager__row {
  width: 100%;
  padding: 16px 0;
  border: 0;
  border-top: 1px solid #efefef;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.admin-inventory-manager__row.is-active {
  background: #f7f9fb;
}

.admin-inventory-manager__empty,
.admin-inventory-manager__detail-empty {
  padding: 32px 0;
  color: #666666;
  font-size: 14px;
}

.admin-inventory-manager__product {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.admin-inventory-manager__product img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border: 1px solid #ececec;
  background: #f7f9fb;
}

.admin-inventory-manager__product strong {
  display: block;
  color: #111111;
  font-size: 15px;
  line-height: 1.4;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-inventory-manager__product span {
  display: block;
  margin-top: 6px;
  color: #7a7a7a;
  font-size: 13px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-inventory-manager__state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: auto;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
}

.admin-inventory-manager__state.is-stable {
  color: #1c3f94;
}

.admin-inventory-manager__state.is-warning {
  color: #c57a00;
}

.admin-inventory-manager__state.is-soldout {
  color: #c62828;
}

.admin-inventory-manager__detail {
  display: grid;
  gap: 18px;
}

.admin-inventory-manager__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-inventory-manager__detail-meta {
  padding: 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-inventory-manager__detail-meta p {
  margin: 10px 0 0;
}

.admin-inventory-manager__form-card {
  display: grid;
  gap: 18px;
  padding: 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-inventory-manager__form-heading {
  display: grid;
  gap: 8px;
}

.admin-inventory-manager__form-heading strong {
  color: #111111;
  font-size: 18px;
}

.admin-inventory-manager__form-heading p {
  margin: 0;
  color: #666666;
  font-size: 13px;
  line-height: 1.5;
}

.admin-inventory-manager__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-inventory-manager__form--single {
  grid-template-columns: 1fr;
}

.admin-inventory-manager__field {
  display: grid;
  gap: 8px;
}

.admin-inventory-manager__field label {
  color: #555555;
  font-size: 13px;
}

.admin-inventory-manager__field input,
.admin-inventory-manager__field select {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
}

.admin-inventory-manager__actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
}

.admin-inventory-manager__actions button {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  cursor: pointer;
}

.admin-inventory-manager__retry {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .admin-inventory-manager__summary,
  .admin-inventory-manager__detail-grid,
  .admin-inventory-manager__head,
  .admin-inventory-manager__row,
  .admin-inventory-manager__form {
    grid-template-columns: 1fr;
  }

  .admin-inventory-manager__head {
    display: none;
  }

  .admin-inventory-manager__row {
    gap: 8px;
    align-items: start;
  }
}

@media (max-width: 720px) {
  .admin-inventory-manager__search {
    width: 100%;
  }

  .admin-inventory-manager__product {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
  }

  .admin-inventory-manager__product img {
    width: 64px;
    height: 64px;
  }

  .admin-inventory-manager__actions {
    justify-content: stretch;
  }

  .admin-inventory-manager__actions button {
    width: 100%;
  }
}
</style>
