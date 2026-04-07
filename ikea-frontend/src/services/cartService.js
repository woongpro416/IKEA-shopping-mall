import httpRequester from '../libs/httpRequester';
import {
  createCommerceRecommendations,
} from '../data/commerceSeed';
import { useAccountStore } from '../stores/account';

function resolveCurrentMemberId() {
  const accountStore = useAccountStore();
  accountStore.hydrate();

  if (
    accountStore.memberId === null
    || accountStore.memberId === undefined
    || accountStore.memberId === ''
  ) {
    return null;
  }

  return accountStore.memberId;
}

function shouldFallbackCartRequest(error) {
  return [400, 404, 405].includes(Number(error?.status ?? 0));
}

async function runCartRequestWithFallback(requestFactories = []) {
  let lastError = null;

  for (let index = 0; index < requestFactories.length; index += 1) {
    const requestFactory = requestFactories[index];

    try {
      return await requestFactory();
    } catch (error) {
      lastError = error;

      if (!shouldFallbackCartRequest(error) || index === requestFactories.length - 1) {
        throw error;
      }
    }
  }

  throw lastError ?? new Error('Cart request failed.');
}

function buildLegacyMemberCartPath(suffix = '') {
  const memberId = resolveCurrentMemberId();
  return memberId === null ? '' : `/cart/${memberId}${suffix}`;
}

export async function getMyCart() {
  return httpRequester.get('/cart');
}

export function getMemberCart() {
  return getMyCart();
}

export async function addCartItem(memberIdOrCartRequest, maybeCartRequest) {
  const cartRequest = maybeCartRequest === undefined ? memberIdOrCartRequest : maybeCartRequest;
  return httpRequester.post('/cart', cartRequest);
}

export function addGuestCartItem(cartRequest = {}, guestCartKey = '') {
  const normalizedGuestCartKey = String(guestCartKey ?? '').trim();

  return httpRequester.post(
    '/cart/guest',
    cartRequest,
    normalizedGuestCartKey
      ? { query: { guestCartKey: normalizedGuestCartKey } }
      : {},
  );
}

export function clearGuestCart(guestCartKey) {
  return httpRequester.delete('/cart/guest/clear', {
    query: { guestCartKey },
  });
}

export function updateCartItemQuantity(cartItemId, quantity) {
  return httpRequester.patch(
    `/cart/${cartItemId}/quantity`,
    { quantity },
    { query: { quantity } },
  );
}

export function deleteCartItem(cartItemId) {
  return httpRequester.delete(`/cart/${cartItemId}`);
}

export async function clearMyCart() {
  return httpRequester.delete('/cart/clear');
}

export function clearMemberCart() {
  return clearMyCart();
}

export function getFallbackCartItems() {
  return [];
}

export function getFallbackRecommendations(excludeIds = []) {
  return createCommerceRecommendations(excludeIds);
}
