import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  CUSTOMER_SERVICE_FAQ_CATEGORIES,
  CUSTOMER_SERVICE_FAQ_ROWS,
  CUSTOMER_SERVICE_NOTICE_ROWS,
} from '../constants/customerServiceContent';
import { resolveCustomerServiceSection } from '../constants/customerServiceNavigation';
import { getCustomerSupportQnaRows } from '../services/customerSupportService';
import { getCurrentMember } from '../services/memberService';
import { getCustomerNoticeRows } from '../services/noticeService';
import { useAccountStore } from '../stores/account';
import { resolveLookupErrorMessage } from '../utils/apiErrorMessage';
import { hasAdminAccess, hasAuthenticatedSession } from '../utils/accessControl';

const BOARD_PAGE_SIZE = 6;

export function useCustomerServiceBoard() {
  const route = useRoute();
  const accountStore = useAccountStore();

  accountStore.hydrate();

  const noticeKeyword = ref('');
  const activeFaqCategory = ref(CUSTOMER_SERVICE_FAQ_CATEGORIES[0]);
  const openFaqIds = ref(['faq-1']);
  const faqPage = ref(1);
  const qnaKeyword = ref('');
  const noticePage = ref(1);
  const qnaPage = ref(1);
  const noticeRows = ref([]);
  const hasLoadedNoticeRows = ref(false);
  const isNoticeLoading = ref(false);
  const noticeLoadError = ref('');
  const qnaRows = ref([]);
  const isQnaLoading = ref(false);
  const qnaLoadError = ref('');

  const currentSection = computed(() => resolveCustomerServiceSection(route.name));
  const qnaSubmitted = computed(
    () => currentSection.value === 'qna' && route.query.submitted === '1',
  );
  const qnaViewerMode = computed(() => {
    if (!hasAuthenticatedSession(accountStore)) {
      return 'guest';
    }

    if (hasAdminAccess(accountStore)) {
      return 'admin';
    }

    return 'member';
  });
  const canBrowseQnaRows = computed(() => qnaViewerMode.value !== 'guest');

  const filteredNotices = computed(() => {
    const keyword = noticeKeyword.value.trim();
    const sourceRows = hasLoadedNoticeRows.value ? noticeRows.value : CUSTOMER_SERVICE_NOTICE_ROWS;

    if (!keyword) {
      return sourceRows;
    }

    return sourceRows.filter((row) => row.title.includes(keyword));
  });

  const noticeTotalPages = computed(() =>
    Math.max(1, Math.ceil(filteredNotices.value.length / BOARD_PAGE_SIZE)),
  );

  const pagedNotices = computed(() => {
    const start = (noticePage.value - 1) * BOARD_PAGE_SIZE;
    return filteredNotices.value.slice(start, start + BOARD_PAGE_SIZE);
  });

  const filteredFaqRows = computed(() => {
    if (activeFaqCategory.value === CUSTOMER_SERVICE_FAQ_CATEGORIES[0]) {
      return CUSTOMER_SERVICE_FAQ_ROWS;
    }

    return CUSTOMER_SERVICE_FAQ_ROWS.filter((row) => row.category === activeFaqCategory.value);
  });

  const faqTotalPages = computed(() =>
    Math.max(1, Math.ceil(filteredFaqRows.value.length / BOARD_PAGE_SIZE)),
  );

  const pagedFaqRows = computed(() => {
    const start = (faqPage.value - 1) * BOARD_PAGE_SIZE;
    return filteredFaqRows.value.slice(start, start + BOARD_PAGE_SIZE);
  });

  const filteredQnaRows = computed(() => {
    const keyword = qnaKeyword.value.trim().toLowerCase();

    if (!keyword) {
      return qnaRows.value;
    }

    return qnaRows.value.filter((row) => (
      [
        row.title,
        row.questionContent,
        row.answerContent,
        row.writer,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    ));
  });

  const filteredQnaCount = computed(() => filteredQnaRows.value.length);
  const qnaTotalPages = computed(() =>
    Math.max(1, Math.ceil(filteredQnaRows.value.length / BOARD_PAGE_SIZE)),
  );

  const pagedQnaRows = computed(() => {
    const start = (qnaPage.value - 1) * BOARD_PAGE_SIZE;
    return filteredQnaRows.value.slice(start, start + BOARD_PAGE_SIZE);
  });

  async function loadNoticeRows() {
    isNoticeLoading.value = true;
    noticeLoadError.value = '';
    hasLoadedNoticeRows.value = false;

    try {
      noticeRows.value = await getCustomerNoticeRows();
    } catch (error) {
      noticeRows.value = CUSTOMER_SERVICE_NOTICE_ROWS;
      noticeLoadError.value = resolveLookupErrorMessage(error, '공지사항을 불러오지 못했습니다.');
    } finally {
      hasLoadedNoticeRows.value = true;
      isNoticeLoading.value = false;
    }
  }

  async function loadQnaRows() {
    if (!canBrowseQnaRows.value) {
      qnaRows.value = [];
      qnaLoadError.value = '';
      isQnaLoading.value = false;
      return;
    }

    isQnaLoading.value = true;
    qnaLoadError.value = '';
    const previousRows = Array.isArray(qnaRows.value) ? [...qnaRows.value] : [];

    try {
      try {
        const currentMember = await getCurrentMember();
        const memberSession = currentMember?.data ?? currentMember ?? null;

        if (memberSession && typeof memberSession === 'object') {
          accountStore.setMemberSession(memberSession);
          accountStore.setProfileHydrated(true);
        }
      } catch {
        // Keep the last known session so QnA loading can continue.
      }

      qnaRows.value = await getCustomerSupportQnaRows({}, {
        includeAll: hasAdminAccess(accountStore),
      });
    } catch (error) {
      qnaRows.value = previousRows;
      qnaLoadError.value = resolveLookupErrorMessage(error, '등록 내역을 불러오지 못했습니다.');
    } finally {
      isQnaLoading.value = false;
    }
  }

  function selectFaqCategory(category) {
    activeFaqCategory.value = category;
    faqPage.value = 1;
  }

  function toggleFaq(id) {
    openFaqIds.value = openFaqIds.value.includes(id)
      ? openFaqIds.value.filter((item) => item !== id)
      : [...openFaqIds.value, id];
  }

  function changeNoticePage(page) {
    noticePage.value = page;
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }

  function changeFaqPage(page) {
    faqPage.value = page;
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }

  function changeQnaPage(page) {
    qnaPage.value = page;
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }

  onMounted(() => {
    if (currentSection.value === 'notice') {
      void loadNoticeRows();
    }

    if (currentSection.value === 'qna') {
      void loadQnaRows();
    }
  });

  watch(noticeKeyword, () => {
    noticePage.value = 1;
  });

  watch(faqTotalPages, (pageCount) => {
    if (faqPage.value > pageCount) {
      faqPage.value = pageCount;
    }
  });

  watch(qnaKeyword, () => {
    qnaPage.value = 1;
  });

  watch(currentSection, (section) => {
    faqPage.value = 1;
    noticePage.value = 1;
    qnaPage.value = 1;

    if (section === 'notice') {
      void loadNoticeRows();
    }

    if (section === 'qna') {
      void loadQnaRows();
    }
  });

  watch(
    () => [accountStore.accessToken, accountStore.role],
    () => {
      if (currentSection.value === 'qna') {
        void loadQnaRows();
      }
    },
  );

  return {
    activeFaqCategory,
    canBrowseQnaRows,
    changeFaqPage,
    changeNoticePage,
    changeQnaPage,
    currentSection,
    faqCategories: CUSTOMER_SERVICE_FAQ_CATEGORIES,
    faqPage,
    faqTotalPages,
    filteredFaqRows,
    filteredQnaCount,
    isNoticeLoading,
    isQnaLoading,
    noticeLoadError,
    noticeKeyword,
    noticePage,
    noticeTotalPages,
    openFaqIds,
    pagedFaqRows,
    pagedNotices,
    pagedQnaRows,
    qnaPage,
    qnaKeyword,
    qnaLoadError,
    qnaSubmitted,
    qnaTotalPages,
    qnaViewerMode,
    reloadQnaRows: loadQnaRows,
    selectFaqCategory,
    toggleFaq,
  };
}
