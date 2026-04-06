<script setup>
import { computed, onMounted, reactive, shallowRef, watch } from 'vue';
import { useCatalogStore } from '../../stores/catalog';
import AdminProductBasicSection from './AdminProductBasicSection.vue';
import AdminProductDetailSection from './AdminProductDetailSection.vue';
import AdminProductImagesSection from './AdminProductImagesSection.vue';
import AdminProductPreviewSection from './AdminProductPreviewSection.vue';
import AdminPagination from './AdminPagination.vue';
import AdminPanel from './AdminPanel.vue';
import CommonStatePanel from '../common/CommonStatePanel.vue';
import {
  createAdminProduct,
  deleteAdminProduct,
  getProductCatalog,
  updateAdminProduct,
} from '../../services/adminService';
import {
  formatAdminCurrency,
  formatAdminDate,
  normalizeAdminProduct,
  normalizeArrayPayload,
} from '../../mappers/adminManagementMapper';
import { buildProductDetailPath } from '../../constants/routes';
import {
  ADMIN_PRODUCT_BADGE_OPTIONS,
  buildProductDeliveryMessage,
  buildProductOptionSummary,
  buildProductQuickFacts,
  createEmptyProductAttributes,
  getCategorySubtypeOptions,
  getProductAttributeFieldDefinitions,
  pickProductAttributes,
} from '../../constants/productAttributeConfig';
import { useFeedback } from '../../composables/useFeedback';
import { resolveAdminActionErrorMessage } from '../../utils/apiErrorMessage';

const catalogStore = useCatalogStore();
const categories = computed(() => catalogStore.backendCategories ?? []);
const products = shallowRef([]);
const isLoading = shallowRef(false);
const isSubmitting = shallowRef(false);
const searchKeyword = shallowRef('');
const currentPage = shallowRef(1);
const pageSize = 5;
const activeProductId = shallowRef('');
const selectedMainFiles = shallowRef([]);
const selectedGalleryFiles = shallowRef([]);
const selectedDimensionFiles = shallowRef([]);
const productFormRef = shallowRef(null);
const statusMessage = shallowRef('');
const loadErrorMessage = shallowRef('');
const { requestConfirm } = useFeedback();

function calculateDiscountRate(price, originalPrice) {
  const resolvedPrice = Number(price ?? 0);
  const resolvedOriginalPrice = Number(originalPrice ?? 0);

  if (!resolvedPrice || !resolvedOriginalPrice || resolvedOriginalPrice <= resolvedPrice) {
    return 0;
  }

  return Math.round(((resolvedOriginalPrice - resolvedPrice) / resolvedOriginalPrice) * 100);
}

const formState = reactive({
  name: '',
  brand: '',
  badge: '',
  label: '',
  typeSlug: '',
  price: '',
  originalPrice: '',
  categoryId: '',
  attributes: createEmptyProductAttributes(),
  heroHook: '',
  descriptionText: '',
  highlightsText: '',
  measurementsText: '',
});

const filteredProducts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return products.value;
  }

  return products.value.filter((product) => {
    const haystacks = [
      product.name,
      product.brand,
      product.categoryName,
      product.categorySlug,
      product.label,
    ]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());

    return haystacks.some((value) => value.includes(keyword));
  });
});

const pageCount = computed(() => Math.max(Math.ceil(filteredProducts.value.length / pageSize), 1));
const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProducts.value.slice(start, start + pageSize);
});

const selectedProduct = computed(
  () => products.value.find((product) => String(product.productId) === String(activeProductId.value)) ?? null,
);
const selectedCategory = computed(
  () => categories.value.find((item) => String(item.backendCategoryId) === String(formState.categoryId)) ?? categories.value[0] ?? null,
);
const subtypeOptions = computed(() => getCategorySubtypeOptions(selectedCategory.value));
const selectedSubtypeOption = computed(
  () => subtypeOptions.value.find((option) => option.slug === formState.typeSlug) ?? subtypeOptions.value[0] ?? null,
);
const badgeOptions = computed(() => {
  const merged = [
    ...ADMIN_PRODUCT_BADGE_OPTIONS,
    ...subtypeOptions.value.map((option) => ({
      value: option.label,
      label: option.label,
    })),
  ];

  const seen = new Set();
  return merged.filter((option) => {
    if (seen.has(option.value)) {
      return false;
    }

    seen.add(option.value);
    return true;
  });
});
const visibleAttributeFields = computed(() => getProductAttributeFieldDefinitions(
  selectedCategory.value?.slug ?? '',
  formState.typeSlug || 'all',
));
const formModeLabel = computed(() => (activeProductId.value ? '상품 수정' : '상품 등록'));
const priceValue = computed(() => Number(formState.price ?? 0));
const originalPriceValue = computed(() => Number(formState.originalPrice ?? 0));
const discountRatePreview = computed(() => calculateDiscountRate(priceValue.value, originalPriceValue.value));
const existingGalleryCount = computed(() => {
  if (!selectedProduct.value?.detailDraft?.galleryImages?.length) {
    return 0;
  }

  return selectedProduct.value.detailDraft.galleryImages.length;
});
const hasExistingDimensionImage = computed(() => Boolean(selectedProduct.value?.detailDraft?.dimensionImage));
const previewQuickFacts = computed(() => buildProductQuickFacts(buildPreviewProduct()).slice(0, 4));
const previewOptionCopy = computed(() => buildProductOptionSummary(buildPreviewProduct()));
const previewDeliveryCopy = computed(() => buildProductDeliveryMessage(buildPreviewProduct()));

