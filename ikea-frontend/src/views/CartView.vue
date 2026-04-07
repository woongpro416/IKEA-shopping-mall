<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CartDeliveryGroupCards from '../components/cart/CartDeliveryGroupCards.vue';
import CartDeliveryGroupTable from '../components/cart/CartDeliveryGroupTable.vue';
import CartRecommendationsSection from '../components/cart/CartRecommendationsSection.vue';
import CartShippingGuideModal from '../components/cart/CartShippingGuideModal.vue';
import CartSummarySection from '../components/cart/CartSummarySection.vue';
import GuestCheckoutPromptDialog from '../components/common/GuestCheckoutPromptDialog.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import { useCommerceCart } from '../composables/useCommerceCart';
import { useCartViewState } from '../composables/useCartViewState';
import { ROUTE_PATHS } from '../constants/routes';
import { buildDeliveryGroups } from '../services/commerceShippingService';
import { useAccountStore } from '../stores/account';
import { hasAuthenticatedSession } from '../utils/accessControl';

const router = useRouter();
const route = useRoute();
const accountStore = useAccountStore();
const noticeVisible = ref(true);
const showShippingGuideModal = ref(false);
const shippingGuideTitle = ref('');
const shippingGuideBody = ref('');
const isGuestCheckoutPromptOpen = ref(false);
const pendingCheckoutIntent = ref({
  mode: 'all',
  itemId: '',
});

const {
  cartItems,
  selectedItems,
  allSelected,
  recommendations,
  refreshCart,
  updateQuantity,
  removeItem,
  removeSelected,
} = useCommerceCart();

onMounted(() => {
  accountStore.hydrate();
  void refreshCart({ force: true }).catch(() => {});
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
});

const {
  discountTotal,
  finalTotal,
  formatPrice,
  goToCheckout,
  productTotal,
  recommendProducts,
  shippingTotal,
} = useCartViewState(router, ROUTE_PATHS.orderCheckout, selectedItems, recommendations);

const hasSelectableItems = computed(() => cartItems.value.some((item) => !item.isSoldOut));
const canCheckoutSelected = computed(() => selectedItems.value.length > 0);
const canCheckoutAll = computed(() => hasSelectableItems.value);
const soldOutItemCount = computed(() => cartItems.value.filter((item) => item.isSoldOut).length);
const deliveryGroups = computed(() => buildDeliveryGroups(cartItems.value));

function openShippingGuide(title, body) {
  shippingGuideTitle.value = title || '배송 안내';
  shippingGuideBody.value = body || '결제 단계에서 배송 일정과 추가 비용을 다시 확인할 수 있습니다.';
  showShippingGuideModal.value = true;
}

function closeShippingGuide() {
  showShippingGuideModal.value = false;
}

function openGuestCheckoutPrompt(mode = 'all', itemId = '') {
  pendingCheckoutIntent.value = {
    mode,
    itemId: String(itemId ?? ''),
  };
  isGuestCheckoutPromptOpen.value = true;
}

function closeGuestCheckoutPrompt() {
  isGuestCheckoutPromptOpen.value = false;
  pendingCheckoutIntent.value = {
    mode: 'all',
    itemId: '',
  };
}

function moveToMemberLogin() {
  closeGuestCheckoutPrompt();
  router.push({
    path: ROUTE_PATHS.memberLogin,
    query: {
      redirect: route.fullPath,
    },
  });
}

function handleCheckoutAction(mode = 'all', itemId = '') {
  if (!hasAuthenticatedSession(accountStore)) {
    openGuestCheckoutPrompt(mode, itemId);
    return;
  }

  goToCheckout(mode, itemId);
}

function continueGuestCheckout() {
  const { mode, itemId } = pendingCheckoutIntent.value;
  closeGuestCheckoutPrompt();
  goToCheckout(mode, itemId);
}
</script>

