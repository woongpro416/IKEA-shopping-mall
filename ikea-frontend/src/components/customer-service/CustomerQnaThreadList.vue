<script setup>
import { shallowRef, watch } from 'vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  emptyTitle: {
    type: String,
    default: '등록된 내역이 없습니다.',
  },
  emptyDescription: {
    type: String,
    default: '',
  },
  showWriter: {
    type: Boolean,
    default: false,
  },
  showItemActions: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['edit-item', 'delete-item', 'toggle-item']);

const openIds = shallowRef([]);

function isOpen(itemId) {
  return openIds.value.includes(String(itemId));
}

function toggleItem(itemId) {
  const normalizedItemId = String(itemId);
  const willOpen = !isOpen(normalizedItemId);
  openIds.value = willOpen
    ? [...openIds.value, normalizedItemId]
    : openIds.value.filter((id) => id !== normalizedItemId);
  const item = props.items.find((entry) => String(entry.id) === normalizedItemId);
  emit('toggle-item', item, willOpen);
}

watch(
  () => props.items.map((item) => String(item.id)),
  (nextIds) => {
    openIds.value = openIds.value.filter((itemId) => nextIds.includes(itemId));

    if (!openIds.value.length && nextIds.length === 1) {
      openIds.value = [nextIds[0]];
    }
  },
  { immediate: true },
);
</script>

<template>
  <div v-if="items.length" class="cs-qna-list">
    <article v-for="item in items" :key="item.id" class="cs-qna-list__item">
      <button
        class="cs-qna-list__summary"
        type="button"
        :class="{ 'is-open': isOpen(item.id) }"
        @click="toggleItem(item.id)"
      >
        <div class="cs-qna-list__copy">
          <strong>{{ item.title }}</strong>
          <p v-if="showWriter && item.writer">{{ item.writer }}</p>
        </div>
        <div class="cs-qna-list__meta">
          <span class="cs-qna-list__status" :class="{ 'is-pending': item.status !== '답변완료' }">
            {{ item.status }}
          </span>
          <span>{{ item.date }}</span>
          <b>{{ isOpen(item.id) ? '-' : '+' }}</b>
        </div>
      </button>

      <div v-if="isOpen(item.id)" class="cs-qna-list__detail">
        <section class="cs-qna-list__detail-section">
          <span>내용</span>
          <p>{{ item.questionContent || '등록된 내용을 확인할 수 없습니다.' }}</p>
        </section>

        <section class="cs-qna-list__detail-section">
          <span>답변</span>
          <p v-if="item.answerContent">{{ item.answerContent }}</p>
          <p v-else class="is-pending-copy">답변이 등록되면 여기에서 바로 확인할 수 있습니다.</p>
          <small v-if="item.answerDate">{{ item.answerDate }}</small>
        </section>

        <div v-if="showItemActions" class="cs-qna-list__actions">
          <button type="button" class="cs-qna-list__action cs-qna-list__action--secondary" @click="emit('edit-item', item)">
            수정
          </button>
          <button type="button" class="cs-qna-list__action cs-qna-list__action--danger" @click="emit('delete-item', item)">
            삭제
          </button>
        </div>
      </div>
    </article>
  </div>

  <CommonStatePanel
    v-else
    :title="emptyTitle"
    :description="emptyDescription"
    align="left"
    compact
  />
</template>

<style scoped>
.cs-qna-list {
  display: grid;
  border-top: 1px solid #111111;
}

.cs-qna-list__item {
  border-bottom: 1px solid #eceff3;
}

.cs-qna-list__summary {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.cs-qna-list__copy {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.cs-qna-list__copy strong {
  color: #111111;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
}

.cs-qna-list__copy p,
.cs-qna-list__detail-section p,
.cs-qna-list__detail-section small {
  margin: 0;
  color: #666666;
  font-size: 14px;
  line-height: 1.7;
}

.cs-qna-list__meta {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #666666;
  font-size: 13px;
  white-space: nowrap;
}

.cs-qna-list__status {
  color: #0f6b3b;
  font-weight: 600;
}

.cs-qna-list__status.is-pending,
.cs-qna-list__detail-section .is-pending-copy {
  color: #b54708;
}

.cs-qna-list__meta b {
  color: #111111;
  font-size: 18px;
  font-weight: 500;
}

.cs-qna-list__detail {
  display: grid;
  gap: 16px;
  padding: 0 0 20px;
}

.cs-qna-list__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.cs-qna-list__detail-section {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid #eceff3;
  background: #fafafa;
}

.cs-qna-list__detail-section span {
  color: #111111;
  font-size: 14px;
  font-weight: 600;
}

.cs-qna-list__action {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #111111;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.cs-qna-list__action--danger {
  border-color: #e3c8c8;
  color: #b42318;
}

@media (max-width: 720px) {
  .cs-qna-list__summary,
  .cs-qna-list__meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .cs-qna-list__summary {
    gap: 12px;
    padding: 16px 0;
  }

  .cs-qna-list__actions {
    justify-content: stretch;
  }
}
</style>
