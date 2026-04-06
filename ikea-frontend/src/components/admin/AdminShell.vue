<script setup>
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import { ADMIN_NAV_ITEMS, resolveAdminNavItem } from '../../constants/adminNavigation';
import { useAccountStore } from '../../stores/account';
import SiteChrome from '../layout/SiteChrome.vue';

defineOptions({
  name: 'AdminShell',
});

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
});

const route = useRoute();
const accountStore = useAccountStore();
const { memberName, loginId } = storeToRefs(accountStore);

const activeNav = computed(() => resolveAdminNavItem(route.path));
const operatorLabel = computed(() => memberName.value || loginId.value || '운영 관리자');
</script>

<template>
  <SiteChrome>
    <main class="admin-page">
      <div class="admin-page__inner">
        <div class="admin-breadcrumb">
          <RouterLink to="/" class="admin-breadcrumb__home" aria-label="홈으로 이동">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 10.5L12 4L20 10.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M7 9.8V19H17V9.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>
          <span>/</span>
          <span>관리자</span>
          <span>/</span>
          <span>{{ activeNav.label }}</span>
        </div>

        <section class="admin-layout">
          <aside class="admin-sidebar">
            <h1>관리자</h1>

            <nav class="admin-sidebar__nav" aria-label="관리자 메뉴">
              <RouterLink
                v-for="item in ADMIN_NAV_ITEMS"
                :key="item.id"
                :to="item.to"
                class="admin-sidebar__link"
                :class="{ 'is-active': activeNav.id === item.id }"
              >
                <strong>{{ item.label }}</strong>
              </RouterLink>
            </nav>

            <div class="admin-sidebar__meta">
              <article>
                <span>운영자</span>
                <strong>{{ operatorLabel }}</strong>
              </article>
              <article>
                <span>현재 메뉴</span>
                <strong>{{ activeNav.label }}</strong>
              </article>
            </div>
          </aside>

          <section class="admin-content">
            <header class="admin-content__header">
              <div class="admin-content__copy">
                <h2>{{ props.title }}</h2>
                <p v-if="props.description">{{ props.description }}</p>
              </div>
              <div v-if="$slots.action" class="admin-content__action">
                <slot name="action" />
              </div>
            </header>

            <section class="admin-content__body">
              <slot />
            </section>
          </section>
        </section>
      </div>
    </main>
  </SiteChrome>
</template>

<style scoped>
.admin-page {
  background: #ffffff;
}

.admin-page__inner {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 28px 0 96px;
}

.admin-breadcrumb {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  color: #8f8f8f;
  font-size: 13px;
  line-height: 1;
}

.admin-breadcrumb__home {
  display: inline-flex;
  width: 14px;
  height: 14px;
  color: #8f8f8f;
}

.admin-breadcrumb__home svg {
  width: 100%;
  height: 100%;
}

.admin-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 64px;
  margin-top: 40px;
}

.admin-sidebar {
  align-self: start;
}

.admin-sidebar h1 {
  margin: 0 0 28px;
  color: #111111;
  font-size: 36px;
  line-height: 1.2;
  font-weight: 700;
}

.admin-sidebar__nav {
  display: grid;
  border-top: 2px solid #111111;
}

.admin-sidebar__link {
  display: flex;
  align-items: center;
  min-height: 56px;
  border-bottom: 1px solid #e6e6e6;
  color: #222222;
  text-decoration: none;
}

.admin-sidebar__link strong {
  font-size: 16px;
  font-weight: 500;
}

.admin-sidebar__link.is-active strong {
  color: #111111;
  font-weight: 700;
}

.admin-sidebar__meta {
  display: grid;
  gap: 12px;
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid #e6e6e6;
}

.admin-sidebar__meta article {
  padding: 16px 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-sidebar__meta span {
  display: block;
  color: #777777;
  font-size: 13px;
}

.admin-sidebar__meta strong {
  display: block;
  margin-top: 8px;
  color: #111111;
  font-size: 18px;
  line-height: 1.35;
}

.admin-content {
  min-width: 0;
}

.admin-content__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e6e6e6;
}

.admin-content__copy h2 {
  margin: 0;
  color: #111111;
  font-size: 38px;
  line-height: 1.15;
  font-weight: 700;
}

.admin-content__copy p {
  margin: 10px 0 0;
  color: #666666;
  font-size: 15px;
  line-height: 1.6;
}

.admin-content__body {
  padding-top: 28px;
}

@media (max-width: 1024px) {
  .admin-layout {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .admin-sidebar__nav {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    border-top: 0;
    gap: 10px;
  }

  .admin-sidebar__link {
    justify-content: center;
    min-height: 48px;
    padding: 0 12px;
    border: 1px solid #e6e6e6;
    text-align: center;
  }

  .admin-sidebar__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-page__inner {
    width: min(100%, calc(100% - 24px));
    padding-top: 20px;
    padding-bottom: 72px;
  }

  .admin-content__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-sidebar h1 {
    margin-bottom: 20px;
    font-size: 30px;
  }

  .admin-sidebar__nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-sidebar__link strong {
    font-size: 14px;
    line-height: 1.35;
    word-break: keep-all;
  }

  .admin-sidebar__meta {
    grid-template-columns: 1fr;
  }

  .admin-content__copy h2 {
    font-size: 32px;
  }
}

@media (max-width: 420px) {
  .admin-sidebar__nav {
    grid-template-columns: 1fr;
  }

  .admin-content__copy h2 {
    font-size: 28px;
  }
}
</style>
