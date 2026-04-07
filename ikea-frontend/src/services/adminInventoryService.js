import { normalizeProductCollection } from '../mappers/catalogMapper';
import { getProductList } from './productService';
import { getAdminProductStock, updateAdminProductStock } from './productStockService';

const ADMIN_SAFE_STOCK_STORAGE_KEY = 'homio-admin-inventory-safe-stock';

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

function normalizeProductId(value) {
  return String(value ?? '').trim();
}

function isTrackableProductId(productId) {
  return /^\d+$/.test(normalizeProductId(productId));
}

function normalizeInteger(value, fallback = 0) {
  const normalizedValue = Number(value);

  if (!Number.isFinite(normalizedValue)) {
    return fallback;
  }

  return Math.trunc(normalizedValue);
}

function formatInventoryTimestamp(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

function readStoredSafeStockMap() {
  if (!canUseStorage()) {
    return {};
  }

  try {
    const raw = window.localStorage.getItem(ADMIN_SAFE_STOCK_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function writeStoredSafeStockMap(safeStockMap) {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.setItem(ADMIN_SAFE_STOCK_STORAGE_KEY, JSON.stringify(safeStockMap));
}

async function loadCatalogProducts() {
  const response = await getProductList();
  return normalizeProductCollection(response, []);
}

async function loadAdminStock(productId) {
  if (!isTrackableProductId(productId)) {
    return null;
  }

  try {
    return await getAdminProductStock(Number(productId));
  } catch {
    return null;
  }
}

function buildInventoryItem(product, safeStockMap = {}, stockPayload = null) {
  const productId = normalizeProductId(product.productId ?? product.id);
  const stockSource = stockPayload?.data ?? stockPayload ?? {};
  const stock = normalizeInteger(stockSource.quantity, 0);
  const storedSafeStock = safeStockMap[productId];
  const safeStock = Math.max(0, normalizeInteger(storedSafeStock, 0));

  return {
    productId,
    name: product.name ?? '',
    categoryName: product.categoryName ?? product.categoryLabel ?? '-',
    image: product.imgPath ?? product.image ?? '',
    sku: `HM-${productId.slice(-5) || String(index + 1).padStart(5, '0')}`,
    stock,
    reserved: 0,
    safeStock,
    updatedAt: formatInventoryTimestamp(stockSource.updatedAt),
  };
}

export async function getAdminInventoryItems() {
  const products = await loadCatalogProducts();
  const safeStockMap = readStoredSafeStockMap();
  const stockPayloads = await Promise.all(
    products.map((product) => loadAdminStock(product.productId ?? product.id)),
  );

  return products.map((product, index) => (
    buildInventoryItem(product, safeStockMap, stockPayloads[index])
  ));
}

export async function adjustAdminInventoryItem(productId, { type, quantity, currentStock }) {
  const normalizedProductId = normalizeProductId(productId);

  if (!isTrackableProductId(normalizedProductId)) {
    throw new Error('재고 수량을 조정할 수 없는 상품입니다.');
  }

  const delta = Math.max(0, normalizeInteger(quantity, 0));
  const baseStock = Math.max(0, normalizeInteger(currentStock, 0));
  const nextQuantity = type === 'decrease'
    ? Math.max(0, baseStock - delta)
    : baseStock + delta;
  const response = await updateAdminProductStock(Number(normalizedProductId), {
    quantity: nextQuantity,
  });
  const payload = response?.data ?? response ?? {};

  return {
    productId: normalizedProductId,
    stock: normalizeInteger(payload.quantity, nextQuantity),
    updatedAt: formatInventoryTimestamp(payload.updatedAt),
  };
}

export function updateAdminInventorySafeStock(productId, { safeStock }) {
  const normalizedProductId = normalizeProductId(productId);
  const safeStockMap = readStoredSafeStockMap();
  const normalizedSafeStock = Math.max(0, normalizeInteger(safeStock, 0));
  const nextSafeStockMap = {
    ...safeStockMap,
    [normalizedProductId]: normalizedSafeStock,
  };

  writeStoredSafeStockMap(nextSafeStockMap);

  return {
    productId: normalizedProductId,
    safeStock: normalizedSafeStock,
    updatedAt: formatInventoryTimestamp(new Date()),
  };
}
