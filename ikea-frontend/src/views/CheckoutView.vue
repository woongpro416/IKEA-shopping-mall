<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CheckoutDeliveryGroupTable from '../components/checkout/CheckoutDeliveryGroupTable.vue';
import CheckoutFinalCard from '../components/checkout/CheckoutFinalCard.vue';
import CheckoutInstallNoticeModal from '../components/checkout/CheckoutInstallNoticeModal.vue';
import CheckoutShippingGuideModal from '../components/checkout/CheckoutShippingGuideModal.vue';
import CommonStatePanel from '../components/common/CommonStatePanel.vue';
import SiteChrome from '../components/layout/SiteChrome.vue';
import {
  completeCheckout,
  getCheckoutItemsForFlow,
  startExternalCheckout,
  useCommerceCart,
} from '../composables/useCommerceCart';
import { useFeedback } from '../composables/useFeedback';
import { ROUTE_PATHS } from '../constants/routes';
import {
  buildDeliveryGroups,
  calculateShippingTotal,
  hasSpecialDeliveryItems as hasSpecialDeliveryItemsInOrder,
  isSpecialDeliveryItem,
} from '../services/commerceShippingService';
import { useAccountStore } from '../stores/account';
import { resolveCheckoutErrorMessage } from '../utils/apiErrorMessage';

const route = useRoute();
const router = useRouter();
const accountStore = useAccountStore();
const { refreshCart } = useCommerceCart();
const { showError } = useFeedback();
const scheduleTimeOptions = ['오전', '오후'];

const orderItems = ref([]);
const ordererName = ref('');
const ordererPhone = ref('');
const sameAsOrderer = ref(true);
const receiverName = ref('');
const receiverPhone = ref('');
const zoneCode = ref('');
const addressMain = ref('');
const addressDetail = ref('');
const deliveryRequest = ref('');
const elevatorOption = ref('1-7인승');
const carAccess = ref('진입가능');
const freeCarryService = ref(false);
const scheduleYear = ref('');
const scheduleMonth = ref('');
const scheduleDay = ref('');
const scheduleTime = ref('');
const pointAmount = ref('0');
const paymentMethod = ref('bank');
const finalAgreement = ref(false);
const isSubmitting = ref(false);
const checkoutAddressGuide = ref('');
const isGuestCheckout = computed(() => !accountStore.accessToken);
const EXTERNAL_PAYMENT_METHODS = ['kakaopay', 'tosspay'];

const BASE_PAYMENT_METHODS = [
  {
    id: 'kakaopay',
    label: '카카오페이',
    enabled: true,
    memberOnly: false,
  },
  {
    id: 'tosspay',
    label: '토스페이',
    enabled: true,
    memberOnly: false,
  },
  {
    id: 'bank',
    label: '무통장입금',
    enabled: true,
    memberOnly: false,
  },
];

async function syncOrderItems() {
  accountStore.hydrate();

  try {
    await refreshCart({ force: true });
  } catch {
    // Keep the current checkout snapshot when the cart sync is temporarily unavailable.
  }

  orderItems.value = getCheckoutItemsForFlow(
    String(route.query.mode ?? 'all'),
    String(route.query.itemId ?? ''),
  );
}

onMounted(async () => {
  await syncOrderItems();
  paymentMethod.value = defaultPaymentMethod.value;
  prefillCheckoutFormFromAccount();
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
});

watch(
  () => [route.query.mode, route.query.itemId],
  async () => {
    await syncOrderItems();
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  },
);

watch([sameAsOrderer, ordererName, ordererPhone], ([checked, name, phone]) => {
  if (checked) {
    receiverName.value = name;
    receiverPhone.value = phone;
  }
}, { immediate: true });

watch(pointAmount, (value) => {
  const normalized = String(value ?? '').replace(/[^\d]/g, '');
  pointAmount.value = normalized === '' ? '0' : String(Number(normalized));
});

const paymentMethods = computed(() => BASE_PAYMENT_METHODS.filter((method) => (
  method.enabled && (!method.memberOnly || !isGuestCheckout.value)
)));

const defaultPaymentMethod = computed(() => (
  paymentMethods.value[0]?.id ?? ''
));