function formatMultilineText(lines = []) {
  return lines
    .map((line) => String(line ?? '').trim())
    .filter(Boolean)
    .join('\n');
}

function formatMeasurementsText(measurements = []) {
  return measurements
    .filter((item) => item?.label && item?.value)
    .map((item) => `${item.label}: ${item.value}`)
    .join('\n');
}

function replaceAttributeValues(nextValues = {}) {
  Object.keys(formState.attributes).forEach((fieldId) => {
    formState.attributes[fieldId] = nextValues[fieldId] ?? '';
  });
}

function pruneAttributeValues() {
  const preserved = { ...formState.attributes };
  const nextValues = createEmptyProductAttributes();

  visibleAttributeFields.value.forEach((field) => {
    nextValues[field.id] = preserved[field.id] ?? '';
  });

  replaceAttributeValues(nextValues);
}

function readEditableAttributes(product, categorySlug, typeSlug = 'all') {
  return {
    ...createEmptyProductAttributes(),
    ...pickProductAttributes(product, categorySlug, typeSlug),
  };
}

function resetFileSelections() {
  selectedMainFiles.value = [];
  selectedGalleryFiles.value = [];
  selectedDimensionFiles.value = [];
}

function resolveDetailDraft(product) {
  const draft = product.detailDraft ?? {};
  const productDescription = String(product.description ?? '').trim();
  const productFeatures = Array.isArray(product.features) ? product.features.filter(Boolean) : [];

  return {
    heroHook: draft.heroHook ?? productDescription,
    descriptionText: formatMultilineText(
      Array.isArray(draft.description) && draft.description.length
        ? draft.description
        : productDescription
          ? [productDescription]
          : [],
    ),
    highlightsText: formatMultilineText(
      Array.isArray(draft.highlights) && draft.highlights.length
        ? draft.highlights
        : productFeatures,
    ),
    measurementsText: formatMeasurementsText(draft.measurements ?? []),
    galleryImages:
      draft.galleryImages
      ?? [product.image, product.altImage].filter(Boolean),
    dimensionImage: draft.dimensionImage ?? '',
  };
}

function syncSubtypeSelection() {
  if (!subtypeOptions.value.length) {
    formState.typeSlug = '';
    formState.label = selectedCategory.value?.label ?? '';
    return;
  }

  const matchedOption = subtypeOptions.value.find((option) => option.slug === formState.typeSlug)
    ?? subtypeOptions.value[0];

  formState.typeSlug = matchedOption.slug;
  formState.label = matchedOption.label;
}

function clearFormFields() {
  activeProductId.value = '';
  formState.name = '';
  formState.brand = 'HOMiO';
  formState.badge = '';
  formState.label = '';
  formState.typeSlug = '';
  formState.price = '';
  formState.originalPrice = '';
  formState.categoryId = categories.value[0]?.backendCategoryId
    ? String(categories.value[0].backendCategoryId)
    : '';
  replaceAttributeValues();
  formState.heroHook = '';
  formState.descriptionText = '';
  formState.highlightsText = '';
  formState.measurementsText = '';
  resetFileSelections();
  syncSubtypeSelection();
}

function beginCreateMode({ clearStatus = true } = {}) {
  clearFormFields();

  if (clearStatus) {
    statusMessage.value = '';
  }
}

