<script setup>
import { computed, onMounted, shallowRef, watch } from 'vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import {
  deleteAdminMember,
  getAdminMemberDetail,
  getAdminMembers,
  updateAdminMemberRole,
} from '../../services/adminService';
import {
  formatAdminDate,
  formatAdminDateTime,
  normalizeAdminMember,
  normalizeArrayPayload,
  normalizeObjectPayload,
} from '../../mappers/adminManagementMapper';
import { useFeedback } from '../../composables/useFeedback';
import { resolveAdminActionErrorMessage } from '../../utils/apiErrorMessage';

const members = shallowRef([]);
const selectedMemberId = shallowRef('');
const selectedMember = shallowRef(null);
const roleDraft = shallowRef('USER');
const searchKeyword = shallowRef('');
const currentPage = shallowRef(1);
const statusMessage = shallowRef('');
const loadErrorMessage = shallowRef('');
const isLoading = shallowRef(false);
const isDetailLoading = shallowRef(false);
const isSaving = shallowRef(false);
const pageSize = 5;
let latestDetailRequestToken = 0;
const { requestConfirm } = useFeedback();

const canChangeRole = computed(() => Boolean(
  selectedMember.value
  && !selectedMember.value.deleted
  && !isDetailLoading.value
  && !isSaving.value
));
const canSoftDelete = computed(() => Boolean(
  selectedMember.value
  && selectedMember.value.memberRole === 'USER'
  && !selectedMember.value.deleted
  && !isSaving.value
));
const selectedMemberStatusLabel = computed(() => (
  selectedMember.value?.deleted ? '탈퇴 회원' : '활성 회원'
));
const saveButtonLabel = computed(() => (
  isSaving.value ? '권한 저장 중...' : '권한 저장'
));

const filteredMembers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return members.value;
  }

  return members.value.filter((member) => {
    const haystacks = [
      member.name,
      member.loginId,
      member.email,
      member.phoneNumber,
    ]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());

    return haystacks.some((value) => value.includes(keyword));
  });
});

const pageCount = computed(() => Math.max(Math.ceil(filteredMembers.value.length / pageSize), 1));
const pagedMembers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredMembers.value.slice(start, start + pageSize);
});

function applyMembers(items) {
  members.value = items
    .map((item) => normalizeAdminMember(item))
    .filter((item) => item.memberId);
}

function syncSelectedMember(preferredMemberId = selectedMemberId.value) {
  const matchedMember = members.value.find((member) => member.memberId === preferredMemberId);

  if (matchedMember) {
    setSelectedMember(matchedMember);
    return;
  }

  if (members.value[0]) {
    setSelectedMember(members.value[0]);
    return;
  }

  selectedMemberId.value = '';
  selectedMember.value = null;
  roleDraft.value = 'USER';
}

function setSelectedMember(member) {
  selectedMemberId.value = member.memberId;
  selectedMember.value = member;
  roleDraft.value = member.memberRole;
  statusMessage.value = '';
}

async function loadMembers(options = {}) {
  const {
    preferredMemberId = selectedMemberId.value,
  } = options;
  isLoading.value = true;
  loadErrorMessage.value = '';
  const previousSelectedMemberId = selectedMemberId.value;

  try {
    const payload = await getAdminMembers();
    applyMembers(normalizeArrayPayload(payload, []));
  } catch (error) {
    applyMembers([]);
    loadErrorMessage.value = resolveAdminActionErrorMessage(error, '회원 목록을 불러오지 못했습니다.');
    return false;
  } finally {
    isLoading.value = false;
  }

  syncSelectedMember(preferredMemberId);

  if (selectedMemberId.value && selectedMemberId.value === previousSelectedMemberId) {
    await loadMemberDetail(selectedMemberId.value);
  }

  return true;
}

async function loadMemberDetail(memberId) {
  const fallbackMember = members.value.find((member) => member.memberId === memberId);
  if (!fallbackMember) {
    return;
  }

  const requestToken = ++latestDetailRequestToken;
  isDetailLoading.value = true;

  try {
    const payload = await getAdminMemberDetail(memberId);
    if (requestToken !== latestDetailRequestToken || selectedMemberId.value !== memberId) {
      return;
    }
    const normalizedMember = normalizeAdminMember(normalizeObjectPayload(payload, fallbackMember));
    selectedMember.value = normalizedMember;
    roleDraft.value = normalizedMember.memberRole;
  } catch {
    if (requestToken !== latestDetailRequestToken || selectedMemberId.value !== memberId) {
      return;
    }
    selectedMember.value = fallbackMember;
    roleDraft.value = fallbackMember.memberRole;
  } finally {
    if (requestToken === latestDetailRequestToken) {
      isDetailLoading.value = false;
    }
  }
}

