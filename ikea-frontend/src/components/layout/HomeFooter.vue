<script setup>
import {
  computed,
  onBeforeUnmount,
  shallowRef,
  watch,
} from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { ROUTE_PATHS } from '../../constants/routes';
import { useHomeStore } from '../../stores/home';

const router = useRouter();
const homeStore = useHomeStore();
const {
  footerInfoLines,
  footerLinks,
  footerNotice,
  footerSupportCards,
} = storeToRefs(homeStore);
const isEmailRefusalOpen = shallowRef(false);

const footerLinkTargets = computed(() => ({
  이용약관: ROUTE_PATHS.policyTerms,
  개인정보처리방침: ROUTE_PATHS.policyPrivacy,
  '위치정보 이용약관': ROUTE_PATHS.policyLocation,
  공지사항: ROUTE_PATHS.customerServiceNotice,
}));

function resolveFooterLink(linkLabel) {
  return footerLinkTargets.value[linkLabel] ?? '';
}

function handleFooterLinkClick(linkLabel) {
  if (linkLabel === '이메일 무단수집 거부') {
    isEmailRefusalOpen.value = true;
  }
}

function closeEmailRefusalPopup() {
  isEmailRefusalOpen.value = false;
}

function handleSupportCardClick(card) {
  if (!card?.title) {
    return;
  }

  if (card.title === '상품 문의') {
    router.push(ROUTE_PATHS.customerServiceQnaWrite);
    return;
  }

  router.push(ROUTE_PATHS.customerServiceQna);
}

function handleDocumentKeydown(event) {
  if (event.key === 'Escape') {
    closeEmailRefusalPopup();
  }
}

watch(isEmailRefusalOpen, (isOpen) => {
  if (typeof document === 'undefined') {
    return;
  }

  document.body.style.overflow = isOpen ? 'hidden' : '';

  if (isOpen) {
    window.addEventListener('keydown', handleDocumentKeydown);
    return;
  }

  window.removeEventListener('keydown', handleDocumentKeydown);
});

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.body.style.overflow = '';
  }

  window.removeEventListener('keydown', handleDocumentKeydown);
});
</script>

<template>
  <footer class="hs-footer">
    <div class="hs-footer__inner">
      <div class="hs-footer__top">
        <nav class="hs-footer__links">
          <template v-for="link in footerLinks" :key="link">
            <RouterLink v-if="resolveFooterLink(link)" :to="resolveFooterLink(link)">{{ link }}</RouterLink>
            <button
              v-else
              type="button"
              class="hs-footer__link-button"
              @click="handleFooterLinkClick(link)"
            >
              {{ link }}
            </button>
          </template>
        </nav>
      </div>

      <section class="hs-footer__service">
        <div class="hs-footer__service-grid">
          <div class="hs-footer__brand">
            <img class="hs-footer__brand-logo" src="/logo.png" alt="HOMiO" />
            <strong>고객센터 이용안내</strong>
            <p>평일 09:00 - 18:00, 토요일 09:00 - 13:00</p>
            <p>(일요일 및 공휴일 휴무)</p>
          </div>

          <article
            v-for="card in footerSupportCards ?? []"
            :key="card.title"
            class="hs-support-inline"
          >
            <div class="hs-support-inline__head">
              <strong>{{ card.title }}</strong>
              <span>|</span>
            </div>
            <p>{{ card.description }}</p>
            <b>{{ card.phone }}</b>
            <button type="button" @click="handleSupportCardClick(card)">{{ card.cta }}</button>
          </article>

          <div class="hs-footer__qr">
            <div class="hs-footer__qr-box">QR</div>
            <span>QR 보기</span>
          </div>
        </div>
      </section>

      <section class="hs-footer__company">
        <div class="hs-footer__company-lines">
          <p v-for="line in footerInfoLines ?? []" :key="line">{{ line }}</p>
        </div>
      </section>

      <p class="hs-footer__escrow">
        KEB하나은행 구매안전서비스 채무지급보증 서비스 가입사실 확인
      </p>
      <p class="hs-footer__copy">(C) HOMiO. All rights reserved.</p>
      <p v-if="footerNotice" class="hs-footer__notice">{{ footerNotice }}</p>
    </div>

    <div
      v-if="isEmailRefusalOpen"
      class="hs-footer-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="footer-email-refusal-title"
      @click.self="closeEmailRefusalPopup"
    >
      <article class="hs-footer-modal__panel">
        <div class="hs-footer-modal__head">
          <h2 id="footer-email-refusal-title">이메일 무단수집 거부</h2>
          <button type="button" class="hs-footer-modal__close" aria-label="팝업 닫기" @click="closeEmailRefusalPopup">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M6 6L18 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
              <path d="M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
          </button>
        </div>
        <div class="hs-footer-modal__body">
          <p>HOMiO는 사전 동의 없이 게시된 이메일 주소를 수집하는 행위를 거부합니다.</p>
          <p>
            본 웹사이트에 게시된 이메일 주소가 전자우편 수집 프로그램이나 그 밖의 기술적 장치를 이용하여
            무단으로 수집되는 것을 거부합니다.
          </p>
          <p>
            이를 위반할 경우 정보통신망 이용촉진 및 정보보호 등에 관한 법률에 따라 처벌될 수 있습니다.
          </p>
        </div>
      </article>
    </div>
  </footer>
