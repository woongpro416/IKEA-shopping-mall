import { computed, onBeforeUnmount, ref, unref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { buildProductCategoryPath, ROUTE_PATHS } from '../constants/routes';

export function useHeaderMenu(categoriesSource, options = {}) {
  const router = useRouter();
  const route = useRoute();
  const categories = computed(() => unref(categoriesSource) ?? []);
  const resolvedDefaultTab = options.defaultTab ?? (route.path.startsWith('/customer-service') ? 'customer-service' : 'furniture');
  const routeCategorySlug = typeof route.params.categorySlug === 'string' ? route.params.categorySlug : '';
  const resolvedInitialCategory = categories.value.find(
    (category) => category.slug === routeCategorySlug || category.id === options.defaultCategoryId,
  ) ?? categories.value[0];

  const activeTab = ref(resolvedDefaultTab);
  const activeCategoryId = ref(resolvedInitialCategory?.id ?? '');
  const isMenuOpen = ref(false);

  let closeTimerId;

  const activeCategory = computed(
    () => categories.value.find((category) => category.id === activeCategoryId.value) ?? categories.value[0] ?? null,
  );

  function clearCloseTimer() {
    if (typeof closeTimerId === 'number') {
      window.clearTimeout(closeTimerId);
      closeTimerId = undefined;
    }
  }

  function openMenu(tabId) {
    if (tabId === 'customer-service') {
      return;
    }

    clearCloseTimer();
    activeTab.value = tabId;
    isMenuOpen.value = true;
  }

  function pinMenuOpen() {
    clearCloseTimer();
    isMenuOpen.value = true;
  }

  function closeMenu() {
    clearCloseTimer();
    isMenuOpen.value = false;
  }

  function scheduleCloseMenu() {
    clearCloseTimer();
    closeTimerId = window.setTimeout(() => {
      isMenuOpen.value = false;
      closeTimerId = undefined;
    }, 120);
  }

  function setActiveCategory(categoryId) {
    clearCloseTimer();
    activeCategoryId.value = categoryId;
  }

  function goToCategory(categoryValue, typeSlug) {
    const resolvedCategory = categories.value.find(
      (category) => category.slug === categoryValue || category.id === categoryValue,
    ) ?? categories.value[0];

    if (!resolvedCategory) {
      return;
    }

    closeMenu();
    router.push({
      path: buildProductCategoryPath(resolvedCategory.slug),
      query: typeSlug && typeSlug !== 'all' ? { type: typeSlug } : {},
    });
  }

  function handleSubmenuCategoryClick(category) {
    if (!category) {
      return;
    }

    goToCategory(category.slug);
  }

  function handleSubmenuCardClick(category, item) {
    if (item?.to) {
      closeMenu();
      router.push(item.to);
      return;
    }

    if (!category || !item?.slug) {
      return;
    }

    goToCategory(category.slug, item.slug);
  }

  function handleHeaderTabClick(tab) {
    if (!tab) {
      return;
    }

    if (tab.id === 'customer-service') {
      activeTab.value = 'customer-service';
      closeMenu();
      router.push(ROUTE_PATHS.customerServiceNotice);
      return;
    }

    openMenu(tab.id);
  }

  watch(
    () => route.params.categorySlug,
    (categorySlug) => {
      if (typeof categorySlug !== 'string') {
        return;
      }

      const matchedCategory = categories.value.find((category) => category.slug === categorySlug);
      if (matchedCategory) {
        activeCategoryId.value = matchedCategory.id;
      }
    },
    { immediate: true },
  );

  watch(
    categories,
    (nextCategories) => {
      if (!nextCategories.length) {
        activeCategoryId.value = '';
        return;
      }

      const hasActiveCategory = nextCategories.some((category) => category.id === activeCategoryId.value);
      if (!hasActiveCategory) {
        activeCategoryId.value = nextCategories[0].id;
      }
    },
    { immediate: true },
  );

  onBeforeUnmount(clearCloseTimer);

  return {
    activeTab,
    activeCategoryId,
    activeCategory,
    cartPath: ROUTE_PATHS.cart,
    isMenuOpen,
    memberLoginPath: ROUTE_PATHS.memberLogin,
    memberMyPagePath: ROUTE_PATHS.memberMyPage,
    openMenu,
    pinMenuOpen,
    closeMenu,
    scheduleCloseMenu,
    setActiveCategory,
    goToCategory,
    handleSubmenuCategoryClick,
    handleSubmenuCardClick,
    handleHeaderTabClick,
  };
}
