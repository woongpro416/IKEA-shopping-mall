<script setup>
import { computed, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import AdminPagination from '../components/admin/AdminPagination.vue';
import CommonStatePanel from '../components/common/CommonStatePanel.vue';
import CustomerQnaThreadList from '../components/customer-service/CustomerQnaThreadList.vue';
import CustomerServiceShell from '../components/customer-service/CustomerServiceShell.vue';
import { useFeedback } from '../composables/useFeedback';
import { useCustomerServiceBoard } from '../composables/useCustomerServiceBoard';
import {
  ROUTE_PATHS,
  buildCustomerServiceNoticeDetailPath,
} from '../constants/routes';
import { deleteQnaQuestion } from '../services/qnaService';
import { resolveAdminActionErrorMessage } from '../utils/apiErrorMessage';

const router = useRouter();
const { requestConfirm, showError } = useFeedback();
const isDeletingQna = shallowRef(false);
const {
  activeFaqCategory,
  canBrowseQnaRows,
  changeFaqPage,
  changeNoticePage,
  changeQnaPage,
  currentSection,
  faqCategories,
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
  reloadQnaRows,
  selectFaqCategory,
  toggleFaq,
} = useCustomerServiceBoard();

const pageTitle = computed(() => {
  if (currentSection.value === 'faq') {
    return 'FAQ';
  }

  if (currentSection.value === 'qna') {
    return 'QnA';
  }

  return '공지사항';
});

const qnaFeedbackMessage = computed(() => {
  if (qnaSubmitted.value) {
    return '등록이 완료되었습니다. 답변이 등록되면 이 화면에서 바로 확인할 수 있습니다.';
  }

  if (router.currentRoute.value.query.updated === '1') {
    return '내역을 수정했습니다.';
  }

  if (router.currentRoute.value.query.deleted === '1') {
    return '내역을 삭제했습니다.';
  }

  return '';
});

const qnaNoteMessage = computed(() => {
  if (qnaViewerMode.value === 'guest') {
    return 'QnA는 로그인한 계정 기준으로 등록 내역과 답변 상태를 확인할 수 있습니다.';
  }

  if (qnaViewerMode.value === 'admin') {
    return '관리자 계정은 전체 QnA 목록을 확인할 수 있습니다.';
  }

  return '로그인한 계정의 등록 내역만 표시됩니다.';
});

const qnaSearchPlaceholder = computed(() => '제목/답변 검색');
const qnaEmptyDescription = computed(() => {
  if (qnaViewerMode.value === 'member') {
    return '등록한 내역이 없습니다.';
  }

  if (qnaViewerMode.value === 'admin') {
    return '등록된 문의가 없습니다.';
  }

  return '로그인 후 등록 내역을 확인해 주세요.';
});

function openQnaWrite() {
  router.push(ROUTE_PATHS.customerServiceQnaWrite);
}

function openQnaEdit(item) {
  router.push({
    path: ROUTE_PATHS.customerServiceQnaWrite,
    query: {
      editId: item.id,
    },
  });
}

function openQnaLogin() {
  router.push({
    path: ROUTE_PATHS.memberLogin,
    query: {
      redirect: ROUTE_PATHS.customerServiceQna,
    },
  });
}

async function removeQna(item) {
  if (isDeletingQna.value) {
    return;
  }

  const confirmed = await requestConfirm({
    title: '내역 삭제',
    message: '등록 내역을 삭제하시겠습니까?',
    confirmLabel: '삭제',
  });

  if (!confirmed) {
    return;
  }

  isDeletingQna.value = true;

  try {
    await deleteQnaQuestion(Number(item.id));
    await reloadQnaRows();
    await router.replace({
      path: ROUTE_PATHS.customerServiceQna,
      query: {
        deleted: '1',
      },
    });
  } catch (error) {
    showError(resolveAdminActionErrorMessage(error, '등록 내역을 삭제하지 못했습니다.'));
  } finally {
    isDeletingQna.value = false;
  }
}
</script>

<template>
  <CustomerServiceShell :title="pageTitle" :current-section="currentSection">
    <template v-if="currentSection === 'notice'">
      <div class="cs-toolbar">
        <div class="cs-search">
          <input v-model="noticeKeyword" type="text" placeholder="공지사항 검색" />
        </div>
        <p class="cs-toolbar__hint">입력과 동시에 결과가 바로 반영됩니다.</p>
      </div>

      <div class="cs-board">
        <div class="cs-board__head">
          <span>번호</span>
          <span>제목</span>
          <span>등록일</span>
        </div>
        <div v-if="pagedNotices.length" class="cs-board__body">
          <RouterLink
            v-for="row in pagedNotices"
            :key="row.id"
            :to="buildCustomerServiceNoticeDetailPath(row.id)"
            class="cs-board__row"
          >
            <span>{{ row.badge || row.id }}</span>
            <strong>{{ row.title }}</strong>
            <span>{{ row.date }}</span>
          </RouterLink>
        </div>
        <CommonStatePanel
          v-else-if="isNoticeLoading"
          tone="loading"
          title="공지사항 목록을 불러오는 중입니다."
          compact
        />
        <CommonStatePanel
          v-else-if="noticeLoadError"
          tone="error"
          title="공지사항 목록을 확인할 수 없습니다."
          :description="noticeLoadError"
          compact
        />
        <CommonStatePanel
          v-else
          title="검색 결과가 없습니다."
          description="다른 검색어로 다시 확인해 주세요."
          compact
        />
      </div>

      <AdminPagination
        :current-page="noticePage"
        :page-count="noticeTotalPages"
        @update:current-page="changeNoticePage"
      />
    </template>

    <template v-else-if="currentSection === 'faq'">
      <div class="cs-faq-filter">
        <button
          v-for="category in faqCategories"
          :key="category"
          type="button"
          :class="{ 'is-active': activeFaqCategory === category }"
          @click="selectFaqCategory(category)"
        >
          {{ category }}
        </button>
      </div>

      <div class="cs-faq-list">
        <article v-for="faq in pagedFaqRows" :key="faq.id" class="cs-faq-item">
          <button class="cs-faq-item__question" type="button" @click="toggleFaq(faq.id)">
            <span>{{ faq.category }}</span>
            <strong>{{ faq.question }}</strong>
            <b>{{ openFaqIds.includes(faq.id) ? '-' : '+' }}</b>
          </button>
          <div v-if="openFaqIds.includes(faq.id)" class="cs-faq-item__answer">
            {{ faq.answer }}
          </div>
        </article>
      </div>

      <AdminPagination
        v-if="filteredFaqRows.length"
        :current-page="faqPage"
        :page-count="faqTotalPages"
        @update:current-page="changeFaqPage"
      />
    </template>

    <template v-else>
      <div v-if="qnaFeedbackMessage" class="cs-feedback">
        {{ qnaFeedbackMessage }}
      </div>

      <div class="cs-qna-actions">
        <div class="cs-qna-actions__buttons">
          <button v-if="qnaViewerMode === 'member'" type="button" @click="openQnaWrite">작성하기</button>
          <button v-else-if="qnaViewerMode === 'guest'" type="button" class="is-light" @click="openQnaLogin">로그인 후 이용</button>
        </div>
      </div>

      <div class="cs-qna-note">
        {{ qnaNoteMessage }}
      </div>

      <template v-if="canBrowseQnaRows">
        <div class="cs-toolbar cs-toolbar--qna">
          <div class="cs-search">
            <input v-model="qnaKeyword" type="text" :placeholder="qnaSearchPlaceholder" />
          </div>
          <p class="cs-toolbar__hint">입력과 동시에 결과가 바로 반영됩니다.</p>
        </div>

        <CommonStatePanel
          v-if="isQnaLoading"
          tone="loading"
          title="등록 내역을 불러오는 중입니다."
          compact
        />
        <CommonStatePanel
          v-else-if="qnaLoadError && !pagedQnaRows.length"
          tone="error"
          title="등록 내역을 확인할 수 없습니다."
          :description="qnaLoadError"
          compact
        />

        <template v-else>
          <div v-if="qnaLoadError" class="cs-feedback cs-feedback--warning">
            {{ qnaLoadError }}
          </div>

          <CustomerQnaThreadList
            :items="pagedQnaRows"
            :empty-description="qnaEmptyDescription"
            :show-item-actions="qnaViewerMode === 'member'"
            :show-writer="qnaViewerMode === 'admin'"
            @edit-item="openQnaEdit"
            @delete-item="removeQna"
          />

          <AdminPagination
            v-if="filteredQnaCount"
            :current-page="qnaPage"
            :page-count="qnaTotalPages"
            @update:current-page="changeQnaPage"
          />
        </template>
      </template>

      <CommonStatePanel
        v-else
        layout="boxed"
        align="left"
        title="로그인 후 등록 내역을 확인해 주세요."
        description="QnA는 로그인한 계정 기준으로 등록, 조회, 수정, 삭제할 수 있습니다."
      >
        <template #actions>
          <button type="button" class="cs-inline-action" @click="openQnaLogin">로그인</button>
        </template>
      </CommonStatePanel>
    </template>
  </CustomerServiceShell>
</template>

<style scoped>
.cs-toolbar {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 0 16px;
}

.cs-search {
  display: flex;
  width: min(420px, 100%);
}

.cs-search input {
  flex: 1;
  height: 44px;
  border: 1px solid #d9d9d9;
  padding: 0 14px;
  font-size: 14px;
  color: #111111;
  box-sizing: border-box;
}

.cs-toolbar__hint {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.cs-qna-actions {
  margin-bottom: 16px;
}

.cs-qna-actions__buttons {
  display: flex;
  gap: 10px;
}

.cs-qna-actions__buttons button {
  height: 44px;
  padding: 0 18px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.cs-qna-actions__buttons button.is-light {
  border-color: #d9d9d9;
  background: #ffffff;
  color: #444444;
}

.cs-inline-action {
  height: 40px;
  padding: 0 16px;
  border: 1px solid #111111;
  background: #111111;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.cs-qna-note {
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid #eceff3;
  background: #fafafa;
  color: #555555;
  font-size: 14px;
  line-height: 1.7;
}

.cs-feedback {
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid #dce7f8;
  background: #f5f9ff;
  color: #264a86;
  font-size: 14px;
  line-height: 1.7;
}

.cs-feedback--warning {
  border-color: #f1d8aa;
  background: #fff8ea;
  color: #7c5411;
}

.cs-board {
  border-top: 1px solid #111111;
}

.cs-board__head,
.cs-board__row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 140px;
  align-items: center;
}

.cs-board__head {
  min-height: 52px;
  border-bottom: 1px solid #e6e6e6;
  color: #555555;
  font-size: 14px;
  font-weight: 600;
}

.cs-board__head span,
.cs-board__row span {
  display: flex;
  justify-content: center;
}

.cs-board__row {
  width: 100%;
  min-height: 62px;
  border-bottom: 1px solid #efefef;
  background: transparent;
  color: #111111;
  text-decoration: none;
}

.cs-board__row strong {
  min-width: 0;
  padding-right: 18px;
  font-size: 15px;
  font-weight: 500;
}

.cs-faq-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cs-faq-filter button {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #111111;
  cursor: pointer;
}

.cs-faq-filter button.is-active {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.cs-faq-list {
  display: grid;
  border-top: 1px solid #111111;
}

.cs-faq-item {
  border-bottom: 1px solid #eceff3;
}

.cs-faq-item__question {
  width: 100%;
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 16px;
  padding: 18px 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.cs-faq-item__question span,
.cs-faq-item__question b {
  color: #555555;
  font-size: 14px;
}

.cs-faq-item__question strong {
  color: #111111;
  font-size: 15px;
  font-weight: 600;
}

.cs-faq-item__answer {
  padding: 0 0 18px 156px;
  color: #555555;
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .cs-faq-item__question {
    grid-template-columns: 110px minmax(0, 1fr) 32px;
  }

  .cs-faq-item__answer {
    padding-left: 126px;
  }
}

@media (max-width: 720px) {
  .cs-toolbar,
  .cs-qna-actions__buttons {
    align-items: stretch;
  }

  .cs-search {
    width: 100%;
  }

  .cs-qna-actions__buttons {
    flex-direction: column;
  }

  .cs-board__head,
  .cs-board__row,
  .cs-faq-item__question {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 16px 0;
  }

  .cs-board__head {
    display: none;
  }

  .cs-board__row span,
  .cs-board__head span {
    justify-content: flex-start;
  }

  .cs-board__row strong {
    padding-right: 0;
  }

  .cs-faq-item__answer {
    padding: 0 0 16px;
  }
}
</style>
