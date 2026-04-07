<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteChrome from '../components/layout/SiteChrome.vue';
import { ROUTE_PATHS } from '../constants/routes';
import { useAccountStore } from '../stores/account';
import { hasAdminAccess, hasAuthenticatedSession } from '../utils/accessControl';

const route = useRoute();
const router = useRouter();
const accountStore = useAccountStore();

accountStore.hydrate();

const attemptedPath = computed(() => String(route.query.from ?? '').trim());
const accessDeniedReason = computed(() => String(route.query.reason ?? '').trim());
const isAuthenticated = computed(() => hasAuthenticatedSession(accountStore));
const isAdmin = computed(() => hasAdminAccess(accountStore));
const title = computed(() => {
  if (accessDeniedReason.value === 'admin-required') {
    return '접근 권한이 없습니다.';
  }

  return '요청한 페이지로 이동할 수 없습니다.';
});
const description = computed(() => {
  if (accessDeniedReason.value === 'admin-required') {
    return '이 화면은 관리자 계정만 사용할 수 있습니다. 현재 계정으로는 접근할 수 없습니다.';
  }

  return '권한 또는 경로를 다시 확인해 주세요.';
});

const primaryActionLabel = computed(() => {
  if (accessDeniedReason.value === 'admin-required') {
    if (isAdmin.value) {
      return '관리자 홈';
    }

    if (isAuthenticated.value) {
      return '마이페이지';
    }
  }

  if (!isAuthenticated.value) {
    return '로그인';
  }

  return '홈으로 이동';
});

function goHome() {
  router.push(ROUTE_PATHS.home);
}

function goPrimary() {
  if (accessDeniedReason.value === 'admin-required') {
    if (isAdmin.value) {
      router.push(ROUTE_PATHS.adminDashboard);
      return;
    }

    if (isAuthenticated.value) {
      router.push(ROUTE_PATHS.memberMyPage);
      return;
    }
  }

  if (!isAuthenticated.value) {
    router.push({
      path: ROUTE_PATHS.memberLogin,
      query: {
        redirect: attemptedPath.value || ROUTE_PATHS.home,
      },
    });
    return;
  }

  goHome();
}

function goBack() {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back();
    return;
  }

  goHome();
}
</script>

<template>
  <SiteChrome>
    <main class="access-denied-page">
      <div class="access-denied-page__inner">
        <section class="access-denied-card" aria-labelledby="access-denied-title">
          <p class="access-denied-card__eyebrow">HOMiO</p>
          <h1 id="access-denied-title" class="access-denied-card__title">{{ title }}</h1>
          <p class="access-denied-card__description">{{ description }}</p>
          <p v-if="attemptedPath" class="access-denied-card__path">
            요청 경로: <strong>{{ attemptedPath }}</strong>
          </p>

          <div class="access-denied-card__actions">
            <button
              type="button"
              class="access-denied-card__button access-denied-card__button--primary"
              @click="goPrimary"
            >
              {{ primaryActionLabel }}
            </button>
            <button type="button" class="access-denied-card__button" @click="goBack">
              이전 페이지
            </button>
          </div>
        </section>
      </div>
    </main>
  </SiteChrome>
</template>

<style scoped>
.access-denied-page {
  background: #ffffff;
}

.access-denied-page__inner {
  width: min(1280px, calc(100% - 40px));
  min-height: 640px;
  margin: 0 auto;
  padding: 80px 0 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.access-denied-card {
  width: min(100%, 640px);
  padding: 52px 44px;
  border: 1px solid #e5e5e5;
  background: #fafafa;
  text-align: center;
}

.access-denied-card__eyebrow {
  margin: 0 0 18px;
  color: #777777;
  font-size: 13px;
  letter-spacing: 0.16em;
}

.access-denied-card__title {
  margin: 0;
  color: #111111;
  font-size: 36px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.access-denied-card__description {
  margin: 18px 0 0;
  color: #4b4b4b;
  font-size: 16px;
  line-height: 1.8;
}

.access-denied-card__path {
  margin: 18px 0 0;
  color: #666666;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-all;
}

.access-denied-card__path strong {
  color: #111111;
  font-weight: 700;
}

.access-denied-card__actions {
  margin-top: 34px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.access-denied-card__button {
  min-width: 160px;
  height: 48px;
  padding: 0 20px;
  border: 1px solid #cfcfcf;
  background: #ffffff;
  color: #111111;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.access-denied-card__button--primary {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

@media (max-width: 720px) {
  .access-denied-page__inner {
    width: min(100%, calc(100% - 24px));
    min-height: 0;
    padding: 48px 0 72px;
  }

  .access-denied-card {
    padding: 36px 20px;
  }

  .access-denied-card__title {
    font-size: 28px;
  }

  .access-denied-card__actions {
    flex-direction: column;
  }

  .access-denied-card__button {
    width: 100%;
  }
}
</style>