function beginEditMode(product) {
  const normalizedProduct = normalizeAdminProduct(product, categories.value);
  const detailDraft = resolveDetailDraft(normalizedProduct);

  activeProductId.value = normalizedProduct.productId;
  formState.name = normalizedProduct.name;
  formState.brand = normalizedProduct.brand || 'HOMiO';
  formState.badge = normalizedProduct.badge ?? '';
  formState.price = normalizedProduct.price ? String(normalizedProduct.price) : '';
  formState.originalPrice = normalizedProduct.originalPrice ? String(normalizedProduct.originalPrice) : '';
  formState.categoryId = String(
    normalizedProduct.categoryId || selectedCategory.value?.backendCategoryId || categories.value[0]?.backendCategoryId || '',
  );
  formState.typeSlug = normalizedProduct.typeSlug || '';
  syncSubtypeSelection();
  replaceAttributeValues(
    readEditableAttributes(
      normalizedProduct,
      selectedCategory.value?.slug ?? normalizedProduct.categorySlug ?? '',
      formState.typeSlug || 'all',
    ),
  );
  formState.heroHook = detailDraft.heroHook;
  formState.descriptionText = detailDraft.descriptionText;
  formState.highlightsText = detailDraft.highlightsText;
  formState.measurementsText = detailDraft.measurementsText;
  resetFileSelections();
  statusMessage.value = `"${normalizedProduct.name}" 상품 수정 중입니다.`;
  requestAnimationFrame(() => {
    productFormRef.value?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  });
}

function applyProducts(items) {
  const normalizedItems = items
    .map((item) => normalizeAdminProduct(item, categories.value))
    .filter((item) => item.productId);

  products.value = normalizedItems;

  if (activeProductId.value) {
    const hasActiveProduct = normalizedItems.some((item) => item.productId === activeProductId.value);

    if (!hasActiveProduct) {
      beginCreateMode({ clearStatus: false });
    }
  }
}

async function loadProducts() {
  isLoading.value = true;
  loadErrorMessage.value = '';

  try {
    const payload = await getProductCatalog();
    applyProducts(normalizeArrayPayload(payload, []));
    return true;
  } catch (error) {
    applyProducts([]);
    loadErrorMessage.value = resolveAdminActionErrorMessage(error, '상품 목록을 불러오지 못했습니다.');
    return false;
  } finally {
    isLoading.value = false;
  }
}

function handleMainFileChange(event) {
  selectedMainFiles.value = [...(event.target.files ?? [])];
}

function handleGalleryFileChange(event) {
  selectedGalleryFiles.value = [...(event.target.files ?? [])];
}

function handleDimensionFileChange(event) {
  selectedDimensionFiles.value = [...(event.target.files ?? [])];
}

function buildPreviewProduct() {
  const attributeValues = Object.fromEntries(
    visibleAttributeFields.value
      .map((field) => [field.id, String(formState.attributes[field.id] ?? '').trim()])
      .filter(([, value]) => value),
  );

  return {
    id: activeProductId.value || '',
    productId: activeProductId.value || '',
    categorySlug: selectedCategory.value?.slug ?? '',
    categoryLabel: selectedCategory.value?.label ?? '',
    label: selectedSubtypeOption.value?.label ?? formState.label,
    typeSlug: formState.typeSlug || 'all',
    badge: formState.badge,
    attributes: attributeValues,
    ...attributeValues,
  };
}

function updateFormField({ field, value }) {
  formState[field] = value;
}

function updateAttributeField({ fieldId, value }) {
  formState.attributes[fieldId] = value;
}

async function submitProduct() {
  if (!formState.name.trim() || !formState.categoryId || !formState.typeSlug || !Number(formState.price)) {
    statusMessage.value = '상품명, 카테고리, 대표 분류, 가격을 확인해 주세요.';
    return;
  }

  isSubmitting.value = true;
  const payload = {
    name: formState.name.trim(),
    price: Number(formState.price || 0),
    categoryId: Number(formState.categoryId),
    files: [
      ...selectedMainFiles.value,
      ...selectedGalleryFiles.value,
      ...selectedDimensionFiles.value,
    ],
  };
  const isEditMode = Boolean(activeProductId.value);

  try {
    if (isEditMode) {
      await updateAdminProduct(activeProductId.value, payload);
    } else {
      await createAdminProduct(payload);
    }

    const didLoadFromServer = await loadProducts();
    await catalogStore.loadProducts().catch(() => {});
    statusMessage.value = didLoadFromServer
      ? (isEditMode ? '상품 정보를 수정했습니다.' : '새 상품을 등록했습니다.')
      : (isEditMode
        ? '상품 수정은 완료됐지만 목록 재조회는 실패했습니다.'
        : '상품 등록은 완료됐지만 목록 재조회는 실패했습니다.');
    beginCreateMode({ clearStatus: false });
  } catch (error) {
    statusMessage.value = isEditMode
      ? resolveAdminActionErrorMessage(error, '상품 수정에 실패했습니다.')
      : resolveAdminActionErrorMessage(error, '상품 등록에 실패했습니다.');
  } finally {
    isSubmitting.value = false;
  }
}