const deliveryGroups = computed(() => buildDeliveryGroups(orderItems.value));
const hasInstallDeliveryItems = computed(() => hasSpecialDeliveryItemsInOrder(orderItems.value));
const orderCount = computed(() => orderItems.value.reduce((sum, item) => sum + item.quantity, 0));
const productTotal = computed(() => orderItems.value.reduce(
  (sum, item) => sum + ((item.originalPrice ?? item.price) * item.quantity),
  0,
));
const catalogDiscountTotal = computed(() => orderItems.value.reduce(
  (sum, item) => sum + (((item.originalPrice ?? item.price) - item.price) * item.quantity),
  0,
));
const couponDiscount = computed(() => 0);
const shippingTotal = computed(() => calculateShippingTotal(orderItems.value));
const maxPointUsage = computed(() => Math.max(
  0,
  productTotal.value - catalogDiscountTotal.value - couponDiscount.value,
));
const pointApplied = computed(() => Math.min(Number(pointAmount.value || '0'), maxPointUsage.value));
const finalTotal = computed(() => Math.max(
  0,
  maxPointUsage.value - pointApplied.value + shippingTotal.value,
));
const selectedPaymentMethod = computed(
  () => paymentMethods.value.find((method) => method.id === paymentMethod.value)
    ?? paymentMethods.value[0],
);
const paymentMethodGuide = computed(() => (
  '카카오페이, 토스페이, 무통장입금 중 원하는 결제수단을 선택해 주세요.'
));
const submitButtonLabel = computed(() => (
  paymentMethod.value === 'kakaopay'
    ? '카카오페이로 이동'
    : paymentMethod.value === 'tosspay'
      ? '토스페이로 이동'
      : '주문 접수하기'
));
const availableScheduleDates = computed(() => {
  const dates = [];
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const end = new Date(start);

  end.setMonth(end.getMonth() + 3);

  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    dates.push({
      key: `${cursor.getFullYear()}-${cursor.getMonth() + 1}-${cursor.getDate()}`,
      year: String(cursor.getFullYear()),
      month: String(cursor.getMonth() + 1),
      day: String(cursor.getDate()),
    });
  }

  return dates;
});
const scheduleYearOptions = computed(() => (
  [...new Set(availableScheduleDates.value.map((item) => item.year))]
));
const scheduleMonthOptions = computed(() => (
  [...new Set(
    availableScheduleDates.value
      .filter((item) => item.year === scheduleYear.value)
      .map((item) => item.month),
  )]
));
const scheduleDayOptions = computed(() => (
  availableScheduleDates.value
    .filter((item) => item.year === scheduleYear.value && item.month === scheduleMonth.value)
    .map((item) => item.day)
));

watch(
  paymentMethods,
  (methods) => {
    if (!methods.some((method) => method.id === paymentMethod.value)) {
      paymentMethod.value = methods[0]?.id ?? '';
    }
  },
  { immediate: true },
);

watch(
  [availableScheduleDates, scheduleYear, scheduleMonth],
  () => {
    const [firstDate] = availableScheduleDates.value;

    if (!firstDate) {
      return;
    }

    if (!scheduleYearOptions.value.includes(scheduleYear.value)) {
      scheduleYear.value = firstDate.year;
    }

    if (!scheduleMonthOptions.value.includes(scheduleMonth.value)) {
      scheduleMonth.value = scheduleMonthOptions.value[0] ?? firstDate.month;
    }

    if (!scheduleDayOptions.value.includes(scheduleDay.value)) {
      scheduleDay.value = scheduleDayOptions.value[0] ?? firstDate.day;
    }

    if (!scheduleTimeOptions.includes(scheduleTime.value)) {
      scheduleTime.value = scheduleTimeOptions[0];
    }
  },
  { immediate: true },
);
const installationCategoryCounts = computed(() => {
  const grouped = new Map();

  orderItems.value.forEach((item) => {
    if (!isSpecialDeliveryItem(item)) {
      return;
    }

    const currentCount = grouped.get(item.categoryLabel) ?? 0;
    grouped.set(item.categoryLabel, currentCount + item.quantity);
  });

  return Array.from(grouped.entries()).map(([label, quantity]) => ({ label, quantity }));
});