async function submitRoleChange() {
  if (!selectedMember.value || selectedMember.value.deleted) {
    return;
  }

  const memberId = selectedMember.value.memberId;
  isSaving.value = true;
  statusMessage.value = '';

  try {
    await updateAdminMemberRole(memberId, roleDraft.value);
    const didLoadFromServer = await loadMembers({
      preferredMemberId: memberId,
    });
    statusMessage.value = didLoadFromServer
      ? '회원 권한을 변경했습니다.'
      : '권한은 변경됐지만 목록 재조회는 실패했습니다.';
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '회원 권한 변경에 실패했습니다.');
  }

  isSaving.value = false;
}

async function removeSelectedMember() {
  if (!canSoftDelete.value) {
    return;
  }

  const confirmed = await requestConfirm({
    title: '회원 탈퇴 처리',
    message: [
      `"${selectedMember.value.name}" 회원을 탈퇴 처리할까요?`,
      '',
      '진행 중인 주문 또는 취소되지 않은 결제가 있으면 처리되지 않습니다.',
    ].join('\n'),
    confirmLabel: '탈퇴 처리',
  });

  if (!confirmed) {
    return;
  }

  const memberId = selectedMember.value.memberId;
  isSaving.value = true;
  statusMessage.value = '';

  try {
    await deleteAdminMember(memberId);
    const didLoadFromServer = await loadMembers();
    statusMessage.value = didLoadFromServer
      ? '회원 탈퇴 처리를 완료했습니다.'
      : '회원은 탈퇴 처리됐지만 목록 재조회는 실패했습니다.';
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '회원 탈퇴 처리에 실패했습니다.');
  }

  isSaving.value = false;
}

watch(
  () => filteredMembers.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);

watch(selectedMemberId, (memberId) => {
  if (!memberId) {
    return;
  }

  loadMemberDetail(memberId);
});

onMounted(loadMembers);
</script>

<template>
  <section class="admin-members-manager">
    <AdminPanel title="회원 목록" description="회원 검색과 기본 상태 확인">
      <template #action>
        <input
          v-model="searchKeyword"
          type="text"
          class="admin-members-manager__search"
          placeholder="이름, 아이디, 이메일 검색"
        />
      </template>

      <div class="admin-members-manager__table">
        <div class="admin-members-manager__head">
          <span>이름</span>
          <span>아이디</span>
          <span>이메일</span>
          <span>권한</span>
          <span>가입일</span>
        </div>

        <button
          v-for="member in pagedMembers"
          :key="member.memberId"
          type="button"
          class="admin-members-manager__row"
          :class="{ 'is-active': selectedMemberId === member.memberId, 'is-deleted': member.deleted }"
          @click="setSelectedMember(member)"
        >
          <strong>
            {{ member.name }}
            <em v-if="member.deleted" class="admin-members-manager__badge">탈퇴</em>
          </strong>
          <span>{{ member.loginId }}</span>
          <span>{{ member.email }}</span>
          <span>{{ member.memberRole }}</span>
          <span>{{ formatAdminDate(member.createdAt) }}</span>
        </button>

        <CommonStatePanel
          v-if="!pagedMembers.length"
          :tone="isLoading ? 'loading' : loadErrorMessage ? 'error' : 'neutral'"
          :title="isLoading ? '회원 목록을 불러오는 중입니다.' : loadErrorMessage ? '회원 목록을 불러오지 못했습니다.' : '표시할 회원이 없습니다.'"
          :description="loadErrorMessage"
          compact
        />
      </div>

      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>

    <AdminPanel title="회원 상세" description="기본 정보와 권한 관리">
      <div v-if="selectedMember" class="admin-members-manager__detail">
        <div class="admin-members-manager__info-grid">
          <article>
            <span>이름</span>
            <strong>{{ selectedMember.name }}</strong>
          </article>
          <article>
            <span>아이디</span>
            <strong>{{ selectedMember.loginId }}</strong>
          </article>
          <article>
            <span>이메일</span>
            <strong>{{ selectedMember.email || '-' }}</strong>
          </article>
          <article>
            <span>연락처</span>
            <strong>{{ selectedMember.phoneNumber || '-' }}</strong>
          </article>
          <article>
            <span>우편번호</span>
            <strong>{{ selectedMember.zoneCode || '-' }}</strong>
          </article>
          <article>
            <span>회원 상태</span>
            <strong>{{ selectedMemberStatusLabel }}</strong>
          </article>
          <article>
            <span>가입 시각</span>
            <strong>{{ formatAdminDateTime(selectedMember.createdAt) }}</strong>
          </article>
        </div>

        <article class="admin-members-manager__address">
          <span>주소</span>
          <strong>{{ selectedMember.addressMain || selectedMember.address || '-' }}</strong>
          <p v-if="selectedMember.addressDetail">{{ selectedMember.addressDetail }}</p>
        </article>

        <div class="admin-members-manager__role-editor">
          <label>
            <span>권한</span>
            <select v-model="roleDraft" :disabled="!canChangeRole">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>

          <div class="admin-members-manager__actions">
            <button type="button" class="admin-members-manager__primary" :disabled="!canChangeRole" @click="submitRoleChange">
              {{ saveButtonLabel }}
            </button>
            <button
              type="button"
              class="admin-members-manager__secondary"
              :disabled="!canSoftDelete"
              @click="removeSelectedMember"
            >
              {{ selectedMember.deleted ? '탈퇴 처리됨' : '회원 탈퇴 처리' }}
            </button>
          </div>
        </div>

        <p v-if="selectedMember.deleted" class="admin-members-manager__notice">
          탈퇴 처리된 회원은 권한 변경이나 추가 수정이 불가능합니다.
        </p>

        <p v-if="statusMessage" class="admin-members-manager__status">{{ statusMessage }}</p>
      </div>

      <CommonStatePanel
        v-else
        :tone="loadErrorMessage ? 'error' : 'neutral'"
        :title="loadErrorMessage ? '회원 상세를 표시할 수 없습니다.' : '회원을 선택하면 상세 정보가 표시됩니다.'"
        :description="loadErrorMessage"
        align="left"
        compact
      />
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-members-manager {
  display: grid;
  gap: 40px;
}

