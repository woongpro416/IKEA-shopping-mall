<script setup>
import { computed, onMounted, reactive, shallowRef, watch } from 'vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import {
  createAdminQnaAnswer,
  deleteAdminQnaAnswer,
  getAdminQnas,
  updateAdminQnaAnswer,
} from '../../services/adminService';
import {
  formatAdminDateTime,
  normalizeAdminQnaThreads,
  normalizeArrayPayload,
} from '../../mappers/adminManagementMapper';
import { useFeedback } from '../../composables/useFeedback';
import { resolveAdminActionErrorMessage } from '../../utils/apiErrorMessage';

const threads = shallowRef([]);
const selectedThreadId = shallowRef('');
const searchKeyword = shallowRef('');
const statusFilter = shallowRef('ALL');
const statusMessage = shallowRef('');
const loadErrorMessage = shallowRef('');
const isLoading = shallowRef(false);
const isSubmitting = shallowRef(false);
const currentPage = shallowRef(1);
const pageSize = 5;
const { requestConfirm } = useFeedback();

const answerForm = reactive({
  content: '',
});

const filteredThreads = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();

  return threads.value.filter((thread) => {
    const matchesStatus = statusFilter.value === 'ALL'
      ? true
      : (statusFilter.value === 'ANSWERED' ? Boolean(thread.answer) : !thread.answer);

    if (!matchesStatus) {
      return false;
    }

    if (!keyword) {
      return true;
    }

    const haystacks = [
      thread.title,
      thread.writer,
      thread.question?.content,
    ]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());

    return haystacks.some((value) => value.includes(keyword));
  });
});

const threadCounts = computed(() => ([
  { value: 'ALL', label: '전체', count: threads.value.length },
  { value: 'WAITING', label: '답변대기', count: threads.value.filter((thread) => !thread.answer).length },
  { value: 'ANSWERED', label: '답변완료', count: threads.value.filter((thread) => Boolean(thread.answer)).length },
]));

const pageCount = computed(() => Math.max(Math.ceil(filteredThreads.value.length / pageSize), 1));
const pagedThreads = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredThreads.value.slice(start, start + pageSize);
});

const selectedThread = computed(
  () => threads.value.find((thread) => thread.id === selectedThreadId.value) ?? null,
);
const submitButtonLabel = computed(() => {
  if (isSubmitting.value) {
    return selectedThread.value?.answer ? '답변 수정 중...' : '답변 등록 중...';
  }

  return selectedThread.value?.answer ? '답변 수정' : '답변 등록';
});

function getThreadStatus(thread) {
  return thread?.answer ? '답변완료' : '답변대기';
}

function syncAnswerForm(thread) {
  answerForm.content = thread?.answer?.content || '';
}

function syncSelectedThread(preferredThreadId = selectedThreadId.value) {
  const matchedThread = threads.value.find((thread) => thread.id === preferredThreadId);

  if (matchedThread) {
    selectedThreadId.value = matchedThread.id;
    syncAnswerForm(matchedThread);
    return;
  }

  selectedThreadId.value = threads.value[0]?.id ?? '';
  syncAnswerForm(threads.value[0] ?? null);
}

function applyThreads(items) {
  threads.value = normalizeAdminQnaThreads(items);
}

async function loadThreads(options = {}) {
  const {
    preferredThreadId = selectedThreadId.value,
  } = options;
  isLoading.value = true;
  loadErrorMessage.value = '';

  try {
    const payload = await getAdminQnas();
    applyThreads(normalizeArrayPayload(payload, []));
  } catch (error) {
    applyThreads([]);
    loadErrorMessage.value = resolveAdminActionErrorMessage(
      error,
      '문의 목록을 불러오지 못했습니다.',
    );
    return false;
  } finally {
    isLoading.value = false;
  }

  syncSelectedThread(preferredThreadId);

  return true;
}

function selectThread(thread) {
  selectedThreadId.value = thread.id;
  syncAnswerForm(thread);
  statusMessage.value = '';
}