async function removeProduct(product) {
  const confirmed = await requestConfirm({
    title: '상품 삭제',
    message: `"${product.name}" 상품을 삭제할까요?`,
    confirmLabel: '삭제',
  });

  if (!confirmed) {
    return;
  }

  try {
    await deleteAdminProduct(product.productId);
    const didLoadFromServer = await loadProducts();
    await catalogStore.loadProducts().catch(() => {});
    statusMessage.value = didLoadFromServer
      ? '상품을 삭제했습니다.'
      : '상품 삭제는 완료됐지만 목록 재조회는 실패했습니다.';

    if (activeProductId.value === product.productId) {
      beginCreateMode({ clearStatus: false });
    }
  } catch (error) {
    statusMessage.value = resolveAdminActionErrorMessage(error, '상품 삭제에 실패했습니다.');
  }
}

watch(
  () => selectedCategory.value?.slug,
  () => {
    syncSubtypeSelection();
    pruneAttributeValues();
  },
);

watch(
  () => formState.typeSlug,
  () => {
    if (selectedSubtypeOption.value) {
      formState.label = selectedSubtypeOption.value.label;
    }
    pruneAttributeValues();
  },
);

watch(searchKeyword, () => {
  currentPage.value = 1;
});

watch(
  () => filteredProducts.value.length,
  () => {
    if (currentPage.value > pageCount.value) {
      currentPage.value = pageCount.value;
    }
  },
);

onMounted(async () => {
  await catalogStore.loadCategories().catch(() => {});
  await loadProducts();
  beginCreateMode();
});
</script>

<template>
  <section class="admin-products-manager">
    <AdminPanel title="상품 목록" description="등록된 상품을 확인하고 수정하거나 삭제합니다.">
      <template #action>
        <input
          v-model="searchKeyword"
          type="text"
          class="admin-products-manager__search"
          placeholder="상품명, 브랜드, 대표 분류 검색"
        />
      </template>

      <div class="admin-products-manager__table">
        <div class="admin-products-manager__head">
          <span>상품</span>
          <span>대표 분류</span>
          <span>가격</span>
          <span>할인</span>
          <span>등록일</span>
          <span>관리</span>
        </div>

        <article
          v-for="product in pagedProducts"
          :key="product.productId"
          class="admin-products-manager__row"
          :class="{ 'is-active': activeProductId === product.productId }"
        >
          <div class="admin-products-manager__product">
            <img :src="product.image" :alt="product.name" />
            <div>
              <strong>{{ product.name }}</strong>
              <span>{{ product.brand }} · {{ product.categoryName }}</span>
            </div>
          </div>
          <span>{{ product.label || '-' }}</span>
          <strong>{{ formatAdminCurrency(product.price) }}</strong>
          <span>{{ product.discountRate ? `${product.discountRate}%` : '-' }}</span>
          <span>{{ formatAdminDate(product.createdAt) }}</span>
          <div class="admin-products-manager__row-actions">
            <button type="button" @click="beginEditMode(product)">수정</button>
            <RouterLink :to="buildProductDetailPath(product.productId)">미리보기</RouterLink>
            <button type="button" @click="removeProduct(product)">삭제</button>
          </div>
        </article>

        <CommonStatePanel
          v-if="!pagedProducts.length"
          :tone="isLoading ? 'loading' : loadErrorMessage ? 'error' : 'neutral'"
          :title="isLoading ? '상품 목록을 불러오는 중입니다.' : loadErrorMessage ? '상품 목록을 불러오지 못했습니다.' : '표시할 상품이 없습니다.'"
          :description="loadErrorMessage"
          compact
        />
      </div>

      <AdminPagination v-model:current-page="currentPage" :page-count="pageCount" />
    </AdminPanel>

    <AdminPanel :title="formModeLabel" description="상품 페이지와 상세 페이지에 바로 반영될 기준값을 입력합니다.">
      <template v-if="activeProductId" #action>
        <button type="button" class="admin-products-manager__primary" @click="beginCreateMode">
          새 상품 작성
        </button>
      </template>

      <form ref="productFormRef" class="admin-products-manager__form" @submit.prevent="submitProduct">
        <AdminProductBasicSection
          :form-state="formState"
          :categories="categories"
          :subtype-options="subtypeOptions"
          :badge-options="badgeOptions"
          :visible-attribute-fields="visibleAttributeFields"
          :discount-rate-preview="discountRatePreview"
          @update-field="updateFormField"
          @update-attribute="updateAttributeField"
        />

        <AdminProductDetailSection :form-state="formState" @update-field="updateFormField" />

        <AdminProductImagesSection
          :selected-main-files="selectedMainFiles"
          :selected-gallery-files="selectedGalleryFiles"
          :selected-dimension-files="selectedDimensionFiles"
          :selected-product="selectedProduct"
          :existing-gallery-count="existingGalleryCount"
          :has-existing-dimension-image="hasExistingDimensionImage"
          @main-file-change="handleMainFileChange"
          @gallery-file-change="handleGalleryFileChange"
          @dimension-file-change="handleDimensionFileChange"
        />

        <AdminProductPreviewSection
          :preview-quick-facts="previewQuickFacts"
          :preview-option-copy="previewOptionCopy"
          :preview-delivery-copy="previewDeliveryCopy"
        />

        <div class="admin-products-manager__form-actions">
          <button type="button" class="admin-products-manager__secondary" @click="beginCreateMode">
            입력 초기화
          </button>
          <button type="submit" class="admin-products-manager__primary" :disabled="isSubmitting">
            {{ isSubmitting ? '저장 중...' : activeProductId ? '상품 수정' : '상품 등록' }}
          </button>
        </div>

        <p v-if="statusMessage" class="admin-products-manager__status">{{ statusMessage }}</p>
      </form>
    </AdminPanel>
  </section>
