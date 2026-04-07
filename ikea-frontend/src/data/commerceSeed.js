import {
  catalogProducts,
  findCatalogProductById,
} from './catalog';
import { useCatalogStore } from '../stores/catalog';

const RECOMMENDATION_LIMIT = 4;
const RECOMMENDATION_SESSION_KEY = 'homio-cart-recommendation-seed';

function resolveProduct(productId) {
  const normalizedProductId = String(productId ?? '').trim();
  const catalogStore = resolveCatalogStore();
  const product = catalogStore?.findProductById?.(normalizedProductId) ?? findCatalogProductById(normalizedProductId);

  if (!product) {
    throw new Error(`Unknown commerce seed product: ${productId}`);
  }

  return product;
}

function resolveCatalogStore() {
  try {
    return useCatalogStore();
  } catch {
    return null;
  }
}

function resolveCatalogProducts() {
  const catalogStore = resolveCatalogStore();
  const runtimeProducts = Array.isArray(catalogStore?.products) ? catalogStore.products : [];

  return runtimeProducts.length ? runtimeProducts : catalogProducts;
}

function getPrimaryImage(product) {
  return product.image ?? product.imgPath ?? '';
}

function getOptionSummary(product) {
  const parts = [];

  if (product.color) {
    parts.push(product.color);
  }

  if (product.material) {
    parts.push(product.material);
  }

  if (!parts.length && Array.isArray(product.features)) {
    parts.push(...product.features.slice(0, 2));
  }

  return parts.length ? parts.join(' / ') : product.label;
}

function getDeliveryLabel(product) {
  return `${product.categoryLabel} 배송`;
}

function getShippingText(product) {
  if (['sofa', 'bed-mattress', 'kitchen-furniture'].includes(product.categorySlug)) {
    return '배송비/설치비 확인';
  }

  return '일반배송';
}

function getShippingSubText(product) {
  if (['sofa', 'bed-mattress', 'kitchen-furniture'].includes(product.categorySlug)) {
    return '지역 및 설치 환경에 따라 결제 단계에서 안내됩니다.';
  }

  return `${product.categoryLabel} 상품 기준으로 결제 단계에서 배송 조건을 확인합니다.`;
}

function getOriginalPrice(product) {
  return product.originalPrice ?? product.price;
}

export function createCommerceCartItem(productId, overrides = {}) {
  const product = resolveProduct(productId);

  return {
    id: `cart-${product.id}`,
    productId: String(product.id),
    selected: true,
    brand: product.brand,
    seller: 'HOMiO',
    deliveryLabel: getDeliveryLabel(product),
    deliverySubLabel: '배송 일정과 배송비는 결제 단계에서 다시 안내됩니다.',
    name: product.name,
    option: `옵션: ${getOptionSummary(product)}`,
    image: getPrimaryImage(product),
    quantity: 1,
    price: product.price,
    originalPrice: getOriginalPrice(product),
    shippingText: getShippingText(product),
    shippingSubText: getShippingSubText(product),
    shippingNote: getShippingSubText(product),
    shippingLink: '지역별 배송 조건 안내',
    detailPath: `/product/${product.id}`,
    categoryLabel: product.categoryLabel,
    label: product.label,
    color: product.color ?? '',
    material: product.material ?? '',
    ...overrides,
  };
}

function buildRecommendation(productId) {
  const product = resolveProduct(productId);

  return {
    id: `recommend-${product.id}`,
    productId: String(product.id),
    title: product.name,
    image: getPrimaryImage(product),
    originalPrice: getOriginalPrice(product),
    price: product.price,
    badge: product.label,
    detailPath: `/product/${product.id}`,
    brand: product.brand,
  };
}

function canUseSessionStorage() {
  return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined';
}

function getRecommendationSessionSeed() {
  if (!canUseSessionStorage()) {
    return 'server';
  }

  const storedSeed = String(window.sessionStorage.getItem(RECOMMENDATION_SESSION_KEY) ?? '').trim();

  if (storedSeed) {
    return storedSeed;
  }

  const nextSeed = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  window.sessionStorage.setItem(RECOMMENDATION_SESSION_KEY, nextSeed);
  return nextSeed;
}

function createSeededRandom(seed) {
  let hash = 2166136261;

  for (let index = 0; index < seed.length; index += 1) {
    hash ^= seed.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }

  return () => {
    hash += 0x6d2b79f5;
    let next = Math.imul(hash ^ (hash >>> 15), 1 | hash);
    next ^= next + Math.imul(next ^ (next >>> 7), 61 | next);
    return ((next ^ (next >>> 14)) >>> 0) / 4294967296;
  };
}

function shuffleWithSeed(items, seed) {
  const randomizedItems = [...items];
  const nextRandom = createSeededRandom(seed);

  for (let index = randomizedItems.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(nextRandom() * (index + 1));
    [randomizedItems[index], randomizedItems[swapIndex]] = [randomizedItems[swapIndex], randomizedItems[index]];
  }

  return randomizedItems;
}

export function createCommerceCartSeed() {
  return [];
}

export function createCommerceRecommendations(excludeIds = []) {
  const blocked = new Set(excludeIds.map((value) => String(value)));
  const sessionSeed = getRecommendationSessionSeed();
  const cartSeed = excludeIds
    .map((value) => String(value ?? '').trim())
    .filter(Boolean)
    .sort()
    .join('|');
  const recommendationSeed = `${sessionSeed}:${cartSeed}`;

  const randomizedCatalogIds = shuffleWithSeed(
    resolveCatalogProducts()
      .filter((product) => !blocked.has(String(product.id)))
      .filter((product) => String(product.image ?? '').trim())
      .map((product) => String(product.id)),
    recommendationSeed,
  );

  const candidateIds = [...randomizedCatalogIds];
  const dedupedIds = [...new Set(candidateIds)].slice(0, RECOMMENDATION_LIMIT);

  return dedupedIds.map((productId) => buildRecommendation(productId));
}

