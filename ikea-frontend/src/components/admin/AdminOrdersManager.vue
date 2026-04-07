<script setup>
import { computed, onMounted, shallowRef, watch } from 'vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import {
  ADMIN_ORDER_FILTER_OPTIONS,
  getAdminOrderStatusLabel,
  getNextAdminOrderStatuses,
} from '../../constants/adminOrderConfig';
import {
  getAdminOrders,
  getAdminPayments,
  updateAdminOrderStatus,
} from '../../services/adminService';
import {
  formatAdminCurrency,
  formatAdminDateTime,
  mergeAdminOrdersWithPayments,
  normalizeAdminOrder,
  normalizeArrayPayload,
  resolveAdminOrderPaymentLabel,
  resolveAdminOrderPaymentStatusLabel,
  resolveAdminOrderPaymentSummary,
} from '../../mappers/adminManagementMapper';
import { resolveAdminActionErrorMessage } from '../../utils/apiErrorMessage';

const STATUS_OPTIONS = ADMIN_ORDER_FILTER_OPTIONS;

const allOrders = shallowRef([]);
const selectedOrderId = shallowRef('');
const statusFilter = shallowRef('ALL');
const statusDraft = shallowRef('');
const statusMessage = shallowRef('');
const loadErrorMessage = shallowRef('');
const loadNoticeMessage = shallowRef('');
const isLoading = shallowRef(false);
const isSubmitting = shallowRef(false);
const currentPage = shallowRef(1);
const pageSize = 5;

const orderCounts = computed(() => STATUS_OPTIONS.map((option) => ({
  ...option,
  count: option.value === 'ALL'
    ? allOrders.value.length
    : allOrders.value.filter((order) => order.orderStatus === option.value).length,
})));

const filteredOrders = computed(() => {
  if (statusFilter.value === 'ALL') {
    return allOrders.value;
  }

  return allOrders.value.filter((order) => order.orderStatus === statusFilter.value);
});

const pageCount = computed(() => Math.max(Math.ceil(filteredOrders.value.length / pageSize), 1));
const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredOrders.value.slice(start, start + pageSize);
});

const selectedOrder = computed(
  () => allOrders.value.find((order) => order.orderId === selectedOrderId.value) ?? null,
);

const availableStatusOptions = computed(() => getNextAdminOrderStatuses(selectedOrder.value?.orderStatus)
  .map((value) => ({
    value,
    label: formatStatusLabel(value),
  })));

const canSubmitStatusChange = computed(() => (
  Boolean(selectedOrder.value)
  && Boolean(statusDraft.value)
  && availableStatusOptions.value.some((option) => option.value === statusDraft.value)
));

const statusEditorHint = computed(() => {
  if (!selectedOrder.value) {
    return '';
  }

  if (!availableStatusOptions.value.length) {
    return '현재 상태에서는 더 이상 주문 단계를 변경할 수 없습니다.';
  }

  return `${formatStatusLabel(selectedOrder.value.orderStatus)} 다음 단계만 선택할 수 있습니다.`;
});

function formatStatusLabel(status) {
  return getAdminOrderStatusLabel(status);
}

function formatPaymentLabel(order) {
  return resolveAdminOrderPaymentLabel(order);
}

function formatPaymentStatusLabel(order) {
  return resolveAdminOrderPaymentStatusLabel(order);
}

function formatPaymentSummary(order) {
  return resolveAdminOrderPaymentSummary(order);
}

function createOrderItemKey(orderId, item, index) {
  return `${orderId}-${item.productId ?? item.productName ?? item.name ?? index}`;
}

function applyOrders(items, payments = []) {
  const normalizedOrders = items
    .map((item) => normalizeAdminOrder(item))
    .filter((item) => item.orderId);

  allOrders.value = mergeAdminOrdersWithPayments(normalizedOrders, payments);

  const hasSelectedOrder = allOrders.value.some((order) => order.orderId === selectedOrderId.value);

  if (!hasSelectedOrder) {
    selectedOrderId.value = allOrders.value[0]?.orderId ?? '';
  }
}

async function loadOrders() {
  isLoading.value = true;
  loadErrorMessage.value = '';
  loadNoticeMessage.value = '';

  try {
    const [ordersResult, paymentsResult] = await Promise.allSettled([
      getAdminOrders(),
      getAdminPayments(),
    ]);

    if (ordersResult.status !== 'fulfilled') {
      applyOrders([]);
      loadErrorMessage.value = resolveAdminActionErrorMessage(
        ordersResult.reason,
        '주문 목록을 불러오지 못했습니다.',
      );
      return false;
    }

    const orderItems = normalizeArrayPayload(ordersResult.value, []);
    const paymentItems = paymentsResult.status === 'fulfilled'
      ? normalizeArrayPayload(paymentsResult.value, [])
      : [];

    applyOrders(orderItems, paymentItems);

    if (paymentsResult.status !== 'fulfilled') {
      loadNoticeMessage.value = resolveAdminActionErrorMessage(
        paymentsResult.reason,
        '결제수단 정보는 일부 표시되지 않을 수 있습니다.',
      );
    }
  } catch (error) {
    applyOrders([]);
    loadErrorMessage.value = resolveAdminActionErrorMessage(error, '주문 목록을 불러오지 못했습니다.');
    return false;
  } finally {
    isLoading.value = false;
  }

  return true;
}

