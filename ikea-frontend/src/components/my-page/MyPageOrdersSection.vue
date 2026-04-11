<script setup>
import { computed } from 'vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import { ROUTE_PATHS } from '../../constants/routes';

const props = defineProps({
  orderSteps: {
    type: Array,
    required: true,
  },
  recentOrders: {
    type: Array,
    required: true,
  },
  reviewStatusMessage: {
    type: String,
    default: '',
  },
  reviewStatusTone: {
    type: String,
    default: '',
  },
  buildProductDetailPath: {
    type: Function,
    required: true,
  },
  shouldShowOrderAction: {
    type: Function,
    required: true,
  },
  isOrderActionPending: {
    type: Function,
    required: true,
  },
  getOrderActionLabel: {
    type: Function,
    required: true,
  },
  shouldShowReviewAction: {
    type: Function,
    required: true,
  },
  isReviewActionDisabled: {
    type: Function,
    required: true,
  },
  getReviewActionLabel: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits(['open-review', 'request-order-action']);

const reviewStatusClass = computed(() => ({
  'my-order-board__status--error': props.reviewStatusTone === 'error',
  'my-order-board__status--success': props.reviewStatusTone === 'success',
}));

function openReview(order) {
  emit('open-review', order);
}

function requestOrderAction(order) {
  emit('request-order-action', order);
}
</script>

<template>
  <section id="orders" class="my-section">
    <div class="my-section__action-bar">
      <RouterLink :to="ROUTE_PATHS.customerServiceQna">배송 문의 바로가기</RouterLink>
    </div>

    <div class="my-status-grid">
      <article v-for="step in orderSteps" :key="step.id" class="my-status-card">
        <strong>{{ step.count }}</strong>
        <span>{{ step.label }}</span>
      </article>
    </div>

    <p v-if="reviewStatusMessage" class="my-order-board__status" :class="reviewStatusClass">
      {{ reviewStatusMessage }}
    </p>

    <div class="my-order-board">
      <div class="my-order-board__head">
        <span>주문일</span>
        <span>상품정보</span>
        <span>상태</span>
        <span>금액</span>
        <span>관리</span>
      </div>

      <template v-if="recentOrders.length">
        <article v-for="order in recentOrders" :key="order.id" class="my-order-row">
          <span class="my-order-row__date">{{ order.date }}</span>
          <div class="my-order-row__product">
            <img :src="order.image" :alt="order.title" />
            <div>
              <strong>{{ order.title }}</strong>
              <p v-if="order.orderNumber" class="my-order-row__number">주문번호 {{ order.orderNumber }}</p>
              <p>{{ order.option }}</p>
            </div>
          </div>
          <span class="my-order-row__status">{{ order.status }}</span>
          <strong class="my-order-row__price">{{ order.price }}</strong>
          <div class="my-order-row__actions">
            <button
              v-if="shouldShowOrderAction(order)"
              type="button"
              class="my-order-row__action-button my-order-row__action-button--danger"
              :disabled="isOrderActionPending(order)"
              @click="requestOrderAction(order)"
            >
              {{ getOrderActionLabel(order) }}
            </button>
            <button
              v-if="shouldShowReviewAction(order)"
              type="button"
              class="my-order-row__action-button"
              :class="{ 'is-complete': isReviewActionDisabled(order) }"
              :disabled="isReviewActionDisabled(order)"
              @click="openReview(order)"
            >
              {{ getReviewActionLabel(order) }}
            </button>
            <RouterLink :to="buildProductDetailPath(order.productId)">상품 보기</RouterLink>
            <RouterLink :to="ROUTE_PATHS.cart">장바구니</RouterLink>
          </div>
        </article>
      </template>
      <CommonStatePanel
        v-else
        title="표시할 주문 내역이 없습니다."
        description="주문이 완료되면 최근 주문 목록이 이 영역에 표시됩니다."
        align="left"
        compact
      />
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

.my-status-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 44px;
}

.my-status-card {
  padding: 20px 18px;
  border: 1px solid var(--border-soft);
  background: var(--surface-strong);
}

.my-status-card span {
  display: block;
  color: var(--text-muted);
  font-size: 13px;
}

.my-status-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text-strong);
  font-size: 28px;
  line-height: 1.1;
}

.my-order-board {
  margin-top: 18px;
  border-top: 1px solid var(--border-strong);
}

.my-order-board__status {
  margin: 18px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.my-order-board__status--error {
  color: var(--status-danger);
}

.my-order-board__status--success {
  color: var(--status-success);
}

.my-order-board__head,
.my-order-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 110px 130px 156px;
  gap: 12px;
  align-items: center;
}

.my-order-board__head {
  min-height: 52px;
  border-bottom: 1px solid var(--border-soft);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.my-order-board__head span,
.my-order-row__date,
.my-order-row__status,
.my-order-row__price {
  display: flex;
  justify-content: center;
}

.my-order-row {
  min-height: 148px;
  border-bottom: 1px solid var(--border-muted);
}

.my-order-row__date,
.my-order-row__status {
  color: var(--text-secondary);
  font-size: 14px;
}

.my-order-row__status {
  font-weight: 700;
}

.my-order-row__product {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  min-width: 0;
}

.my-order-row__product > div {
  min-width: 0;
}

.my-order-row__product img {
  width: 96px;
  height: 96px;
  object-fit: contain;
  border: 1px solid var(--border-soft);
  background: #ffffff;
}

.my-order-row__product strong {
  display: block;
  color: var(--text-strong);
  font-size: 18px;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: break-word;
}

.my-order-row__product p {
  margin: 8px 0 0;
  color: var(--text-muted-strong);
  font-size: 13px;
  line-height: 1.6;
  word-break: keep-all;
  overflow-wrap: break-word;
}

.my-order-row__number {
  color: var(--text-strong);
  font-weight: 600;
  word-break: break-all;
}

.my-order-row__price {
  color: var(--text-strong);
  font-size: 18px;
  font-weight: 700;
}

.my-order-row__actions {
  display: grid;
  gap: 8px;
}

.my-order-row__actions a,
.my-order-row__action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: var(--action-height);
  border: 1px solid var(--border-default);
  color: var(--text-strong);
  text-decoration: none;
  font-size: 13px;
  background: var(--surface-strong);
}

.my-order-row__action-button {
  cursor: pointer;
}

.my-order-row__actions a:hover,
.my-order-row__action-button:hover {
  transform: none !important;
}

.my-order-row__action-button--danger {
  border-color: #ecc8c8;
  background: #fff7f7;
  color: #a33c3c;
}

.my-order-row__action-button.is-complete {
  background: #f6f6f6;
  color: #8a8a8a;
  cursor: default;
}

@media (max-width: 1220px) {
  .my-status-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .my-order-board__head {
    display: none;
  }

  .my-order-row {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 18px 0;
  }

  .my-order-row__product {
    grid-template-columns: 88px minmax(0, 1fr);
    gap: 14px;
    align-items: start;
  }

  .my-order-row__product img {
    width: 88px;
    height: 88px;
  }

  .my-order-row__status,
  .my-order-row__price {
    justify-content: flex-start;
  }

  .my-order-row__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .my-status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .my-section__action-bar {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .my-status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