<template>
  <SiteChrome>
    <main class="cart-page">
      <div class="cart-page__inner">
        <nav class="cart-breadcrumb" aria-label="breadcrumb">
          <RouterLink to="/" class="cart-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>&gt;</span>
          <span>장바구니</span>
        </nav>

        <section class="cart-section">
          <div class="cart-section__title-row">
            <h1>장바구니 ({{ cartItems.length }})</h1>
          </div>

          <div v-if="noticeVisible" class="cart-notice">
            <span>선택한 상품 기준으로 배송 조건과 결제 금액을 먼저 확인할 수 있습니다.</span>
            <button type="button" aria-label="안내 닫기" @click="noticeVisible = false">×</button>
          </div>

          <div v-if="soldOutItemCount" class="cart-stock-notice">
            품절 상품 {{ soldOutItemCount }}개는 주문 대상에서 제외됩니다.
          </div>

          <div class="cart-table">
            <div class="cart-table__head">
              <label class="cart-check cart-table__check">
                <input v-model="allSelected" type="checkbox" :disabled="!hasSelectableItems" />
              </label>
              <span class="cart-table__thumb-placeholder" aria-hidden="true"></span>
              <span class="cart-table__info-col">상품정보</span>
              <span>수량</span>
              <span>상품금액</span>
              <span>배송정보</span>
              <span>주문하기</span>
            </div>

            <template v-if="deliveryGroups.length">
              <template v-for="group in deliveryGroups" :key="group.key">
                <div class="cart-delivery-row">
                  <label class="cart-check cart-table__check">
                    <input v-model="allSelected" type="checkbox" :disabled="!hasSelectableItems" />
                  </label>
                  <div class="cart-table__thumb-placeholder" aria-hidden="true"></div>
                  <div class="cart-delivery-row__copy">
                    <strong>{{ group.title }}({{ group.itemCount }})</strong>
                    <span>{{ group.subtitle }}</span>
                  </div>
                </div>

                <CartDeliveryGroupTable
                  class="cart-delivery-group-table"
                  :group="group"
                  :format-price="formatPrice"
                  :go-to-checkout="handleCheckoutAction"
                  :open-shipping-guide="openShippingGuide"
                  :remove-item="removeItem"
                  :update-quantity="updateQuantity"
                />
                <CartDeliveryGroupCards
                  class="cart-delivery-group-cards"
                  :group="group"
                  :format-price="formatPrice"
                  :go-to-checkout="handleCheckoutAction"
                  :open-shipping-guide="openShippingGuide"
                  :remove-item="removeItem"
                  :update-quantity="updateQuantity"
                />

              </template>
            </template>

            <template v-else-if="cartItems.length">
              <div class="cart-delivery-row">
                <label class="cart-check cart-table__check">
                  <input v-model="allSelected" type="checkbox" :disabled="!hasSelectableItems" />
                </label>
                <div class="cart-table__thumb-placeholder" aria-hidden="true"></div>
                <div class="cart-delivery-row__copy">
                  <strong>HOMiO 배송 묶음</strong>
                  <span>선택한 상품 기준으로 배송 일정과 배송비를 확인합니다.</span>
                </div>
              </div>

              <article v-for="item in cartItems" :key="item.id" class="cart-item" :class="{ 'is-soldout': item.isSoldOut }">
                <div class="cart-item__select">
                  <label class="cart-check">
                    <input v-model="item.selected" type="checkbox" :disabled="item.isSoldOut" />
                  </label>
                </div>

                <RouterLink :to="item.detailPath" class="cart-item__thumb-link">
                  <img :src="item.image" :alt="item.name" />
                </RouterLink>

                <div class="cart-item__copy">
                  <div class="cart-item__brand-line">
                    <strong>{{ item.brand }}</strong>
                    <span>{{ item.seller }}</span>
                  </div>

                  <h2>
                    <RouterLink :to="item.detailPath" class="cart-item__title-link">
                      {{ item.name }}
                    </RouterLink>
                  </h2>
                  <p>{{ item.option }}</p>
                  <p v-if="item.isSoldOut" class="cart-item__soldout">품절 상품입니다. 재입고 전까지 주문할 수 없습니다.</p>
                </div>

                <div class="cart-item__qty">
                  <div class="qty-stepper">
                    <button type="button" aria-label="수량 감소" :disabled="item.isSoldOut" @click="updateQuantity(item.id, -1)">−</button>
                    <span>{{ item.quantity }}</span>
                    <button type="button" aria-label="수량 증가" :disabled="item.isSoldOut" @click="updateQuantity(item.id, 1)">+</button>
                  </div>
                </div>

                <div class="cart-item__price">
                  <strong>{{ formatPrice(item.price * item.quantity) }}</strong>
                  <span v-if="(item.originalPrice ?? item.price) > item.price">
                    {{ formatPrice((item.originalPrice ?? item.price) * item.quantity) }}
                  </span>
                </div>

                <div class="cart-item__shipping">
                  <button
                    class="cart-shipping-trigger"
                    type="button"
                    @click="openShippingGuide(item.shippingText, item.shippingSubText)"
                  >
                    {{ item.shippingText }}
                  </button>
                  <p>{{ item.shippingSubText }}</p>
                </div>

                <div class="cart-item__actions">
                  <button class="cart-item__order-btn" type="button" :disabled="item.isSoldOut" @click="handleCheckoutAction('single', item.productId)">
                    {{ item.isSoldOut ? '품절' : '바로주문' }}
                  </button>
                  <button class="cart-item__remove-btn" type="button" @click="removeItem(item.id)">삭제</button>
                </div>
              </article>
            </template>

            <div v-else class="cart-empty">장바구니에 담긴 상품이 없습니다.</div>
          </div>

          <div class="cart-actions-row">
            <button class="cart-delete-btn" type="button" @click="removeSelected">선택상품 삭제</button>
          </div>

          <ul class="cart-policy-list">
            <li>수량을 조정하면 장바구니에서 바로 금액이 반영됩니다.</li>
            <li>배송비와 설치 조건은 상품 유형과 배송지에 따라 결제 단계에서 다시 확인됩니다.</li>
            <li>선택 주문과 전체 주문은 현재 장바구니 상태를 기준으로 결제 페이지에 반영됩니다.</li>
          </ul>

          <CartSummarySection
            :can-checkout-all="canCheckoutAll"
            :can-checkout-selected="canCheckoutSelected"
            :discount-total="discountTotal"
            :final-total="finalTotal"
            :format-price="formatPrice"
            :product-total="productTotal"
            :shipping-total="shippingTotal"
            @checkout="handleCheckoutAction"
          />
        </section>

        <CartRecommendationsSection
          v-if="recommendProducts.length"
          :format-price="formatPrice"
          :items="recommendProducts"
        />

        <section class="cart-guide">
          <h2>장바구니 이용안내</h2>
          <ul>
            <li>상품 상세에서 확인한 이미지와 옵션 정보가 같은 기준으로 장바구니에 이어집니다.</li>
            <li>선택한 상품만 주문하거나 전체 상품을 한 번에 결제할 수 있습니다.</li>
            <li>배송지와 결제수단 설정은 주문서작성 화면에서 이어서 확인할 수 있습니다.</li>
          </ul>
        </section>
      </div>
    </main>

    <CartShippingGuideModal
      :body="shippingGuideBody"
      :open="showShippingGuideModal"
      :title="shippingGuideTitle"
      @close="closeShippingGuide"
    />
    <GuestCheckoutPromptDialog
      :open="isGuestCheckoutPromptOpen"
      @close="closeGuestCheckoutPrompt"
      @guest-order="continueGuestCheckout"
      @member-order="moveToMemberLogin"
    />
  </SiteChrome>