function formatPrice(value) {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`;
}

function prefillCheckoutFormFromAccount() {
  accountStore.hydrate();

  const memberName = String(accountStore.memberName ?? '').trim();
  const phoneNumber = String(accountStore.phoneNumber ?? '').trim();
  const memberZoneCode = String(accountStore.zoneCode ?? '').trim();
  const memberAddressMain = String(accountStore.addressMain ?? '').trim();
  const memberAddressDetail = String(accountStore.addressDetail ?? '').trim();

  if (memberName) {
    ordererName.value = memberName;
  }

  if (phoneNumber) {
    ordererPhone.value = phoneNumber;
  }

  if (memberZoneCode && !String(zoneCode.value ?? '').trim()) {
    zoneCode.value = memberZoneCode;
  }

  if (memberAddressMain && !String(addressMain.value ?? '').trim()) {
    addressMain.value = memberAddressMain;
  }

  if (memberAddressDetail && !String(addressDetail.value ?? '').trim()) {
    addressDetail.value = memberAddressDetail;
  }
}

const installNoticeChecked = ref(false);
const showInstallNoticeModal = ref(false);
const showShippingGuideModal = ref(false);
const shippingGuideTitle = ref('');
const shippingGuideBody = ref('');

function openInstallNoticeModal() {
  showInstallNoticeModal.value = true;
}

function closeInstallNoticeModal() {
  showInstallNoticeModal.value = false;
}

function confirmInstallNotice() {
  installNoticeChecked.value = true;
  closeInstallNoticeModal();
}

function toggleInstallNotice() {
  if (installNoticeChecked.value) {
    installNoticeChecked.value = false;
    return;
  }

  openInstallNoticeModal();
}

function openShippingGuide(title, body) {
  shippingGuideTitle.value = title || '배송 안내';
  shippingGuideBody.value = body || '결제 단계에서 배송 일정과 추가 비용을 다시 확인할 수 있습니다.';
  showShippingGuideModal.value = true;
}

function closeShippingGuideModal() {
  showShippingGuideModal.value = false;
}

function openZoneCodeGuide() {
  checkoutAddressGuide.value = '우편번호와 주소를 직접 입력해 주세요.';
}

function applyMaxPointAmount() {
  pointAmount.value = String(maxPointUsage.value);
}

function buildScheduleText() {
  return `${scheduleYear.value}년 ${scheduleMonth.value}월 ${scheduleDay.value}일 ${scheduleTime.value}`;
}

function selectPaymentMethod(method) {
  if (!method?.id) {
    return;
  }

  paymentMethod.value = method.id;
}

function validateCheckout() {
  if (!orderItems.value.length) {
    return '주문할 상품이 없습니다.';
  }

  if (!String(ordererName.value ?? '').trim()) {
    return '주문자 이름을 입력해 주세요.';
  }

  if (!String(ordererPhone.value ?? '').trim()) {
    return '주문자 연락처를 입력해 주세요.';
  }

  if (!String(receiverName.value ?? '').trim()) {
    return '받으시는 분 이름을 입력해 주세요.';
  }

  if (!String(receiverPhone.value ?? '').trim()) {
    return '받으시는 분 연락처를 입력해 주세요.';
  }

  if (!String(zoneCode.value ?? '').trim()) {
    return '우편번호를 입력해 주세요.';
  }

  if (!String(addressMain.value ?? '').trim()) {
    return '기본 주소를 입력해 주세요.';
  }

  if (!finalAgreement.value) {
    return '결제 진행 동의 후 주문을 완료해 주세요.';
  }

  return '';
}

async function submitOrder() {
  if (isSubmitting.value) {
    return;
  }

  const validationMessage = validateCheckout();

  if (validationMessage) {
    showError(validationMessage);
    return;
  }

  isSubmitting.value = true;

  try {
    const checkoutPayload = {
      mode: String(route.query.mode ?? 'all'),
      itemId: String(route.query.itemId ?? ''),
      orderItems: orderItems.value,
      ordererName: ordererName.value.trim(),
      ordererPhone: ordererPhone.value.trim(),
      receiverName: receiverName.value.trim(),
      receiverPhone: receiverPhone.value.trim(),
      zoneCode: zoneCode.value.trim(),
      addressMain: addressMain.value.trim(),
      addressDetail: addressDetail.value.trim(),
      deliveryRequest: deliveryRequest.value.trim(),
      scheduleText: buildScheduleText(),
      paymentMethod: paymentMethod.value,
      paymentMethodLabel: selectedPaymentMethod.value.label,
      productTotal: productTotal.value,
      discountTotal: catalogDiscountTotal.value,
      couponDiscount: couponDiscount.value,
      pointApplied: pointApplied.value,
      shippingTotal: shippingTotal.value,
      finalTotal: finalTotal.value,
    };

    if (EXTERNAL_PAYMENT_METHODS.includes(paymentMethod.value)) {
      const paymentRedirect = await startExternalCheckout(checkoutPayload);
      window.location.assign(paymentRedirect.redirectUrl);
      return;
    }

    const completedOrder = await completeCheckout(checkoutPayload);

    await router.push({
      path: ROUTE_PATHS.orderComplete,
      query: {
        orderNumber: completedOrder.orderNumber,
        orderType: completedOrder.isGuestOrder ? 'guest' : 'member',
        ...(completedOrder.isGuestOrder
          ? {
            buyerName: String(
              completedOrder.ordererName
              ?? checkoutPayload.ordererName
              ?? '',
            ).trim(),
          }
          : {}),
      },
    });
  } catch (error) {
    showError(resolveCheckoutErrorMessage(error, '주문 처리 중 오류가 발생했습니다.'));
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <SiteChrome>
    <main class="checkout-page">
      <div class="checkout-page__inner">
        <nav class="checkout-breadcrumb" aria-label="breadcrumb">
          <RouterLink to="/" class="checkout-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>〉</span>
          <span>주문서작성</span>
        </nav>

        <section class="checkout-section">
          <div class="checkout-section__title-row">
            <h1>주문서작성</h1>
            <button class="checkout-outline-button" type="button" @click="router.push(ROUTE_PATHS.cart)">장바구니가기</button>
          </div>

          <div v-if="deliveryGroups.length" class="checkout-board">
            <div class="checkout-board__head">
              <span class="checkout-board__info">상품정보</span>
              <span>수량</span>
              <span>상품금액</span>
              <span>배송정보</span>
            </div>

            <template v-for="group in deliveryGroups" :key="group.key">
              <div class="checkout-board__bundle">
                <strong>{{ group.title }} ({{ group.itemCount }})</strong>
                <span>{{ group.subtitle }}</span>
              </div>

              <CheckoutDeliveryGroupTable
                :group="group"
                :format-price="formatPrice"
                :open-shipping-guide="openShippingGuide"
              />

            </template>

            <div class="checkout-total-strip">
              <article>
                <span>총 상품금액</span>
                <strong>{{ formatPrice(productTotal) }}</strong>
              </article>
              <article>
                <span>총 할인금액</span>
                <strong>{{ formatPrice(catalogDiscountTotal + couponDiscount + pointApplied) }}</strong>
              </article>
              <article>
                <span>총 배송비</span>
                <strong>{{ formatPrice(shippingTotal) }}</strong>
              </article>
              <article class="is-accent">
                <span>총 결제예정금액</span>
                <strong>{{ formatPrice(finalTotal) }}</strong>
              </article>
            </div>
          </div>

          <div v-else-if="orderItems.length" class="checkout-board">
            <div class="checkout-board__head">
              <span class="checkout-board__info">상품정보</span>
              <span>수량</span>
              <span>상품금액</span>
              <span>배송정보</span>
            </div>

            <div class="checkout-board__bundle">
              <strong>HOMiO 주문 상품 ({{ orderCount }})</strong>
              <span>배송 일정과 배송비는 상품 유형과 주소지에 따라 결제 단계에서 최종 안내됩니다.</span>
            </div>

            <article v-for="item in orderItems" :key="item.id" class="checkout-item">
              <div class="checkout-item__info">
                <RouterLink :to="item.detailPath" class="checkout-item__thumb">
                  <img :src="item.image" :alt="item.name" />
                </RouterLink>
                <div class="checkout-item__copy">
                  <div class="checkout-item__meta">
                    <strong>{{ item.brand }}</strong>
                    <span>{{ item.seller }}</span>
                  </div>
                  <h2>
                    <RouterLink :to="item.detailPath">{{ item.name }}</RouterLink>
                  </h2>
                  <p>{{ item.option }}</p>
                </div>
              </div>

              <div class="checkout-item__qty">{{ item.quantity }}</div>

              <div class="checkout-item__price">
                <strong>{{ formatPrice(item.price * item.quantity) }}</strong>
                <span v-if="(item.originalPrice ?? item.price) > item.price">
                  {{ formatPrice((item.originalPrice ?? item.price) * item.quantity) }}
                </span>
              </div>

              <div class="checkout-item__shipping">
                <button
                  class="checkout-shipping-trigger"
                  type="button"
                  @click="openShippingGuide(item.shippingText, item.shippingSubText)"
                >
                  {{ item.shippingText }}
                </button>
                <p>{{ item.shippingSubText }}</p>
              </div>
            </article>

            <div class="checkout-total-strip">
              <article>
                <span>총 상품금액</span>
                <strong>{{ formatPrice(productTotal) }}</strong>
              </article>
              <article>
                <span>총 할인금액</span>
                <strong>{{ formatPrice(catalogDiscountTotal + couponDiscount + pointApplied) }}</strong>
              </article>
              <article>
                <span>총 배송비</span>
                <strong>{{ formatPrice(shippingTotal) }}</strong>
              </article>
              <article class="is-accent">
                <span>총 결제예정금액</span>
                <strong>{{ formatPrice(finalTotal) }}</strong>
              </article>
            </div>
          </div>

          <div v-else class="checkout-empty">
            <CommonStatePanel
              title="주문 가능한 상품이 없습니다."
              description="품절 상품은 체크아웃 대상에서 제외됩니다. 장바구니에서 재고 상태를 다시 확인해 주세요."
              layout="boxed"
              compact
            >
              <template #actions>
                <button class="checkout-outline-button" type="button" @click="router.push(ROUTE_PATHS.cart)">장바구니로 돌아가기</button>
              </template>
            </CommonStatePanel>
          </div>
        </section>

        <div v-if="orderItems.length" class="checkout-layout">
          <section class="checkout-main-column">
            <section class="checkout-panel checkout-delivery-panel">
              <header class="checkout-panel__head">
                <h2>주문 상품 배송정보</h2>
              </header>

              <div class="checkout-form-table">
                <div class="checkout-form-row">
                  <div class="checkout-form-row__label">주문자 정보</div>
                  <div class="checkout-form-row__content checkout-form-row__content--inline">
                    <input v-model="ordererName" type="text" placeholder="주문자 이름" />
                    <input v-model="ordererPhone" type="text" placeholder="휴대폰 번호" />
                    <p class="checkout-inline-note">
                      {{ isGuestCheckout ? '비회원은 주문자 정보를 직접 입력해 주세요.' : '회원 정보가 자동 입력되며, 이 화면에서 직접 수정할 수 있습니다.' }}
                    </p>
                  </div>
                </div>

                <div class="checkout-form-row">
                  <div class="checkout-form-row__label">받으시는 분 <span>*</span></div>
                  <div class="checkout-form-row__content">
                    <div class="checkout-inline">
                      <input v-model="receiverName" type="text" placeholder="받으시는 분 이름" :disabled="sameAsOrderer" />
                      <label class="checkout-check">
                        <input v-model="sameAsOrderer" type="checkbox" />
                        <span>주문자 정보와 동일</span>
                      </label>
                    </div>
                  </div>
                </div>

                <div class="checkout-form-row">
                  <div class="checkout-form-row__label">배송지 주소 <span>*</span></div>
                  <div class="checkout-form-row__content">
                    <div class="checkout-inline checkout-inline--zip">
                      <input v-model="zoneCode" type="text" placeholder="우편번호" />
                      <button class="checkout-outline-button is-small" type="button" @click="openZoneCodeGuide">우편번호 찾기</button>
                    </div>
                    <input v-model="addressMain" type="text" placeholder="기본 주소" />
                    <input v-model="addressDetail" type="text" placeholder="상세 주소" />
                    <p v-if="checkoutAddressGuide" class="checkout-field-note">{{ checkoutAddressGuide }}</p>
                  </div>
                </div>

                <div class="checkout-form-row">
                  <div class="checkout-form-row__label">휴대폰 번호 <span>*</span></div>
                  <div class="checkout-form-row__content">
                    <input v-model="receiverPhone" type="text" placeholder="'-' 없이 숫자만 입력해 주세요" />
                  </div>
                </div>

                <div v-if="hasInstallDeliveryItems" class="checkout-form-row">
                  <div class="checkout-form-row__label">배송 / 설치 참고 정보 <span>*</span></div>
                  <div class="checkout-form-row__content checkout-form-row__content--install">
                    <div class="checkout-choice-row">
                      <span>엘리베이터</span>
                      <div class="checkout-choice-group">
                        <label><input v-model="elevatorOption" type="radio" value="1-7인승" /> 1~7인승</label>
                        <label><input v-model="elevatorOption" type="radio" value="8-10인승" /> 8~10인승</label>
                        <label><input v-model="elevatorOption" type="radio" value="11인승 이상" /> 11인승 이상</label>
                        <label><input v-model="elevatorOption" type="radio" value="없음" /> 없음</label>
                      </div>
                    </div>

                    <div class="checkout-choice-row">
                      <span>차량현장 진입</span>
                      <div class="checkout-choice-group">
                        <label><input v-model="carAccess" type="radio" value="진입가능" /> 진입가능</label>
                        <label><input v-model="carAccess" type="radio" value="진입불가" /> 진입불가</label>
                      </div>
                    </div>

                    <div class="checkout-option-row">
                      <button class="checkout-option-row__main" type="button" @click="toggleInstallNotice">
                        <span class="checkout-option-row__box" :class="{ 'is-active': installNoticeChecked }">✓</span>
                        <span class="checkout-option-row__text">필수 설치 공간 및 현장 추가비용 안내를 확인했습니다.</span>
                      </button>
                      <button class="checkout-inline-link" type="button" @click="openInstallNoticeModal">자세히 보기</button>
                    </div>

                    <div class="checkout-option-row is-muted">
                      <label class="checkout-option-row__main checkout-option-row__main--label">
                        <input v-model="freeCarryService" class="checkout-option-row__input" type="checkbox" />
                        <span class="checkout-option-row__box" :class="{ 'is-active': freeCarryService }">✓</span>
                        <span class="checkout-option-row__text">배송 전 추가 확인 요청</span>
                      </label>
                      <button
                        class="checkout-inline-link"
                        type="button"
                        @click="openShippingGuide('배송 전 추가 확인 요청', '설치 및 추가 배송 조건을 함께 확인해야 하는 주문에서 배송 직전 재확인을 진행합니다.')"
                      >
                        자세히 보기
                      </button>
                    </div>

                    <div class="checkout-count-list">
                      <div v-for="item in installationCategoryCounts" :key="item.label">
                        <span>{{ item.label }}</span>
                        <strong>{{ item.quantity }}개</strong>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="hasInstallDeliveryItems" class="checkout-form-row">
                  <div class="checkout-form-row__label">배송 일정 선택</div>
                  <div class="checkout-form-row__content checkout-inline">
                    <select v-model="scheduleYear">
                      <option v-for="year in scheduleYearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="scheduleMonth">
                      <option v-for="month in scheduleMonthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                    <select v-model="scheduleDay">
                      <option v-for="day in scheduleDayOptions" :key="day" :value="day">{{ day }}일</option>
                    </select>
                    <select v-model="scheduleTime">
                      <option v-for="time in scheduleTimeOptions" :key="time" :value="time">{{ time }}</option>
                    </select>
                  </div>
                </div>

                <div v-if="hasInstallDeliveryItems" class="checkout-form-row">
                  <div class="checkout-form-row__label">배송 요청사항</div>
                  <div class="checkout-form-row__content">
                    <textarea v-model="deliveryRequest" rows="4" placeholder="추가 요청사항이 있다면 입력해 주세요"></textarea>
                  </div>
                </div>
              </div>
            </section>

            <section class="checkout-panel">
              <header class="checkout-panel__head">
                <h2>할인 / 혜택 적용</h2>
              </header>
              <div class="checkout-kv-list">
                <div><span>총 상품금액</span><strong>{{ formatPrice(productTotal) }}</strong></div>
                <div><span>상품할인</span><strong>{{ formatPrice(catalogDiscountTotal) }}</strong></div>
                <div class="checkout-kv-list__inline">
                  <span>상품쿠폰할인</span>
                  <div>
                    <input type="text" :value="couponDiscount" readonly />
                    <button class="checkout-outline-button is-small" type="button">적용가능쿠폰 (0)</button>
                  </div>
                </div>
                <div class="checkout-kv-list__inline">
                  <span>포인트</span>
                  <div>
                    <input v-model="pointAmount" type="text" />
                    <button class="checkout-outline-button is-small" type="button" @click="applyMaxPointAmount">전액사용</button>
                  </div>
                </div>
              </div>
            </section>

            <section class="checkout-panel">
              <header class="checkout-panel__head">
                <h2>결제수단 선택</h2>
              </header>
      <div class="checkout-payment-grid">
        <button
          v-for="method in paymentMethods"
                  :key="method.id"
                  class="checkout-payment-button"
                  :class="{ 'is-active': paymentMethod === method.id }"
                  type="button"
                  @click="selectPaymentMethod(method)"
                >
          <span>{{ method.label }}</span>
        </button>
      </div>
      <p class="checkout-field-note">{{ paymentMethodGuide }}</p>
    </section>
  </section>

          <CheckoutFinalCard
            v-model:final-agreement="finalAgreement"
            :catalog-discount-total="catalogDiscountTotal"
            :coupon-discount="couponDiscount"
            :final-total="finalTotal"
            :format-price="formatPrice"
            :is-guest-checkout="isGuestCheckout"
            :is-submitting="isSubmitting"
            :point-applied="pointApplied"
            :product-total="productTotal"
            :shipping-total="shippingTotal"
            :submit-button-label="submitButtonLabel"
            @submit="submitOrder"
          />
        </div>
      </div>
    </main>

    <CheckoutInstallNoticeModal
      :open="showInstallNoticeModal"
      @close="closeInstallNoticeModal"
      @confirm="confirmInstallNotice"
    />
    <CheckoutShippingGuideModal
      :body="shippingGuideBody"
      :open="showShippingGuideModal"
      :title="shippingGuideTitle"
      @close="closeShippingGuideModal"
    />
  </SiteChrome>
</template>

<style scoped>
.checkout-page {
  padding: 28px 0 88px;
  background: #ffffff;
}

.checkout-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  display: grid;
  gap: 40px;
  color: #111827;
}

.checkout-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.checkout-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.checkout-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.checkout-section__title-row,
.checkout-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.checkout-section__title-row {
  padding-bottom: 18px;
  border-bottom: 2px solid #111111;
}

.checkout-section__title-row h1,
.checkout-block__head h2 {
  margin: 0;
  color: #111111;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.checkout-section__title-row h1 {
  font-size: 32px;
  line-height: 1.2;
}

.checkout-block__head h2 {
  font-size: 24px;
  line-height: 1.25;
}

.checkout-outline-button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #d5d8dd;
  background: #ffffff;
  color: #555555;
  font-size: 13px;
  cursor: pointer;
}

.checkout-outline-button:disabled {
  cursor: default;
  opacity: 0.58;
}

.checkout-outline-button.is-small {
  min-height: 34px;
  padding: 0 10px;
}

.checkout-board {
  display: grid;
}

.checkout-board__head,
.checkout-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px 168px 188px;
  align-items: center;
}

.checkout-board__head {
  min-height: 58px;
  border-bottom: 1px solid #d8dde5;
  color: #555555;
  font-size: 14px;
  font-weight: 600;
}

.checkout-board__head span:nth-child(n + 2) {
  text-align: center;
}

.checkout-board__info {
  text-align: left !important;
}

.checkout-board__bundle {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  border-bottom: 1px solid #d8dde5;
  font-size: 14px;
}

.checkout-board__bundle span,
.checkout-item__copy p,
.checkout-item__shipping p {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.checkout-item {
  gap: 10px;
  padding: 22px 0;
  border-bottom: 1px solid #eceff3;
}

.checkout-item__info {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.checkout-item__thumb {
  display: block;
}

.checkout-item__thumb img {
  width: 94px;
  height: 94px;
  object-fit: contain;
  border: 1px solid #f0f0f0;
  background: #ffffff;
}

.checkout-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666666;
  font-size: 13px;
}

.checkout-item__copy {
  min-width: 0;
}

.checkout-item__copy h2 {
  margin: 8px 0 6px;
  font-size: 18px;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.checkout-item__copy a {
  color: inherit;
}

.checkout-item__qty,
.checkout-item__price,
.checkout-item__shipping {
  display: grid;
  justify-items: center;
  gap: 8px;
  text-align: center;
}

.checkout-item__shipping p {
  white-space: pre-line;
}

.checkout-item__qty {
  font-size: 16px;
  font-weight: 600;
}

.checkout-item__price strong,
.checkout-total-strip strong,
.checkout-kv-list strong {
  color: #111111;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.checkout-item__price strong {
  font-size: 24px;
}

.checkout-item__price span {
  color: #9ca3af;
  font-size: 13px;
  text-decoration: line-through;
}

.checkout-item__shipping strong {
  color: var(--accent);
  font-size: 15px;
}

.checkout-total-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding-top: 28px;
}

.checkout-total-strip article {
  min-height: 112px;
  padding: 20px 22px;
  border: 1px solid #e5e7eb;
  display: grid;
  gap: 10px;
  align-content: start;
}

.checkout-total-strip span {
  color: #6b7280;
  font-size: 14px;
}

.checkout-total-strip strong {
  font-size: 28px;
}

.checkout-total-strip .is-accent strong {
  color: #111111;
}

.checkout-empty {
  display: grid;
  justify-items: center;
  gap: 16px;
  padding: 48px 0 0;
}

.checkout-empty p {
  margin: 0;
  color: #6b7280;
}

.checkout-empty__sub {
  max-width: 420px;
  text-align: center;
  line-height: 1.6;
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 36px;
  align-items: start;
  margin-top: 2px;
}

.checkout-main-column {
  display: grid;
  gap: 28px;
  padding-top: 18px;
  border-top: 2px solid #111111;
}

.checkout-panel {
  display: grid;
  gap: 18px;
}

.checkout-panel__head {
  padding-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}

.checkout-panel__head h2 {
  margin: 0;
  color: #111111;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.checkout-form-section {
  display: grid;
  gap: 28px;
}

.checkout-form-board,
.checkout-side-block {
  display: grid;
  gap: 18px;
}

.checkout-form-board {
  border-top: 2px solid #111111;
}

.checkout-form-row {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr);
  min-height: 76px;
  border-bottom: 1px solid #ededed;
}

.checkout-form-row__label {
  padding: 20px 18px;
  background: #f7f7f8;
  color: #444444;
  font-size: 14px;
  font-weight: 700;
}

.checkout-form-row__label span {
  color: #d63b2d;
}

.checkout-form-row__content {
  display: grid;
  gap: 14px;
  align-content: center;
  padding: 16px 20px;
}

.checkout-form-row__content--install {
  gap: 16px;
}

.checkout-form-row__content--inline {
  grid-template-columns: minmax(0, 180px) minmax(0, 180px) auto;
}

.checkout-inline-note,
.checkout-field-note,
.checkout-kv-list__note {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.checkout-form-row__content input:not([type='checkbox']):not([type='radio']),
.checkout-form-row__content textarea,
.checkout-kv-list__inline input,
.checkout-form-row__content select {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid #e3e5e8;
  background: #ffffff;
  color: #222222;
  font-size: 14px;
  box-sizing: border-box;
}

.checkout-form-row__content textarea {
  min-height: 108px;
  padding-top: 12px;
  resize: vertical;
}

.checkout-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkout-inline--zip {
  max-width: 330px;
}

.checkout-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #222222;
  font-size: 14px;
}

.checkout-check--block {
  min-height: 40px;
}

.checkout-check input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: #111111;
}

.checkout-choice-row {
  display: grid;
  gap: 10px;
}

.checkout-choice-row > span {
  color: #111111;
  font-size: 14px;
  font-weight: 700;
}

.checkout-choice-group {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #222222;
  font-size: 14px;
}

.checkout-shipping-trigger {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--accent);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
  cursor: pointer;
}

.checkout-shipping-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: #666666;
  font-size: 12px;
  text-decoration: underline;
  cursor: pointer;
}

.checkout-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
}

.checkout-option-row.is-muted {
  color: #8f8f8f;
}

.checkout-option-row__main {
  flex: 1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.checkout-option-row__main--label {
  cursor: pointer;
}

.checkout-option-row__input {
  position: absolute;
  width: auto;
  opacity: 0;
  pointer-events: none;
}

.checkout-option-row__box {
  display: inline-grid;
  place-items: center;
  width: 14px;
  min-width: 14px;
  height: 14px;
  border: 1px solid #d1d5db;
  border-radius: 2px;
  background: #ffffff;
  color: transparent;
  font-size: 11px;
  line-height: 1;
}

.checkout-option-row__box.is-active {
  background: #167ddd;
  border-color: #167ddd;
  color: #ffffff;
}

.checkout-option-row__text {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
  word-break: keep-all;
}

.checkout-inline-link {
  border: 0;
  background: transparent;
  color: #666666;
  font-size: 13px;
  text-decoration: underline;
  cursor: pointer;
  white-space: nowrap;
}

.checkout-count-list {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.checkout-count-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #111111;
  font-size: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.checkout-side-block {
  border-top: 2px solid #111111;
  padding-top: 18px;
}

.checkout-kv-list {
  display: grid;
  gap: 0;
  border-top: 0;
}

.checkout-kv-list > div {
  min-height: 68px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ededed;
}

.checkout-kv-list__note {
  display: block;
  min-height: auto;
  padding: 14px 20px 16px;
}

.checkout-kv-list span {
  color: #444444;
  font-size: 14px;
  font-weight: 700;
}

.checkout-kv-list__inline > div {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  width: 100%;
  max-width: 306px;
}

.checkout-kv-list__inline input {
  width: 170px;
  max-width: 170px;
  min-height: 40px;
  height: 40px;
}

.checkout-kv-list__inline .checkout-outline-button.is-small {
  min-width: 126px;
  min-height: 40px;
  height: 40px;
  padding: 0 14px;
  white-space: nowrap;
}

.checkout-payment-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.checkout-payment-note {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.checkout-payment-button {
  min-height: 48px;
  border: 1px solid #d8dde5;
  background: #ffffff;
  color: #111111;
  display: grid;
  gap: 4px;
  align-content: center;
  justify-items: start;
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
}

.checkout-payment-button span {
  font-size: 15px;
  font-weight: 600;
}

.checkout-payment-button small {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.checkout-payment-button.is-active {
  border-color: #111827;
  background: #111827;
  color: #ffffff;
}

.checkout-payment-button.is-active small {
  color: rgba(255, 255, 255, 0.82);
}

.checkout-payment-button.is-disabled {
  cursor: not-allowed;
  opacity: 0.54;
}

@media (max-width: 1120px) {
  .checkout-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .checkout-item,
  .checkout-total-strip,
  .checkout-payment-grid,
  .checkout-form-row,
  .checkout-form-row__content--inline {
    grid-template-columns: 1fr;
  }

  .checkout-board__head {
    display: none;
  }

  .checkout-board__head span,
  .checkout-item__qty,
  .checkout-item__price,
  .checkout-item__shipping {
    text-align: left;
    justify-items: start;
  }

  .checkout-board__bundle,
  .checkout-inline,
  .checkout-kv-list__inline > div {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
