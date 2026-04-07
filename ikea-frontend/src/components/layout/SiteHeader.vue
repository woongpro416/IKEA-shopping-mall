<script setup>
import { computed, onMounted, ref, useTemplateRef, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { useAccountSession } from '../../composables/useAccountSession';
import { useHeaderMenu } from '../../composables/useHeaderMenu';
import { buildSearchPath, ROUTE_PATHS } from '../../constants/routes';
import { useAccountStore } from '../../stores/account';
import { useCartStore } from '../../stores/cart';
import { useCatalogStore } from '../../stores/catalog';
import { useHomeStore } from '../../stores/home';
import { hasAdminAccess } from '../../utils/accessControl';

const route = useRoute();
const router = useRouter();
const accountStore = useAccountStore();
const cartStore = useCartStore();
const catalogStore = useCatalogStore();
const homeStore = useHomeStore();
const headerElementRef = useTemplateRef('headerElementRef');
const searchKeyword = ref('');
const adminDashboardPath = ROUTE_PATHS.adminDashboard;
const { hydrateCurrentMember, loggedIn, submitLogout } = useAccountSession();
const { memberName, profileHydrated, profileRequested } = storeToRefs(accountStore);
const { cartItems } = storeToRefs(cartStore);
const { backendCategories } = storeToRefs(catalogStore);
const { mainTabs } = storeToRefs(homeStore);

const {
  activeCategory,
  activeCategoryId,
  activeTab,
  cartPath,
  isMenuOpen,
  memberLoginPath,
  memberMyPagePath,
  openMenu,
  pinMenuOpen,
  scheduleCloseMenu,
  setActiveCategory,
  handleSubmenuCategoryClick,
  handleSubmenuCardClick,
  handleHeaderTabClick,
} = useHeaderMenu(backendCategories);

const accountActionLabel = computed(() => (loggedIn.value ? '로그아웃' : '로그인'));
const myPageActionLabel = computed(() => (memberName.value ? `${memberName.value}님` : '마이'));
const showAdminAction = computed(() => hasAdminAccess(accountStore));
const cartQuantity = computed(() => (
  new Set(
    cartItems.value
      .map((item) => String(item?.productId ?? item?.id ?? '').trim())
      .filter(Boolean),
  ).size
));

function handleAccountClick() {
  if (loggedIn.value) {
    submitLogout();
    return;
  }

  router.push(memberLoginPath);
}

function handleMyPageClick() {
  router.push(memberMyPagePath);
}

function syncSearchKeywordFromRoute() {
  searchKeyword.value = String(route.query.q ?? '').trim();
}

function handleSearchSubmit() {
  router.push(buildSearchPath(searchKeyword.value));
}

function getHeaderElement() {
  return headerElementRef.value;
}

onMounted(() => {
  void catalogStore.ensureCatalogLoaded();
  void cartStore.ensureCartLoaded().catch(() => {});
  syncSearchKeywordFromRoute();

  if (!loggedIn.value || profileHydrated.value || profileRequested.value) {
    return;
  }

  hydrateCurrentMember({
    silent: true,
  });
});

watch(
  () => route.fullPath,
  () => {
    syncSearchKeywordFromRoute();
  },
);

defineExpose({
  getHeaderElement,
});
</script>

<template>
  <header ref="headerElementRef" class="hs-header" @mouseleave="scheduleCloseMenu">
    <div class="hs-header__inner">
      <RouterLink to="/" class="hs-header__logo" aria-label="홈으로 이동">
        <img src="/logo.png" alt="HOMiO" />
      </RouterLink>

      <nav class="hs-header__tabs">
        <button
          v-for="tab in mainTabs ?? []"
          :key="tab.id"
          class="hs-header__tab"
          :class="{
            'is-active':
              tab.id === 'customer-service'
                ? activeTab === tab.id
                : activeTab === tab.id && isMenuOpen,
          }"
          type="button"
          @mouseenter="tab.id !== 'customer-service' && openMenu(tab.id)"
          @focus="tab.id !== 'customer-service' && openMenu(tab.id)"
          @click="handleHeaderTabClick(tab)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <form class="hs-header__search" @submit.prevent="handleSearchSubmit">
        <input
          v-model.trim="searchKeyword"
          type="text"
          placeholder="어떤 상품을 찾고 계신가요?"
          aria-label="상품 검색"
        />
        <button type="submit" class="hs-header__search-button" aria-label="상품 검색">
          <svg viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8" />
            <path
              d="M20 20L16.65 16.65"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </form>

      <div class="hs-header__utils">
        <button class="hs-util" type="button" @click="handleAccountClick">
          <svg viewBox="0 0 24 24" fill="none">
            <path d="M10 6H20V18H10" stroke="currentColor" stroke-width="1.8" />
            <path d="M13 12H3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path
              d="M7 8L3 12L7 16"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span>{{ accountActionLabel }}</span>
        </button>

        <button class="hs-util" type="button" @click="router.push(cartPath)">
          <span class="hs-util__icon">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M7 8H17L19 20H5L7 8Z" stroke="currentColor" stroke-width="1.8" />
              <path
                d="M9 8V6.8C9 4.7 10.34 3.5 12 3.5C13.66 3.5 15 4.7 15 6.8V8"
                stroke="currentColor"
                stroke-width="1.8"
              />
            </svg>
            <span v-if="cartQuantity > 0" class="hs-util__badge">{{ cartQuantity }}</span>
          </span>
          <span>장바구니</span>
        </button>

        <button class="hs-util" type="button" @click="handleMyPageClick">
          <svg viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.8" />
            <path
              d="M5.5 20C6.1 16.9 8.47 15 12 15C15.53 15 17.9 16.9 18.5 20"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
            />
          </svg>
          <span>{{ myPageActionLabel }}</span>
        </button>

        <button v-if="showAdminAction" class="hs-util" type="button" @click="router.push(adminDashboardPath)">
          <svg viewBox="0 0 24 24" fill="none">
            <rect
              x="4.5"
              y="4.5"
              width="15"
              height="15"
              rx="2.8"
              stroke="currentColor"
              stroke-width="1.8"
            />
            <path d="M8 9.5H16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M8 14H12.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <path d="M15.5 14H16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
          <span>관리자</span>
        </button>
      </div>
    </div>

    <div v-show="isMenuOpen" class="hs-submenu" @mouseenter="pinMenuOpen">
      <div class="hs-submenu__inner">
        <div class="hs-submenu__category-list">
          <button
            v-for="category in backendCategories ?? []"
            :key="category.id"
            class="hs-submenu__category"
            :class="{ 'is-active': activeCategoryId === category.id }"
            type="button"
            @mouseenter="setActiveCategory(category.id)"
            @focus="setActiveCategory(category.id)"
            @click="handleSubmenuCategoryClick(category)"
          >
            {{ category.label }}
          </button>
        </div>

        <div
          class="hs-submenu__sofa"
          :style="{ '--submenu-card-count': Math.min(activeCategory?.cards?.length || 1, 6) }"
        >
          <button
            v-for="item in activeCategory?.cards ?? []"
            :key="item.slug"
            class="hs-submenu__sofa-card"
            type="button"
            @click="handleSubmenuCardClick(activeCategory, item)"
          >
            <span class="hs-submenu__sofa-thumb">
              <img :src="item.image" :alt="item.label" />
            </span>
            <strong>{{ item.label }}</strong>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.hs-header {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid var(--hs-line, #e5e7eb);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
}

.hs-header__inner,
.hs-submenu__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
}