function selectOrder(order) {
  selectedOrderId.value = order.orderId;
  statusMessage.value = '';
}

async function submitStatusChange() {
  if (!selectedOrder.value) {
    return;
  }

  const orderId = selectedOrder.value.orderId;
  isSubmitting.value = true;
  statusMessage.value = '';

  try {
    await updateAdminOrderStatus(orderId, statusDraft.value);
    const didLoadFromServer = await loadOrders();
    statusMessage.value = didLoadFromServer
      ? '주문 상태를 저장했습니다.'
      : '주문 상태는 저장됐지만 목록 재조회는 실패했습니다.';
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '주문 상태 변경에 실패했습니다.');
  }

  isSubmitting.value = false;
}

watch(selectedOrder, (order) => {
  const nextStatuses = getNextAdminOrderStatuses(order?.orderStatus);
  statusDraft.value = nextStatuses[0] ?? order?.orderStatus ?? '';
}, { immediate: true });

watch(statusFilter, () => {
  currentPage.value = 1;
});

watch(
  () => filteredOrders.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);

onMounted(loadOrders);
</script>

<template>
  <section class="admin-orders-manager">
    <AdminPanel title="주문 상태 관리" description="주문 현황을 확인하고 다음 단계로 상태를 변경합니다.">
      <div class="admin-orders-manager__chips">
        <button
          v-for="option in orderCounts"
          :key="option.value"
          type="button"
          class="admin-orders-manager__chip"
          :class="{ 'is-active': statusFilter === option.value }"
          @click="statusFilter = option.value"
        >
          <span>{{ option.label }}</span>
          <strong>{{ option.count }}</strong>
        </button>
      </div>

      <p v-if="loadNoticeMessage" class="admin-orders-manager__notice">{{ loadNoticeMessage }}</p>

      <div class="admin-orders-manager__table">
        <div class="admin-orders-manager__head">
          <span>주문번호</span>
          <span>주문상품</span>
          <span>결제수단</span>
          <span>상태</span>
          <span>결제금액</span>
          <span>주문시각</span>
        </div>

        <button
          v-for="order in pagedOrders"
          :key="order.orderId"
          type="button"
          class="admin-orders-manager__row"
          :class="{ 'is-active': selectedOrderId === order.orderId }"
          @click="selectOrder(order)"
        >
          <strong>#{{ order.orderNo || order.orderId }}</strong>
          <span>{{ order.orderItems?.[0]?.productName ?? order.orderItems?.[0]?.name ?? '-' }}</span>
          <span>{{ formatPaymentSummary(order) }}</span>
          <span>{{ formatStatusLabel(order.orderStatus) }}</span>
          <strong>{{ formatAdminCurrency(order.totalPrice) }}</strong>
          <span>{{ order.createdAtDisplay || formatAdminDateTime(order.createdAt) }}</span>
        </button>

        <CommonStatePanel
          v-if="!filteredOrders.length"
          :tone="isLoading ? 'loading' : loadErrorMessage ? 'error' : 'neutral'"
          :title="isLoading ? '주문 목록을 불러오는 중입니다.' : loadErrorMessage ? '주문 목록을 불러오지 못했습니다.' : '선택한 상태의 주문이 없습니다.'"
          :description="loadErrorMessage"
          compact
        />
      </div>

      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>

    <AdminPanel title="선택 주문 상세" description="선택한 주문의 배송지와 주문상품을 확인합니다.">
      <div v-if="selectedOrder" class="admin-orders-manager__detail">
        <div class="admin-orders-manager__summary">
          <article>
            <span>주문번호</span>
            <strong>#{{ selectedOrder.orderNo || selectedOrder.orderId }}</strong>
          </article>
          <article>
            <span>주문 시각</span>
            <strong>{{ selectedOrder.createdAtDisplay || formatAdminDateTime(selectedOrder.createdAt) }}</strong>
          </article>
          <article>
            <span>결제수단</span>
            <strong>{{ formatPaymentLabel(selectedOrder) }}</strong>
          </article>
          <article>
            <span>결제상태</span>
            <strong>{{ formatPaymentStatusLabel(selectedOrder) }}</strong>
          </article>
          <article>
            <span>현재 상태</span>
            <strong>{{ formatStatusLabel(selectedOrder.orderStatus) }}</strong>
          </article>
          <article>
            <span>결제금액</span>
            <strong>{{ formatAdminCurrency(selectedOrder.totalPrice) }}</strong>
          </article>
        </div>

        <article class="admin-orders-manager__address">
          <span>배송지</span>
          <strong>{{ selectedOrder.address || '-' }}</strong>
        </article>

        <article class="admin-orders-manager__items">
          <span>주문상품</span>
          <ul>
            <li
              v-for="(item, index) in selectedOrder.orderItems"
              :key="createOrderItemKey(selectedOrder.orderId, item, index)"
            >
              <strong>{{ item.productName ?? item.name ?? '-' }}</strong>
              <span>{{ item.quantity ?? 1 }}개</span>
            </li>
          </ul>
        </article>

        <div class="admin-orders-manager__editor">
          <label>
            <span>다음 상태</span>
            <select v-model="statusDraft" :disabled="!availableStatusOptions.length || isSubmitting">
              <option
                v-for="option in availableStatusOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
              <option v-if="!availableStatusOptions.length" :value="selectedOrder.orderStatus">
                변경 가능 상태 없음
              </option>
            </select>
          </label>

          <button
            type="button"
            class="admin-orders-manager__primary"
            :disabled="isSubmitting || !canSubmitStatusChange"
            @click="submitStatusChange"
          >
            {{ isSubmitting ? '저장 중...' : '상태 저장' }}
          </button>
        </div>

        <p v-if="statusEditorHint" class="admin-orders-manager__status">{{ statusEditorHint }}</p>
        <p v-if="statusMessage" class="admin-orders-manager__status">{{ statusMessage }}</p>
      </div>

      <CommonStatePanel
        v-else
        :tone="loadErrorMessage ? 'error' : 'neutral'"
        :title="loadErrorMessage ? '주문 상세를 표시할 수 없습니다.' : '주문을 선택하면 상세 정보가 표시됩니다.'"
        :description="loadErrorMessage"
        align="left"
        compact
      />
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-orders-manager {
  display: grid;
  gap: 40px;
}

