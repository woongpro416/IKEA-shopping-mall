<script setup>
import { computed, onMounted, reactive, shallowRef, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAccountSession } from '../composables/useAccountSession';
import SiteChrome from '../components/layout/SiteChrome.vue';
import { ROUTE_PATHS } from '../constants/routes';
import {
  buildGuestOrderLookupQuery,
  validateGuestOrderLookupForm,
} from '../utils/guestOrderLookup';

const route = useRoute();
const router = useRouter();
const LOGIN_ID_STORAGE_KEY = 'homio-saved-login-id';
const inquiryType = shallowRef('phone');
const rememberId = shallowRef(false);
const guestLookupError = shallowRef('');
const loginValidationError = shallowRef('');
const guestLookupForm = reactive({
  buyerName: '',
  orderNumber: '',
  phoneNumber: '',
});
const {
  loginError,
  loginForm,
  loginSubmitting,
  submitLogin,
} = useAccountSession();

const inquiryPlaceholder = computed(() => (
  inquiryType.value === 'order'
    ? '예: ORDER_20260406_ab12cd34'
    : '휴대전화번호를 입력해 주세요.'
));
const memberLoginError = computed(() => loginValidationError.value || loginError.value);

function validateLoginForm() {
  const normalizedLoginId = String(loginForm.loginId ?? '').trim();
  const normalizedPassword = String(loginForm.password ?? '');

  if (!normalizedLoginId && !normalizedPassword) {
    return '아이디와 비밀번호를 입력해 주세요.';
  }

  if (!normalizedLoginId) {
    return '아이디 또는 이메일을 입력해 주세요.';
  }

  if (!normalizedPassword) {
    return '비밀번호를 입력해 주세요.';
  }

  return '';
}

function clearLoginValidationError() {
  loginValidationError.value = '';
}

async function handleSubmitLogin() {
  const validationMessage = validateLoginForm();

  if (validationMessage) {
    loginValidationError.value = validationMessage;
    return;
  }

  loginValidationError.value = '';
  await submitLogin();
}

function clearGuestLookupError() {
  guestLookupError.value = '';
}

function canUseLocalStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

function syncSavedLoginId() {
  if (!canUseLocalStorage()) {
    return;
  }

  if (!rememberId.value) {
    window.localStorage.removeItem(LOGIN_ID_STORAGE_KEY);
    return;
  }

  const normalizedLoginId = String(loginForm.loginId ?? '').trim();

  if (!normalizedLoginId) {
    window.localStorage.removeItem(LOGIN_ID_STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(LOGIN_ID_STORAGE_KEY, normalizedLoginId);
}

function openAccountRecoverySupport() {
  router.push(ROUTE_PATHS.customerServiceQna);
}

function openGuestLookup() {
  const validationMessage = validateGuestOrderLookupForm({
    buyerName: guestLookupForm.buyerName,
    inquiryType: inquiryType.value,
    orderNumber: guestLookupForm.orderNumber,
    phoneNumber: guestLookupForm.phoneNumber,
  });

  if (validationMessage) {
    guestLookupError.value = validationMessage;
    return;
  }

  guestLookupError.value = '';

  router.push({
    path: ROUTE_PATHS.guestOrderLookup,
    query: buildGuestOrderLookupQuery({
      buyerName: guestLookupForm.buyerName,
      inquiryType: inquiryType.value,
      orderNumber: guestLookupForm.orderNumber,
      phoneNumber: guestLookupForm.phoneNumber,
    }),
  });
}

onMounted(() => {
  if (canUseLocalStorage()) {
    const savedLoginId = String(window.localStorage.getItem(LOGIN_ID_STORAGE_KEY) ?? '').trim();

    if (savedLoginId) {
      loginForm.loginId = savedLoginId;
      rememberId.value = true;
    }
  }

  const prefetchedLoginId = String(route.query.loginId ?? '').trim();

  if (prefetchedLoginId) {
    loginForm.loginId = prefetchedLoginId;
  }
});

watch(rememberId, () => {
  syncSavedLoginId();
});

watch(
  () => loginForm.loginId,
  () => {
    syncSavedLoginId();
  },
);
</script>

<template>
  <SiteChrome>
    <main class="login-page">
      <div class="login-page__inner">
        <div class="login-breadcrumb">
          <RouterLink to="/" class="login-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>></span>
          <span>로그인</span>
        </div>

        <section class="login-panel">
          <h1>로그인</h1>

          <div class="login-columns">
            <section class="login-box login-box--member">
              <h2>회원 로그인</h2>
              <label class="login-field">
                <span>아이디 또는 이메일</span>
                <input
                  v-model.trim="loginForm.loginId"
                  type="text"
                  placeholder="아이디 또는 이메일 입력"
                  autocomplete="username"
                  @input="clearLoginValidationError"
                />
              </label>

              <label class="login-field login-field--query">
                <span>비밀번호</span>
                <input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="비밀번호"
                  autocomplete="current-password"
                  @input="clearLoginValidationError"
                  @keydown.enter="handleSubmitLogin"
                />
              </label>

              <div class="login-box__support login-box__support--member">
                <label class="login-check login-check--small">
                  <input v-model="rememberId" type="checkbox" />
                  <span>아이디 저장</span>
                </label>
                <p class="login-note">(개인정보 보호를 위해 개인 PC에서만 이용해 주세요.)</p>
                <p v-if="memberLoginError" class="login-status login-status--error">{{ memberLoginError }}</p>
              </div>

              <button class="login-primary" type="button" :disabled="loginSubmitting" @click="handleSubmitLogin">
                {{ loginSubmitting ? '로그인 중...' : '로그인' }}
              </button>

              <div class="login-actions">
                <button type="button" @click="openAccountRecoverySupport">아이디/비밀번호 찾기</button>
                <button type="button" @click="router.push(ROUTE_PATHS.memberJoin)">회원가입</button>
              </div>
            </section>

            <section class="login-box login-box--guest">
              <h2>비회원 주문/배송 조회</h2>

              <label class="login-field">
                <span>이름</span>
                <input v-model.trim="guestLookupForm.buyerName" type="text" @input="clearGuestLookupError" />
              </label>

              <div class="login-box__query">
                <div class="login-radio-row">
                  <label class="login-radio">
                    <input v-model="inquiryType" type="radio" value="order" @change="clearGuestLookupError" />
                    <span>주문번호</span>
                  </label>
                  <label class="login-radio">
                    <input v-model="inquiryType" type="radio" value="phone" @change="clearGuestLookupError" />
                    <span>휴대전화번호</span>
                  </label>
                </div>

                <div v-if="inquiryType === 'phone'" class="login-inline login-inline--phone">
                  <input
                    v-model.trim="guestLookupForm.phoneNumber"
                    type="text"
                    :placeholder="inquiryPlaceholder"
                    @input="clearGuestLookupError"
                  />
                  <button type="button" @click="openGuestLookup">조회 페이지</button>
                </div>
                <div v-else class="login-inline login-inline--order">
                  <input
                    v-model.trim="guestLookupForm.orderNumber"
                    type="text"
                    :placeholder="inquiryPlaceholder"
                    @input="clearGuestLookupError"
                  />
                </div>
              </div>

              <div class="login-box__support login-box__support--guest">
                <p class="login-help">
                  주문번호를 찾을 수 없는 경우,<br />
                  HOMiO 고객센터 1688-4945로 문의해 주세요.
                </p>
                <p class="login-help">주문번호에는 밑줄(_)이 포함될 수 있습니다.</p>
                <p v-if="guestLookupError" class="login-status login-status--error">{{ guestLookupError }}</p>
              </div>

              <button class="login-primary" type="button" @click="openGuestLookup">조회</button>
            </section>
          </div>
        </section>
      </div>
    </main>
  </SiteChrome>
</template>

<style scoped>
.login-page {
  background: #ffffff;
}

.login-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 26px 0 110px;
}

.login-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #909090;
  font-size: 13px;
  line-height: 1;
}

