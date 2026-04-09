import { shallowRef } from 'vue';
import { getProductStock } from './productStockService';

const SOLD_OUT_FLAG_KEYS = ['isSoldOut', 'soldOut'];
const STOCK_VALUE_KEYS = [
  'stock',
  'stockQuantity',
  'availableStock',
  'inventoryQuantity',
  'quantityAvailable',
  'quantity',
];
const storefrontInventoryMapState = shallowRef(new Map());
const pendingStorefrontStockRequests = new Map();

function normalizeProductId(source = {}) {
  if (typeof source === 'string' || typeof source === 'number') {
    return String(source).trim();
  }

  return String(
    source.backendProductId
    ?? source.reviewProductId
    ?? source.productId
    ?? source.id
    ?? '',
  ).trim();
}

function normalizeInteger(value) {
  const nextValue = Number(value);

  if (!Number.isFinite(nextValue)) {
    return null;
  }

  return Math.trunc(nextValue);
}

function resolveExplicitSoldOut(source = {}) {
  for (const key of SOLD_OUT_FLAG_KEYS) {
    if (typeof source?.[key] === 'boolean') {
      return source[key];
    }
  }

  return null;
}

function resolveExplicitStock(source = {}) {
  for (const key of STOCK_VALUE_KEYS) {
    const nextValue = normalizeInteger(source?.[key]);

    if (nextValue !== null) {
      return nextValue;
    }
  }

  return null;
}

function isTrackableProductId(productId = '') {
  return /^\d+$/.test(String(productId ?? '').trim());
}

function unwrapStockPayload(payload) {
  return payload?.data ?? payload ?? {};
}

function createStorefrontInventoryEntry(productId, payload = {}) {
  const source = unwrapStockPayload(payload);

  return {
    productId: normalizeProductId(productId),
    stock: resolveExplicitStock(source),
    updatedAt: String(source.updatedAt ?? '').trim(),
    isTracked: true,
  };
}

function replaceStorefrontInventoryEntry(entry) {
  const nextInventoryMap = new Map(storefrontInventoryMapState.value);
  nextInventoryMap.set(entry.productId, entry);
  storefrontInventoryMapState.value = nextInventoryMap;
  return entry;
}

export function getStorefrontInventoryMap() {
  return storefrontInventoryMapState.value;
}

export async function fetchStorefrontInventory(productId, { force = false } = {}) {
  const normalizedProductId = normalizeProductId(productId);

  if (!isTrackableProductId(normalizedProductId)) {
    return null;
  }

  if (!force && storefrontInventoryMapState.value.has(normalizedProductId)) {
    return storefrontInventoryMapState.value.get(normalizedProductId) ?? null;
  }

  if (!force && pendingStorefrontStockRequests.has(normalizedProductId)) {
    return pendingStorefrontStockRequests.get(normalizedProductId);
  }

  const request = getProductStock(Number(normalizedProductId))
    .then((payload) => replaceStorefrontInventoryEntry(
      createStorefrontInventoryEntry(normalizedProductId, payload),
    ))
    .catch((error) => {
      if (error?.status === 403 || error?.status === 404) {
        return replaceStorefrontInventoryEntry({
          productId: normalizedProductId,
          stock: null,
          updatedAt: '',
          isTracked: false,
        });
      }

      throw error;
    })
    .finally(() => {
      pendingStorefrontStockRequests.delete(normalizedProductId);
    });

  pendingStorefrontStockRequests.set(normalizedProductId, request);
  return request;
}

export async function primeStorefrontInventory(products = []) {
  const productIds = Array.from(
    new Set(
      (Array.isArray(products) ? products : [products])
        .map((product) => normalizeProductId(product))
        .filter((productId) => isTrackableProductId(productId)),
    ),
  );

  await Promise.all(productIds.map((productId) => fetchStorefrontInventory(productId).catch(() => null)));
  return storefrontInventoryMapState.value;
}

export function resolveStorefrontAvailability(source = {}, inventoryMap = getStorefrontInventoryMap()) {
  const productId = normalizeProductId(source);
  const explicitSoldOut = resolveExplicitSoldOut(source);
  const explicitStock = resolveExplicitStock(source);
  const fallbackInventory = productId ? inventoryMap.get(productId) ?? null : null;
  const fallbackStock = normalizeInteger(fallbackInventory?.stock);
  const trackedByInventory = Boolean(fallbackInventory?.isTracked) || fallbackStock !== null;
  const availableStock = explicitStock ?? fallbackStock;
  const isSoldOut = explicitSoldOut ?? (availableStock !== null ? availableStock <= 0 : false);
  const isTracked = explicitSoldOut !== null || explicitStock !== null || trackedByInventory;

  return {
    availableStock,
    isSoldOut,
    isTracked,
    stockNotice: isSoldOut ? '품절' : '',
    stockMessage: isSoldOut ? '현재 재고가 모두 소진되어 구매할 수 없습니다.' : '',
  };
}

export function decorateStorefrontItem(source = {}, inventoryMap = getStorefrontInventoryMap()) {
  return {
    ...source,
    ...resolveStorefrontAvailability(source, inventoryMap),
  };
}

export function decorateStorefrontItems(items = [], inventoryMap = getStorefrontInventoryMap()) {
  return Array.isArray(items)
    ? items.map((item) => decorateStorefrontItem(item, inventoryMap))
    : [];
}