.admin-orders-manager__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-orders-manager__chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  cursor: pointer;
}

.admin-orders-manager__chip strong {
  font-size: 14px;
}

.admin-orders-manager__chip.is-active {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.admin-orders-manager__notice {
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
  font-size: 14px;
  line-height: 1.6;
}

.admin-orders-manager__table {
  border-bottom: 1px solid #ededed;
}

.admin-orders-manager__head,
.admin-orders-manager__row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 120px 100px 130px 160px;
  gap: 16px;
  align-items: center;
}

.admin-orders-manager__head {
  padding: 0 0 14px;
  color: #666666;
  font-size: 13px;
}

.admin-orders-manager__row {
  width: 100%;
  padding: 16px 0;
  border: 0;
  border-top: 1px solid #efefef;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.admin-orders-manager__row.is-active {
  background: #f7f9fb;
}

.admin-orders-manager__row strong {
  color: #111111;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-orders-manager__row span {
  color: #444444;
  font-size: 14px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-orders-manager__detail {
  display: grid;
  gap: 18px;
}

.admin-orders-manager__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.admin-orders-manager__summary article,
.admin-orders-manager__address,
.admin-orders-manager__items {
  padding: 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-orders-manager__summary span,
.admin-orders-manager__address span,
.admin-orders-manager__items span {
  display: block;
  color: #777777;
  font-size: 13px;
}

.admin-orders-manager__summary strong,
.admin-orders-manager__address strong,
.admin-orders-manager__items strong {
  display: block;
  margin-top: 10px;
  color: #111111;
  font-size: 18px;
  line-height: 1.35;
}

.admin-orders-manager__items ul {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.admin-orders-manager__items li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.admin-orders-manager__items li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.admin-orders-manager__items li strong {
  margin-top: 0;
  font-size: 15px;
}

.admin-orders-manager__items li span {
  margin-top: 0;
}

.admin-orders-manager__editor {
  display: flex;
  align-items: end;
  justify-content: flex-end;
  gap: 14px;
}

.admin-orders-manager__editor label {
  display: grid;
  gap: 8px;
  min-width: 220px;
}

.admin-orders-manager__editor label span {
  color: #555555;
  font-size: 13px;
}

.admin-orders-manager__editor select {
  height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
}

.admin-orders-manager__primary {
  min-height: 44px;
  padding: 0 20px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  cursor: pointer;
}

.admin-orders-manager__primary:disabled {
  opacity: 0.6;
  cursor: default;
}

.admin-orders-manager__status,
.admin-orders-manager__empty {
  color: #666666;
  font-size: 14px;
  line-height: 1.6;
}

.admin-orders-manager__status {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
}

@media (max-width: 1024px) {
  .admin-orders-manager__head,
  .admin-orders-manager__row {
    grid-template-columns: 1fr;
  }

  .admin-orders-manager__head {
    display: none;
  }

  .admin-orders-manager__row {
    gap: 8px;
    align-items: start;
  }

  .admin-orders-manager__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-orders-manager__summary {
    grid-template-columns: 1fr;
  }

  .admin-orders-manager__editor {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-orders-manager__editor label {
    min-width: 0;
  }

  .admin-orders-manager__primary {
    width: 100%;
  }

  .admin-orders-manager__items li {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
