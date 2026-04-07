import httpRequester from '../libs/httpRequester';
import { getBackendCategoryId } from '../constants/routes';
import {
  getFallbackCatalogProductDetailContent,
  getFallbackCatalogProducts,
} from './catalogFallbackService';

export function getProductList() {
  return httpRequester.get('/product');
}

export function getProductCategory(categoryId) {
  return httpRequester.get(`/product/category/${categoryId}`);
}

export function getProductCategoryBySlug(categorySlug) {
  return getProductCategory(getBackendCategoryId(categorySlug));
}

export function searchProducts(keyword) {
  return httpRequester.get('/product/search', {
    params: { keyword },
  });
}

export function getProductDetail(productId) {
  return httpRequester.get(`/product/${productId}`);
}

export function getNewProducts() {
  return httpRequester.get('/product/new');
}

export function getBestProducts() {
  return httpRequester.get('/product/best');
}

export function getRecommendedProducts(categoryId) {
  return httpRequester.get('/product/recommend', {
    params: { categoryId },
  });
}

export function getRecommendedProductsBySlug(categorySlug) {
  return getRecommendedProducts(getBackendCategoryId(categorySlug));
}

export function getFallbackProductList() {
  return getFallbackCatalogProducts();
}

export function getFallbackProductDetailContent(product) {
  return getFallbackCatalogProductDetailContent(product);
}
