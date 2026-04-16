<script setup>
defineProps({
  selectedMainFiles: {
    type: Array,
    required: true,
  },
  selectedGalleryFiles: {
    type: Array,
    required: true,
  },
  selectedDimensionFiles: {
    type: Array,
    required: true,
  },
  selectedProduct: {
    type: Object,
    default: null,
  },
  existingGalleryCount: {
    type: Number,
    required: true,
  },
  hasExistingDimensionImage: {
    type: Boolean,
    required: true,
  },
});

const emit = defineEmits([
  'main-file-change',
  'gallery-file-change',
  'dimension-file-change',
]);
</script>

<template>
  <section class="admin-products-manager__section">
    <header class="admin-products-manager__section-head">
      <h3>이미지 자료</h3>
    </header>

    <div class="admin-products-manager__form-grid">
      <label class="admin-products-manager__field-row">
        <span>대표 이미지</span>
        <div class="admin-products-manager__field-control">
          <input type="file" accept="image/*" @change="emit('main-file-change', $event)" />
          <small>{{ selectedMainFiles[0]?.name || selectedProduct?.image || '선택된 파일 없음' }}</small>
        </div>
      </label>

      <label class="admin-products-manager__field-row">
        <span>갤러리 이미지</span>
        <div class="admin-products-manager__field-control">
          <input type="file" accept="image/*" multiple @change="emit('gallery-file-change', $event)" />
          <small>
            {{
              selectedGalleryFiles.length
                ? `${selectedGalleryFiles.length}개 선택`
                : existingGalleryCount
                  ? `기존 ${existingGalleryCount}개 사용`
                  : '선택된 파일 없음'
            }}
          </small>
        </div>
      </label>

      <label class="admin-products-manager__field-row">
        <span>치수 이미지</span>
        <div class="admin-products-manager__field-control">
          <input type="file" accept="image/*" @change="emit('dimension-file-change', $event)" />
          <small>
            {{
              selectedDimensionFiles[0]?.name
                || (hasExistingDimensionImage ? '기존 이미지 사용' : '선택된 파일 없음')
            }}
          </small>
        </div>
      </label>
    </div>
  </section>
</template>

<style scoped>
.admin-products-manager__section {
  display: grid;
  gap: 16px;
}

.admin-products-manager__section-head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.admin-products-manager__section-head h3 {
  margin: 0;
  color: var(--text-strong);
  font-size: 20px;
  line-height: 1.3;
}

.admin-products-manager__form-grid {
  display: grid;
  gap: 14px;
}

.admin-products-manager__field-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.admin-products-manager__field-row > span {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.admin-products-manager__field-control {
  display: grid;
  gap: 8px;
  min-height: 72px;
  align-content: center;
  align-items: center;
}

.admin-products-manager__field-control input[type='file'] {
  display: block;
  width: 100%;
  min-height: var(--control-height);
  padding-top: 10px;
  padding-bottom: 10px;
  padding-inline: 14px;
  border: 1px solid var(--border-default);
  background: var(--surface-strong);
  font: inherit;
}

.admin-products-manager__field-control small {
  color: var(--text-muted-strong);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .admin-products-manager__field-row {
    grid-template-columns: 1fr;
    gap: 8px;
    align-items: start;
  }
}
</style>