async function submitAnswer() {
  if (!selectedThread.value || !answerForm.content.trim()) {
    statusMessage.value = '답변 내용을 입력해 주세요.';
    return;
  }

  const threadId = selectedThread.value.id;
  const hasAnswer = Boolean(selectedThread.value.answer);
  const answerId = selectedThread.value.answer?.qnaId;
  const questionId = selectedThread.value.question.qnaId;
  isSubmitting.value = true;
  statusMessage.value = '';

  const payload = {
    title: `${selectedThread.value.title} 답변`,
    content: answerForm.content.trim(),
  };

  try {
    if (hasAnswer && answerId) {
      await updateAdminQnaAnswer(answerId, payload);
      const didLoadFromServer = await loadThreads({
        preferredThreadId: threadId,
      });
      statusMessage.value = didLoadFromServer
        ? '답변을 수정했습니다.'
        : '답변은 수정됐지만 목록 재조회는 실패했습니다.';
    } else {
      await createAdminQnaAnswer(questionId, payload);
      const didLoadFromServer = await loadThreads({
        preferredThreadId: threadId,
      });
      statusMessage.value = didLoadFromServer
        ? '답변을 등록했습니다.'
        : '답변은 등록됐지만 목록 재조회는 실패했습니다.';
    }
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(
      error,
      hasAnswer ? '답변 수정에 실패했습니다.' : '답변 등록에 실패했습니다.',
    );
  }

  isSubmitting.value = false;
}

async function removeAnswer() {
  if (!selectedThread.value?.answer) {
    return;
  }

  const confirmed = await requestConfirm({
    title: '답변 삭제',
    message: '현재 답변을 삭제할까요?',
    confirmLabel: '삭제',
  });

  if (!confirmed) {
    return;
  }

  const threadId = selectedThread.value.id;
  const answerId = selectedThread.value.answer.qnaId;
  isSubmitting.value = true;
  statusMessage.value = '';

  try {
    await deleteAdminQnaAnswer(answerId);
    const didLoadFromServer = await loadThreads({
      preferredThreadId: threadId,
    });
    statusMessage.value = didLoadFromServer
      ? '답변을 삭제했습니다.'
      : '답변은 삭제됐지만 목록 재조회는 실패했습니다.';
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '답변 삭제에 실패했습니다.');
  }

  isSubmitting.value = false;
}

watch(searchKeyword, () => {
  currentPage.value = 1;
});

watch(statusFilter, () => {
  currentPage.value = 1;
});

watch(
  () => filteredThreads.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);

onMounted(loadThreads);
</script>

<template>
  <section class="admin-qna-manager">
    <AdminPanel title="문의 목록" description="답변 대기 문의를 먼저 확인하고 상태를 관리합니다.">
      <template #action>
        <input
          v-model="searchKeyword"
          type="text"
          class="admin-qna-manager__search"
          placeholder="문의 제목 또는 작성자 검색"
        />
      </template>

      <div class="admin-qna-manager__chips">
        <button
          v-for="option in threadCounts"
          :key="option.value"
          type="button"
          class="admin-qna-manager__chip"
          :class="{ 'is-active': statusFilter === option.value }"
          @click="statusFilter = option.value"
        >
          <span>{{ option.label }}</span>
          <strong>{{ option.count }}</strong>
        </button>
      </div>

      <div class="admin-qna-manager__list">
        <button
          v-for="thread in pagedThreads"
          :key="thread.id"
          type="button"
          class="admin-qna-manager__row"
          :class="{ 'is-active': selectedThreadId === thread.id }"
          @click="selectThread(thread)"
        >
          <div>
            <strong>{{ thread.title }}</strong>
            <span>{{ thread.writer }}</span>
          </div>
          <div class="admin-qna-manager__meta">
            <b>{{ getThreadStatus(thread) }}</b>
            <span>{{ formatAdminDateTime(thread.createdAt) }}</span>
          </div>
        </button>

        <CommonStatePanel
          v-if="!filteredThreads.length"
          :tone="isLoading ? 'loading' : loadErrorMessage ? 'error' : 'neutral'"
          :title="isLoading ? '문의 목록을 불러오는 중입니다.' : loadErrorMessage ? '문의 목록을 불러오지 못했습니다.' : '표시할 문의가 없습니다.'"
          :description="loadErrorMessage"
          compact
        />
      </div>

      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>

    <AdminPanel title="문의 상세" description="질문 내용을 확인하고 답변을 등록하거나 수정합니다.">
      <div v-if="selectedThread" class="admin-qna-manager__detail">
        <article class="admin-qna-manager__question">
          <span>질문</span>
          <strong>{{ selectedThread.question.title }}</strong>
          <p>{{ selectedThread.question.content }}</p>
          <small>
            {{ selectedThread.question.writer }}
            ·
            {{ formatAdminDateTime(selectedThread.question.createdAt) }}
          </small>
        </article>

        <form class="admin-qna-manager__answer-form" @submit.prevent="submitAnswer">
          <label>
            <span>답변 내용</span>
            <textarea v-model="answerForm.content" rows="8" />
          </label>

          <div class="admin-qna-manager__actions">
            <button type="submit" class="admin-qna-manager__primary" :disabled="isSubmitting">
              {{ submitButtonLabel }}
            </button>
            <button
              type="button"
              class="admin-qna-manager__secondary"
              :disabled="!selectedThread.answer || isSubmitting"
              @click="removeAnswer"
            >
              답변 삭제
            </button>
          </div>
        </form>

        <p v-if="statusMessage" class="admin-qna-manager__status">{{ statusMessage }}</p>
      </div>

      <CommonStatePanel
        v-else
        :tone="loadErrorMessage ? 'error' : 'neutral'"
        :title="loadErrorMessage ? '문의 상세를 표시할 수 없습니다.' : '문의를 선택하면 상세 내용이 표시됩니다.'"
        :description="loadErrorMessage"
        align="left"
        compact
      />
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-qna-manager {
  display: grid;
  gap: 40px;
}