</template>

<style scoped>
.cart-page {
  padding: 28px 0 88px;
  background: #ffffff;
}

.cart-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  display: grid;
  gap: 48px;
}

.cart-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.cart-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.cart-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.cart-section,
.cart-guide {
  display: grid;
  gap: 22px;
}

.cart-section__title-row h1,
.cart-guide h2 {
  margin: 0;
  color: #111111;
  letter-spacing: -0.04em;
}

.cart-section__title-row h1 {
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
}

.cart-guide h2 {
  font-size: 28px;
  line-height: 1.25;
  font-weight: 700;
}

.cart-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  background: #f7f9fb;
  border: 1px solid #d9e2ec;
  color: #111827;
  font-size: 15px;
}

.cart-notice button {
  border: 0;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  font-size: 20px;
}

.cart-table {
  border-top: 2px solid #111111;
}

.cart-delivery-group-cards {
  display: none;
}

.cart-table__head,
.cart-delivery-row,
.cart-item {
  display: grid;
  grid-template-columns: 56px 136px minmax(0, 1fr) 132px 160px 188px 124px;
  align-items: center;
}

.cart-table__head {
  min-height: 58px;
  border-bottom: 1px solid #d8dde5;
  color: #555555;
  font-size: 14px;
  font-weight: 600;
}