.login-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.login-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.login-panel {
  padding-top: 54px;
}

.login-panel h1 {
  margin: 0;
  text-align: center;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #111111;
}

.login-columns {
  position: relative;
  max-width: 856px;
  margin: 62px auto 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  column-gap: 76px;
  align-items: start;
}

.login-columns::before {
  content: '';
  position: absolute;
  top: 4px;
  left: 50%;
  width: 1px;
  height: 352px;
  transform: translateX(-0.5px);
  background: #e6e6e6;
}

.login-box {
  width: 100%;
  max-width: 390px;
  display: grid;
  grid-template-rows: auto auto 90px 106px 50px auto;
  align-content: start;
}

.login-box--member {
  justify-self: start;
}

.login-box--guest {
  justify-self: end;
}

.login-box h2 {
  margin: 0 0 36px;
  text-align: center;
  font-size: 18px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #111111;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 22px;
}

.login-field--query {
  min-height: 90px;
  margin-bottom: 0;
}

.login-field > span {
  font-size: 15px;
  line-height: 1.4;
  color: #676767;
}

.login-field input,
.login-inline input {
  width: 100%;
  height: 48px;
  border: 1px solid #dddddd;
  background: #ffffff;
  padding: 0 14px;
  box-sizing: border-box;
  font-size: 14px;
  color: #111111;
  outline: none;
}

.login-field input::placeholder,
.login-inline input::placeholder {
  color: #b9b9b9;
}

.login-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #222222;
}

.login-check--small {
  margin-top: 2px;
  font-size: 14px;
}

.login-check input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: #111111;
}

.login-note {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8b8b8b;
}

.login-status {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.login-status--error {
  color: #c62828;
}

.login-primary {
  width: 100%;
  height: 50px;
  border: 0;
  border-radius: 999px;
  background: #0b0b0b;
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  align-self: start;
}

.login-primary:disabled {
  cursor: wait;
  opacity: 0.72;
}

.login-box__query {
  min-height: 90px;
  margin: 0;
}

.login-box__support {
  min-height: 106px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.login-box__support--member {
  margin-top: 0;
}

.login-box__support--guest {
  margin-top: 0;
}

.login-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

.login-actions button {
  flex: 1;
  height: 42px;
  border: 1px solid #8d8d8d;
  border-radius: 999px;
  background: #ffffff;
  color: #222222;
  font-size: 14px;
  cursor: pointer;
}

.login-radio-row {
  display: flex;
  align-items: center;
  gap: 24px;
  margin: 0 0 10px;
  min-height: 21px;
}

.login-radio {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #222222;
}

.login-radio input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: #111111;
}

.login-inline {
  display: flex;
  gap: 10px;
}

.login-inline--phone input {
  min-width: 0;
}

.login-inline--phone button {
  flex: 0 0 120px;
  height: 48px;
  border: 1px solid #555555;
  background: #ffffff;
  color: #111111;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.login-inline--order input {
  width: 100%;
}

.login-help {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: #8b8b8b;
}

@media (max-width: 980px) {
  .login-columns {
    grid-template-columns: 1fr;
    row-gap: 56px;
  }

  .login-columns::before {
    display: none;
  }

  .login-box {
    max-width: none;
  }

  .login-box--guest,
  .login-box--member {
    justify-self: stretch;
  }
}
</style>
