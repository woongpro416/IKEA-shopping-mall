import {
  backendCategories,
  catalogProducts,
  CATEGORY_ROUTE_MAP,
  DEFAULT_CATEGORY,
  getProductDetailSeed,
} from '../data/catalog';
import { getProductDetailContent } from '../data/productDetailContent';

export function getFallbackCatalogCategories() {
  return backendCategories;
}

export function getFallbackCatalogProducts() {
  return catalogProducts;
}

export function getFallbackCategoryRouteMap() {
  return CATEGORY_ROUTE_MAP;
}

export function getDefaultFallbackCategory() {
  return DEFAULT_CATEGORY;
}

export function getFallbackCatalogProductDetailSeed(productId) {
  return getProductDetailSeed(productId);
}

export function getFallbackCatalogProductDetailContent(product) {
  return getProductDetailContent(product);
}
