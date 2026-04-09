import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useAccountStore } from '../stores/account';
import { useAdminDashboardStore } from '../stores/adminDashboard';

export function useAdminDashboard() {
  const adminDashboardStore = useAdminDashboardStore();
  const accountStore = useAccountStore();
  const {
    dashboard,
    isDashboardLoading,
    loadErrorMessage,
  } = storeToRefs(adminDashboardStore);
  const {
    memberName,
    loginId,
  } = storeToRefs(accountStore);

  const operatorLabel = computed(
    () => memberName.value || loginId.value || '운영 관리자',
  );

  onMounted(() => {
    if (!adminDashboardStore.isDashboardLoading) {
      adminDashboardStore.loadDashboard();
    }
  });

  function removeMember(memberId) {
    adminDashboardStore.removeMember(memberId);
  }

  return {
    dashboard,
    isDashboardLoading,
    loadErrorMessage,
    operatorLabel,
    removeMember,
  };
}