</template>

<style scoped>
.admin-products-manager {
  display: grid;
  gap: 40px;
}

.admin-products-manager__search {
  width: min(320px, 100%);
  height: var(--control-height-compact);
  padding: 0 14px;
  border: 1px solid var(--border-default);
  background: var(--surface-strong);
  box-sizing: border-box;
}

.admin-products-manager__table {
  border-bottom: 1px solid var(--border-muted);
}

.admin-products-manager__head,
.admin-products-manager__row {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) 160px 130px 90px 120px 190px;
  gap: 16px;
  align-items: center;
}

.admin-products-manager__head {
  padding: 0 0 14px;
  color: var(--text-muted-strong);
  font-size: 13px;
}

.admin-products-manager__row {
  padding: 16px 0;
  border-top: 1px solid var(--border-muted);
}

.admin-products-manager__row.is-active {
  background: var(--surface-soft);
}

.admin-products-manager__product {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.admin-products-manager__product img {
  width: 76px;
  height: 76px;
  border: 1px solid var(--border-subtle);
  object-fit: contain;
  background: var(--surface-soft);
}

.admin-products-manager__product strong {
  display: block;
  color: var(--text-strong);
  font-size: 15px;
  line-height: 1.4;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-products-manager__product span {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.admin-products-manager__row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-products-manager__row-actions > button:first-child {
  order: 1;
}

.admin-products-manager__row-actions > button:last-child {
  order: 2;
}

.admin-products-manager__row-actions > a,
.admin-products-manager__preview-link {
  order: 3;
}

.admin-products-manager__row-actions button,
.admin-products-manager__row-actions a,
.admin-products-manager__primary,
.admin-products-manager__secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--action-height);
  padding: 0 16px;
  border: 1px solid var(--border-default);
  background: var(--surface-strong);
  color: var(--text-strong);
  text-decoration: none;
  cursor: pointer;
}

.admin-products-manager__primary {
  border-color: var(--border-strong);
  background: var(--text-strong);
  color: var(--surface-strong);
}

.admin-products-manager__form {
  display: grid;
  gap: 24px;
}

.admin-products-manager__field-control small,
.admin-products-manager__status {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e6edf5;
  background: #f7f9fb;
  color: #556070;
  font-size: 13px;
  line-height: 1.6;
}

.admin-products-manager__form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.admin-products-manager__empty {
  padding-top: 16px;
  color: var(--text-muted-strong);
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .admin-products-manager__head,
  .admin-products-manager__row {
    grid-template-columns: 1fr;
  }

  .admin-products-manager__head {
    display: none;
  }

  .admin-products-manager__row {
    gap: 8px;
    align-items: start;
  }

  .admin-products-manager__row-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-products-manager__search {
    width: 100%;
  }

  .admin-products-manager__product {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 12px;
    align-items: start;
  }

  .admin-products-manager__product img {
    width: 64px;
    height: 64px;
  }

  .admin-products-manager__row-actions {
    grid-template-columns: 1fr;
  }

  .admin-products-manager__form-actions {
    flex-direction: column;
  }

  .admin-products-manager__form-actions button {
    width: 100%;
  }
}
</style>