</template>

<style scoped>
.hs-footer {
  border-top: 1px solid var(--hs-line, #e5e7eb);
  background: var(--hs-soft, #f5f7fa);
  padding: 18px 0 22px;
}

.hs-footer__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  display: grid;
  gap: 14px;
}

.hs-footer__top {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 18px;
}

.hs-footer__links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hs-footer__links a,
.hs-footer__link-button {
  color: var(--hs-ink, #111827);
  font-size: 13px;
}

.hs-footer__link-button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font-family: inherit;
  font-size: 13px;
  font-weight: inherit;
  line-height: 1.55;
  letter-spacing: inherit;
  vertical-align: baseline;
  cursor: pointer;
  appearance: none;
  transform: none !important;
  transition: none !important;
}

.hs-footer__link-button:hover,
.hs-footer__link-button:focus-visible,
.hs-footer__link-button:active {
  transform: none !important;
}

.hs-footer__service-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.1fr) repeat(2, minmax(180px, 1fr)) 108px;
  gap: 14px;
  align-items: start;
}

.hs-footer__brand {
  display: grid;
  gap: 4px;
  align-content: start;
}

.hs-footer__brand-logo {
  width: 148px;
  height: auto;
}

.hs-footer__brand strong,
.hs-support-inline__head strong {
  font-size: 16px;
  color: var(--hs-ink, #111827);
}

.hs-footer__brand p,
.hs-support-inline p,
.hs-footer__company p,
.hs-footer__escrow,
.hs-footer__copy,
.hs-footer__notice {
  margin: 0;
  color: var(--hs-muted, #6b7280);
  font-size: 13px;
  line-height: 1.45;
}

.hs-support-inline {
  display: grid;
  gap: 6px;
  align-content: start;
}

.hs-support-inline__head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hs-support-inline b {
  color: var(--hs-ink, #111827);
  font-size: 15px;
  font-weight: 600;
}

.hs-support-inline button {
  width: fit-content;
  min-height: 36px;
  padding: 0 16px;
  border: 1px solid #d8dde5;
  background: #ffffff;
  color: var(--hs-ink, #111827);
  cursor: pointer;
  font-size: 13px;
}

.hs-footer__qr {
  display: grid;
  justify-items: center;
  gap: 6px;
}

.hs-footer__qr-box {
  width: 92px;
  height: 92px;
  border: 1px solid #d8dde5;
  background:
    linear-gradient(90deg, #111111 10px, transparent 10px) 0 0 / 28px 28px,
    linear-gradient(#111111 10px, transparent 10px) 0 0 / 28px 28px,
    linear-gradient(90deg, transparent 18px, #111111 18px) 0 0 / 28px 28px,
    linear-gradient(transparent 18px, #111111 18px) 0 0 / 28px 28px,
    #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #111111;
  font-size: 15px;
  font-weight: 700;
}

.hs-footer__company {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 18px;
  padding-top: 8px;
  border-top: 1px solid #e4e8ef;
}

.hs-footer__company-lines {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
}

.hs-footer__company-lines p {
  position: relative;
  padding-right: 14px;
}

.hs-footer__company-lines p::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 0;
  width: 1px;
  height: 12px;
  background: #d8dde5;
  transform: translateY(-50%);
}

.hs-footer__company-lines p:last-child::after {
  display: none;
}

.hs-footer__escrow,
.hs-footer__copy,
.hs-footer__notice {
  font-size: 12px;
}

.hs-footer-modal {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 17, 17, 0.42);
}

.hs-footer-modal__panel {
  width: min(100%, 372px);
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
}

.hs-footer-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 28px 18px;
}

.hs-footer-modal__head h2 {
  margin: 0;
  color: #111111;
  font-size: 16px;
  font-weight: 700;
}

.hs-footer-modal__close {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #111111;
  cursor: pointer;
}

.hs-footer-modal__close svg {
  width: 100%;
  height: 100%;
}

.hs-footer-modal__body {
  padding: 0 28px 28px;
  border-top: 1px solid #111111;
}

.hs-footer-modal__body p {
  margin: 18px 0 0;
  color: #333333;
  font-size: 14px;
  line-height: 1.9;
}

.hs-footer-modal__body p:first-child {
  margin-top: 20px;
}

@media (max-width: 1180px) {
  .hs-footer__service-grid {
    grid-template-columns: 1fr;
  }

  .hs-footer__company {
    display: grid;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hs-footer__inner {
    width: calc(100% - 24px);
  }

  .hs-footer__top {
    align-items: flex-start;
  }

  .hs-footer__links {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px 14px;
    width: 100%;
  }

  .hs-footer__brand-logo {
    width: 124px;
  }

  .hs-support-inline {
    gap: 8px;
    padding: 16px;
    border: 1px solid #d8dde5;
    background: #ffffff;
  }

  .hs-footer__qr {
    display: none;
  }

  .hs-footer__company-lines {
    display: grid;
    gap: 4px;
  }

  .hs-footer__company-lines p {
    padding-right: 0;
  }

  .hs-footer__company-lines p::after {
    display: none;
  }

  .hs-footer-modal {
    padding: 16px;
  }

  .hs-footer-modal__head,
  .hs-footer-modal__body {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
