import { defineStore } from 'pinia';
import { normalizeProductCollection } from '../mappers/catalogMapper';
import { getBestProducts, getNewProducts } from '../services/productService';
import { getHomeContent } from '../services/homeService';

let bestProductsRequestPromise = null;
let newProductsRequestPromise = null;

export const useHomeStore = defineStore('home', {
  state: () => ({
    ...getHomeContent(),
    remoteBestProducts: [],
    remoteNewProducts: [],
    remoteBestProductsLoaded: false,
    remoteNewProductsLoaded: false,
  }),
  actions: {
    async loadRemoteBestProducts({ force = false } = {}) {
      if (!force && this.remoteBestProductsLoaded) {
        return this.remoteBestProducts;
      }

      if (!force && bestProductsRequestPromise) {
        return bestProductsRequestPromise;
      }

      bestProductsRequestPromise = (async () => {
      try {
        const response = await getBestProducts();
        this.remoteBestProducts = normalizeProductCollection(response, []);
        this.remoteBestProductsLoaded = true;
      } catch {
        this.remoteBestProducts = [];
        this.remoteBestProductsLoaded = false;
      } finally {
        bestProductsRequestPromise = null;
      }
      return this.remoteBestProducts;
      })();

      return bestProductsRequestPromise;
    },
    async loadRemoteNewProducts({ force = false } = {}) {
      if (!force && this.remoteNewProductsLoaded) {
        return this.remoteNewProducts;
      }

      if (!force && newProductsRequestPromise) {
        return newProductsRequestPromise;
      }

      newProductsRequestPromise = (async () => {
      try {
        const response = await getNewProducts();
        this.remoteNewProducts = normalizeProductCollection(response, []);
        this.remoteNewProductsLoaded = true;
      } catch {
        this.remoteNewProducts = [];
        this.remoteNewProductsLoaded = false;
      } finally {
        newProductsRequestPromise = null;
      }
      return this.remoteNewProducts;
      })();

      return newProductsRequestPromise;
    },
    async loadRemoteHomeProducts(options = {}) {
      await Promise.all([
        this.loadRemoteBestProducts(options),
        this.loadRemoteNewProducts(options),
      ]);
    },
  },
});