.hs-header__inner {
  display: grid;
  grid-template-columns: 176px auto minmax(320px, 1fr) auto;
  align-items: center;
  gap: 22px;
  min-height: 74px;
}

.hs-header__logo {
  display: flex;
  align-items: center;
  width: 160px;
  height: 36px;
  overflow: hidden;
}

.hs-header__logo img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: left center;
}

.hs-header__tabs {
  display: flex;
  align-items: center;
  gap: 28px;
}

.hs-header__tab {
  border: 0;
  background: transparent;
  padding: 0;
  font: inherit;
  font-size: 18px;
  color: var(--hs-ink, #111827);
  cursor: pointer;
}

.hs-header__tab.is-active {
  color: var(--hs-blue, #0058a3);
  font-weight: 700;
}

.hs-header__search {
  position: relative;
}

.hs-header__search input {
  width: 100%;
  height: 46px;
  border: 1px solid #d8dde5;
  border-radius: 10px;
  background: #ffffff;
  padding: 0 50px 0 16px;
  color: var(--hs-ink, #111827);
  font: inherit;
}

.hs-header__search-button {
  position: absolute;
  top: 50%;
  right: 16px;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hs-ink, #111827);
  transform: translateY(-50%);
  cursor: pointer;
  transition: none;
}

.hs-header__search-button:hover,
.hs-header__search-button:focus-visible {
  background: transparent;
  box-shadow: none;
  transform: translateY(-50%);
}

.hs-header__search-button svg,
.hs-util svg {
  width: 100%;
  height: 100%;
}

.hs-header__utils {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hs-util {
  display: grid;
  justify-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: var(--hs-ink, #111827);
  font: inherit;
  cursor: pointer;
}

.hs-util__icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hs-util svg {
  width: 24px;
  height: 24px;
}

.hs-util > span {
  font-size: 13px;
}

.hs-util__badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--hs-badge-danger);
  color: var(--hs-badge-danger-ink);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.hs-submenu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 41;
  border-top: 1px solid #eef2f7;
  border-bottom: 1px solid #eef2f7;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}

.hs-submenu__inner {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 28px;
  padding: 24px 0;
}

.hs-submenu__category-list {
  display: grid;
  gap: 8px;
}

.hs-submenu__category {
  min-height: 48px;
  border: 1px solid var(--hs-line, #e5e7eb);
  border-radius: 12px;
  background: #ffffff;
  color: var(--hs-ink, #111827);
  text-align: left;
  padding: 0 16px;
  font: inherit;
  cursor: pointer;
}

.hs-submenu__category.is-active {
  border-color: var(--hs-blue, #0058a3);
  background: #f0f6ff;
  color: var(--hs-blue, #0058a3);
  font-weight: 700;
}

.hs-submenu__sofa {
  display: grid;
  grid-template-columns: repeat(var(--submenu-card-count, 6), minmax(0, 1fr));
  gap: 14px;
  align-content: start;
}

.hs-submenu__sofa-card {
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--hs-line, #e5e7eb);
  border-radius: 10px;
  background: #ffffff;
  text-align: center;
  cursor: pointer;
}

.hs-submenu__sofa-card:hover {
  border-color: var(--hs-blue, #0058a3);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.hs-submenu__sofa-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  overflow: hidden;
}

.hs-submenu__sofa-thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.hs-submenu__sofa-card strong {
  font-size: 14px;
  line-height: 1.35;
}

@media (max-width: 1180px) {
  .hs-header__inner {
    grid-template-columns: 176px 1fr auto;
    grid-template-areas:
      'logo tabs tabs'
      'search search utils';
    row-gap: 14px;
    padding-block: 14px;
  }

  .hs-header__logo {
    grid-area: logo;
  }

  .hs-header__tabs {
    grid-area: tabs;
    justify-content: flex-end;
  }

  .hs-header__search {
    grid-area: search;
  }

  .hs-header__utils {
    grid-area: utils;
  }

  .hs-submenu__inner {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hs-header__inner,
  .hs-submenu__inner {
    width: calc(100% - 24px);
  }

  .hs-header__inner {
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      'logo utils'
      'tabs tabs'
      'search search';
    align-items: center;
    column-gap: 12px;
    row-gap: 10px;
    padding-block: 12px;
  }

  .hs-header__logo {
    width: 132px;
    height: 30px;
  }

  .hs-header__tabs,
  .hs-header__search {
    width: 100%;
  }

  .hs-header__tabs {
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 20px;
  }

  .hs-header__tab {
    font-size: 15px;
  }

  .hs-header__search input {
    height: 44px;
  }

  .hs-header__utils {
    width: auto;
    justify-content: flex-end;
    flex-wrap: nowrap;
    gap: 16px;
  }

  .hs-util {
    gap: 3px;
  }

  .hs-util svg {
    width: 22px;
    height: 22px;
  }

  .hs-util > span {
    font-size: 12px;
  }

  .hs-submenu__sofa {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 420px) {
  .hs-header__tabs {
    gap: 16px;
  }

  .hs-header__tab {
    font-size: 14px;
  }

  .hs-header__utils {
    gap: 12px;
  }
}
</style>