.admin-members-manager__search {
  width: min(320px, 100%);
  height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  box-sizing: border-box;
}

.admin-members-manager__table {
  border-bottom: 1px solid #ededed;
}

.admin-members-manager__head,
.admin-members-manager__row {
  display: grid;
  grid-template-columns: 140px 160px minmax(0, 1fr) 100px 120px;
  gap: 16px;
  align-items: center;
}

.admin-members-manager__head {
  padding: 0 0 14px;
  color: #666666;
  font-size: 13px;
}

.admin-members-manager__row {
  width: 100%;
  padding: 16px 0;
  border: 0;
  border-top: 1px solid #efefef;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.admin-members-manager__row.is-active {
  background: #f7f9fb;
}

.admin-members-manager__row.is-deleted {
  color: #8c8c8c;
}

.admin-members-manager__row strong,
.admin-members-manager__row span,
.admin-members-manager__info-grid strong,
.admin-members-manager__address strong,
.admin-members-manager__address p {
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-members-manager__badge {
  display: inline-flex;
  margin-left: 8px;
  padding: 2px 7px;
  border: 1px solid #d0d7e2;
  color: #6f7d92;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  vertical-align: middle;
}

.admin-members-manager__detail {
  display: grid;
  gap: 18px;
}

.admin-members-manager__info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.admin-members-manager__info-grid article,
.admin-members-manager__address,
.admin-members-manager__role-editor {
  padding: 18px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
}

.admin-members-manager__info-grid span,
.admin-members-manager__address span,
.admin-members-manager__role-editor span {
  display: block;
  color: #777777;
  font-size: 13px;
}

.admin-members-manager__info-grid strong,
.admin-members-manager__address strong {
  display: block;
  margin-top: 10px;
  color: #111111;
  font-size: 18px;
  line-height: 1.4;
}

.admin-members-manager__address p {
  margin: 8px 0 0;
  color: #444444;
  font-size: 14px;
  line-height: 1.5;
}

.admin-members-manager__role-editor {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  gap: 14px;
}

.admin-members-manager__role-editor label {
  display: grid;
  gap: 8px;
  width: 200px;
}

.admin-members-manager__role-editor select {
  width: 200px;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
}

.admin-members-manager__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.admin-members-manager__primary,
.admin-members-manager__secondary {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  cursor: pointer;
}

.admin-members-manager__primary {
  border-color: #111111;
  background: #111111;
  color: #ffffff;
}

.admin-members-manager__status,
.admin-members-manager__empty,
.admin-members-manager__notice {
  color: #666666;
  font-size: 14px;
}

.admin-members-manager__status,
.admin-members-manager__notice {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
  line-height: 1.6;
}

@media (max-width: 1024px) {
  .admin-members-manager__head,
  .admin-members-manager__row,
  .admin-members-manager__info-grid {
    grid-template-columns: 1fr;
  }

  .admin-members-manager__head {
    display: none;
  }

  .admin-members-manager__row {
    gap: 8px;
    align-items: start;
  }
}

@media (max-width: 720px) {
  .admin-members-manager__search,
  .admin-members-manager__role-editor select {
    width: 100%;
  }

  .admin-members-manager__role-editor {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-members-manager__role-editor label {
    width: 100%;
  }

  .admin-members-manager__actions {
    flex-direction: column;
  }

  .admin-members-manager__primary,
  .admin-members-manager__secondary {
    width: 100%;
  }
}
</style>
