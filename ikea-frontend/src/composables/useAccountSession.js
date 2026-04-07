import { computed, reactive, shallowRef } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { ROUTE_PATHS } from '../constants/routes';
import { logoutAuth } from '../services/authService';
import { getCurrentMember, loginMember } from '../services/memberService';
import { useAccountStore } from '../stores/account';
import { resolveLoginErrorMessage } from '../utils/apiErrorMessage';

function unwrapPayload(payload) {
  return payload?.data ?? payload ?? {};
}

function extractTokens(payload) {
  const source = unwrapPayload(payload);
  const candidate = source.tokens ?? source.token ?? source.auth ?? source;

  return {
    accessToken: candidate.accessToken ?? candidate.access ?? '',
    refreshToken: candidate.refreshToken ?? candidate.refresh ?? '',
  };
}

function extractMemberSession(payload) {
  const source = unwrapPayload(payload);
  const candidate = source.member ?? source.me ?? source.user ?? source.profile ?? source;

  if (candidate && (candidate.memberId || candidate.loginId || candidate.name || candidate.memberName)) {
    return candidate;
  }

  return null;
}

function decodeJwtPayload(token = '') {
  const payloadSegment = String(token ?? '').split('.')[1];

  if (!payloadSegment) {
    return null;
  }

  try {
    const normalizedPayload = payloadSegment
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(Math.ceil(payloadSegment.length / 4) * 4, '=');
    const decodedPayload = atob(normalizedPayload);
    return JSON.parse(decodedPayload);
  } catch {
    return null;
  }
}

function buildFallbackMemberSession(tokens, loginIdValue) {
  const claims = decodeJwtPayload(tokens.accessToken);
  const resolvedLoginId = String(claims?.sub ?? loginIdValue ?? '').trim();
  const resolvedRole = String(claims?.role ?? '').trim();

  if (!resolvedLoginId && !resolvedRole) {
    return null;
  }

  return {
    loginId: resolvedLoginId,
    memberName: resolvedLoginId,
    role: resolvedRole,
    memberRole: resolvedRole,
  };
}

function resolveRedirectPath(redirectPath, role = '') {
  if (typeof redirectPath === 'string' && redirectPath.startsWith('/')) {
    return redirectPath;
  }

  if (String(role ?? '').trim().toUpperCase() === 'ADMIN') {
    return ROUTE_PATHS.adminDashboard;
  }

  return ROUTE_PATHS.home;
}

export function useAccountSession() {
  const router = useRouter();
  const route = useRoute();
  const accountStore = useAccountStore();
  const loginForm = reactive({
    loginId: '',
    password: '',
  });
  const loginError = shallowRef('');
  const loginSubmitting = shallowRef(false);
  const logoutSubmitting = shallowRef(false);
  const {
    accessToken,
    loggedIn,
    loginId,
    memberName,
    profileHydrated,
    profileRequested,
    refreshToken,
  } = storeToRefs(accountStore);

  const displayName = computed(() => memberName.value || loginId.value || '');

  async function hydrateCurrentMember(options = {}) {
    const { force = false, silent = true } = options;

    if (!accessToken.value) {
      return null;
    }

    if (!force && (profileHydrated.value || profileRequested.value)) {
      return null;
    }

    accountStore.setProfileRequested(true);

    try {
      const response = await getCurrentMember();
      const session = extractMemberSession(response);

      if (session) {
        accountStore.setMemberSession(session);
      }

      accountStore.setProfileRequested(false);
      accountStore.setProfileHydrated(true);
      return response;
    } catch (error) {
      accountStore.setProfileRequested(false);

      if (!silent) {
        throw error;
      }

      return null;
    }
  }

  async function submitLogin(options = {}) {
    const { redirectPath = route.query.redirect } = options;
    loginError.value = '';
    loginSubmitting.value = true;

    try {
      const response = await loginMember({
        loginId: loginForm.loginId.trim(),
        password: loginForm.password,
      });
      const tokens = extractTokens(response);

      if (!tokens.accessToken) {
        loginError.value = '로그인 정보를 다시 확인해 주세요.';
        return null;
      }

      accountStore.setTokens(tokens);
      accountStore.setProfileRequested(false);
      accountStore.setProfileHydrated(false);

      const memberSession = extractMemberSession(response)
        ?? buildFallbackMemberSession(tokens, loginForm.loginId);

      if (memberSession) {
        accountStore.setMemberSession(memberSession);
      }

      await hydrateCurrentMember({ force: true, silent: true });

      router.push(resolveRedirectPath(redirectPath, accountStore.role));
      return response;
    } catch (error) {
      loginError.value = resolveLoginErrorMessage(error);
      return null;
    } finally {
      loginSubmitting.value = false;
    }
  }

  async function submitLogout() {
    if (logoutSubmitting.value) {
      return;
    }

    logoutSubmitting.value = true;

    try {
      if (refreshToken.value) {
        await logoutAuth(refreshToken.value);
      }
    } catch {
      // Preserve the current UX even if the backend logout endpoint is not ready yet.
    } finally {
      accountStore.clearAuth();
      logoutSubmitting.value = false;
    }

    router.push(ROUTE_PATHS.home);
  }

  return {
    displayName,
    hydrateCurrentMember,
    loggedIn,
    loginError,
    loginForm,
    loginSubmitting,
    logoutSubmitting,
    submitLogin,
    submitLogout,
  };
}
