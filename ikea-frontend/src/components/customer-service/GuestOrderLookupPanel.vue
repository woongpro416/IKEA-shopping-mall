<script setup>
import { computed } from 'vue';
import { useGuestOrderLookup } from '../../composables/useGuestOrderLookup';

const {
  canSubmit,
  form,
  isSubmitting,
  searchedOrders,
  statusMessage,
  statusTone,
  submitLookup,
} = useGuestOrderLookup();

const statusClass = computed(() => ({
  'guest-order__status--error': statusTone.value === 'error',
  'guest-order__status--success': statusTone.value === 'success',
}));
</script>

<template>
  <section class="guest-order">
    <div class="guest-order__intro">
      <p>주문을 진행한 현재 브라우저에서 이름과 주문번호 또는 휴대전화번호를 입력하면 주문 상태를 확인할 수 있습니다. 주문번호에는 밑줄(_)이 포함될 수 있습니다.</p>
    </div>

    <form class="guest-order__form" @submit.prevent="submitLookup">
      <div class="guest-order__row">
        <label for="guest-order-name">이름</label>
        <input id="guest-order-name" v-model.trim="form.buyerName" type="text" maxlength="30" />
      </div>

      <div class="guest-order__row">
        <span>조회 방식</span>
        <div class="guest-order__radios">
          <label>
            <input v-model="form.inquiryType" type="radio" value="order" />
            <span>주문번호</span>
          </label>
          <label>
            <input v-model="form.inquiryType" type="radio" value="phone" />
            <span>휴대전화번호</span>
          </label>
        </div>
      </div>

      <div v-if="form.inquiryType === 'order'" class="guest-order__row">
        <label for="guest-order-number">주문번호</label>
        <input
          id="guest-order-number"
          v-model.trim="form.orderNumber"
          type="text"
          maxlength="30"
          placeholder="예: ORDER_20260406_ab12cd34"
        />
      </div>

      <div v-else class="guest-order__row guest-order__row--phone">
        <label for="guest-order-phone">휴대전화번호</label>
        <div class="guest-order__phone">
          <input
            id="guest-order-phone"
            v-model.trim="form.phoneNumber"
            type="text"
            maxlength="20"
            placeholder="010-0000-0000"
          />
        </div>
      </div>

      <p v-if="statusMessage" class="guest-order__status" :class="statusClass">{{ statusMessage }}</p>

      <div class="guest-order__actions">
        <button type="submit" class="guest-order__primary" :disabled="!canSubmit || isSubmitting">
          {{ isSubmitting ? '조회 중...' : '조회' }}
        </button>
      </div>
    </form>

    <section class="guest-order__result">
      <header>
        <h3>조회 결과</h3>
      </header>

      <div v-if="searchedOrders.length" class="guest-order__cards">
        <article v-for="order in searchedOrders" :key="order.orderNumber" class="guest-order__card">
          <div class="guest-order__meta">
            <strong>{{ order.orderNumber }}</strong>
            <span>{{ order.orderedAt }}</span>
          </div>
          <div class="guest-order__summary">
            <span>{{ order.statusLabel }}</span>
            <span>{{ order.paymentMethodLabel }}</span>
            <strong>{{ Number(order.finalTotal ?? 0).toLocaleString('ko-KR') }}원</strong>
          </div>
          <ul>
            <li v-for="item in order.orderItems" :key="`${order.orderNumber}-${item.name}`">
              {{ item.name }} x {{ item.quantity }}개
            </li>
          </ul>
        </article>
      </div>
      <div v-else class="guest-order__empty">
        현재 브라우저에 저장된 주문이 없으면 이름과 주문번호, 또는 이름과 휴대전화번호를 다시 확인해 주세요.
      </div>
    </section>
  </section>
</template>

<style scoped>
.guest-order {
  display: grid;
  gap: 22px;
}

.guest-order__intro,
.guest-order__result {
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.guest-order__intro {
  padding: 16px 18px;
  background: #fafafa;
}

.guest-order__intro p,
.guest-order__status,
.guest-order__empty {
  margin: 0;
  color: #666666;
  font-size: 14px;
  line-height: 1.7;
}

.guest-order__form {
  display: grid;
  gap: 14px;
}

.guest-order__row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
}

.guest-order__row > label,
.guest-order__row > span {
  color: #555555;
  font-size: 14px;
  font-weight: 600;
}

.guest-order__row input {
  width: 100%;
  height: 48px;
  min-height: 48px;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  font: inherit;
  box-sizing: border-box;
}

.guest-order__radios,
.guest-order__phone {
  display: flex;
  align-items: center;
  gap: 14px;
}

.guest-order__radios label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  color: #444444;
  white-space: nowrap;
}

.guest-order__radios input {
  width: 16px;
  height: 16px;
  margin: 0;
  flex: 0 0 auto;
}

.guest-order__radios span {
  white-space: nowrap;
  word-break: keep-all;
}

.guest-order__phone input {
  flex: 1;
}

.guest-order__status--error {
  color: #c62828;
}

.guest-order__status--success {
  color: #0f6b3b;
}

.guest-order__primary {
  height: 48px;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  font-size: 14px;
  box-sizing: border-box;
  cursor: pointer;
  white-space: nowrap;
}

.guest-order__primary:disabled {
  cursor: default;
  opacity: 0.45;
}

.guest-order__actions {
  display: flex;
  justify-content: flex-end;
}

.guest-order__result {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.guest-order__result h3 {
  margin: 0;
  color: #111111;
  font-size: 18px;
}

.guest-order__cards {
  display: grid;
  gap: 14px;
}

.guest-order__card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid #eceff3;
}

.guest-order__meta,
.guest-order__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.guest-order__meta strong,
.guest-order__summary strong {
  color: #111111;
  font-size: 16px;
  font-weight: 700;
}

.guest-order__meta span,
.guest-order__summary span,
.guest-order__card li {
  color: #666666;
  font-size: 14px;
}

.guest-order__card ul {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 720px) {
  .guest-order__row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .guest-order__phone,
  .guest-order__meta,
  .guest-order__summary {
    flex-direction: column;
    align-items: stretch;
  }

  .guest-order__actions {
    justify-content: stretch;
  }
}
</style>