.cart-table__head > span {
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-table__info-col {
  justify-content: flex-start !important;
}

.cart-delivery-row {
  min-height: 72px;
  border-bottom: 1px solid #d8dde5;
}

.cart-delivery-row__copy {
  display: grid;
  gap: 6px;
  align-content: center;
}

.cart-delivery-row__copy strong {
  color: #111827;
  font-weight: 700;
}

.cart-delivery-row__copy span {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.55;
}

.cart-table__check,
.cart-item__select {
  display: inline-flex;
  justify-content: center;
}

.cart-table__thumb-placeholder {
  display: block;
  width: 136px;
  height: 1px;
}

.cart-check input {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: #111111;
}

.cart-stock-notice {
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px solid #f3d1d1;
  background: #fff5f5;
  color: #b42318;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.6;
}

.cart-item {
  min-height: 176px;
  padding: 24px 0;
  border-bottom: 1px solid #eceff3;
}

.cart-item.is-soldout {
  background: linear-gradient(90deg, rgba(248, 250, 252, 0.8), rgba(255, 255, 255, 0));
}

.cart-item__thumb-link {
  display: block;
}

.cart-item__thumb-link img {
  display: block;
  width: 128px;
  height: 128px;
  object-fit: contain;
  background: #ffffff;
}

.cart-item__copy {
  display: grid;
  gap: 10px;
  padding-right: 20px;
}

.cart-item__brand-line {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
}

.cart-item__brand-line strong {
  color: #111827;
}

.cart-item__copy h2 {
  margin: 0;
}

.cart-item__title-link {
  color: #111827;
  font-size: 18px;
  line-height: 1.45;
}

.cart-item__copy p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.cart-item__soldout {
  color: #b42318;
  font-weight: 700;
}

.cart-item__qty,
.cart-item__price,
.cart-item__shipping,
.cart-item__actions {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 12px;
}

.qty-stepper {
  display: inline-grid;
  grid-template-columns: 36px 44px 36px;
  align-items: center;
  height: 38px;
  border: 1px solid #d8dde5;
  background: #ffffff;
}

.qty-stepper button {
  border: 0;
  background: #ffffff;
  height: 100%;
  color: #111111;
  font-size: 18px;
  cursor: pointer;
}

.qty-stepper button:disabled {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: default;
}

.qty-stepper span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  border-left: 1px solid #d8dde5;
  border-right: 1px solid #d8dde5;
}

.cart-item__price strong {
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: -0.04em;
  color: #111111;
}

.cart-item__price span {
  color: #9ca3af;
  font-size: 13px;
  text-decoration: line-through;
}

.cart-item__shipping {
  text-align: center;
}

.cart-shipping-trigger,
.cart-shipping-link {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.cart-shipping-trigger {
  color: #0058a3;
  font-size: 16px;
  font-weight: 700;
}

.cart-item__shipping p {
  margin: 0;
  color: #6b7280;
  white-space: pre-line;
  font-size: 13px;
  line-height: 1.55;
}

.cart-shipping-link {
  color: #4b5563;
  font-size: 12px;
  text-decoration: underline;
}

.cart-item__actions {
  justify-items: stretch;
}

.cart-item__order-btn {
  min-height: 46px;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
}

.cart-item__order-btn {
  width: 100%;
  border: 1px solid #111827;
  background: #111827;
  color: #ffffff;
}

.cart-item__order-btn:disabled {
  border-color: #d8dde5;
  background: #eef1f4;
  color: #8a93a0;
  cursor: default;
}

.cart-delete-btn {
  min-width: 92px;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid #d8dde5;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
}

.cart-item__remove-btn {
  border: 0;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
}

.cart-empty {
  padding: 48px 0;
  text-align: center;
  color: #6b7280;
}

.cart-actions-row {
  display: flex;
  justify-content: flex-start;
}

.cart-policy-list,
.cart-guide ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .cart-table__head,
  .cart-delivery-row,
  .cart-item {
    grid-template-columns: 44px 104px minmax(0, 1fr) 112px 132px 156px 112px;
  }

  .cart-item__thumb-link img {
    width: 96px;
    height: 96px;
  }

}

@media (max-width: 820px) {
  .cart-page__inner {
    width: min(100%, calc(100% - 24px));
    gap: 32px;
  }

  .cart-table__head {
    display: none;
  }

  .cart-delivery-group-table {
    display: none;
  }

  .cart-delivery-group-cards {
    display: block;
  }

  .cart-delivery-row,
  .cart-item {
    grid-template-columns: 32px 88px minmax(0, 1fr);
    align-items: start;
    gap: 14px;
  }

  .cart-delivery-row {
    padding: 18px 0;
    min-height: 0;
  }

  .cart-item {
    padding: 20px 0;
  }

  .cart-item__qty,
  .cart-item__price,
  .cart-item__shipping,
  .cart-item__actions {
    grid-column: 3;
    justify-items: start;
    text-align: left;
  }

  .cart-item__shipping p {
    text-align: left;
  }

}
</style>