.admin-qna-manager__search {
  width: min(320px, 100%);
  height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  box-sizing: border-box;
}

.admin-qna-manager__list {
  display: grid;
  border-bottom: 1px solid #ededed;
}

.admin-qna-manager__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-qna-manager__chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  cursor: pointer;
}

.admin-qna-manager__chip.is-active {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.admin-qna-manager__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 18px 0;
  border: 0;
  border-top: 1px solid #efefef;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.admin-qna-manager__row.is-active {
  background: #f7f9fb;
}

.admin-qna-manager__row strong {
  display: block;
  color: #111111;
  font-size: 15px;
}

.admin-qna-manager__row span,
.admin-qna-manager__meta span {
  display: block;
  margin-top: 6px;
  color: #777777;
  font-size: 13px;
}

.admin-qna-manager__meta {
  text-align: right;
}

.admin-qna-manager__meta b {
  color: #111111;
  font-size: 14px;
}

.admin-qna-manager__detail {
  display: grid;
  gap: 18px;
}

.admin-qna-manager__question,
.admin-qna-manager__answer-form {
  padding: 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-qna-manager__question span,
.admin-qna-manager__answer-form span {
  display: block;
  color: #777777;
  font-size: 13px;
}

.admin-qna-manager__question strong {
  display: block;
  margin-top: 10px;
  color: #111111;
  font-size: 22px;
  line-height: 1.3;
}

.admin-qna-manager__question p {
  margin: 14px 0 0;
  color: #333333;
  line-height: 1.7;
}

.admin-qna-manager__question small {
  display: block;
  margin-top: 12px;
  color: #888888;
  font-size: 13px;
}

.admin-qna-manager__answer-form {
  display: grid;
  gap: 14px;
}

.admin-qna-manager__answer-form label {
  display: grid;
  gap: 8px;
}

.admin-qna-manager__answer-form input,
.admin-qna-manager__answer-form textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  font: inherit;
}

.admin-qna-manager__answer-form textarea {
  resize: vertical;
}

.admin-qna-manager__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.admin-qna-manager__primary,
.admin-qna-manager__secondary {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  cursor: pointer;
}

.admin-qna-manager__primary {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.admin-qna-manager__secondary:disabled {
  opacity: 0.45;
  cursor: default;
}

.admin-qna-manager__status,
.admin-qna-manager__empty {
  color: #666666;
  font-size: 14px;
  line-height: 1.6;
}

.admin-qna-manager__status {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
}

@media (max-width: 720px) {
  .admin-qna-manager__search {
    width: 100%;
  }

  .admin-qna-manager__row {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-qna-manager__meta {
    text-align: left;
  }
}
</style>
