<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
});

const emit = defineEmits(['activate']);
</script>

<template>
  <section class="hs-shortcuts">
    <button
      v-for="item in items"
      :key="item.id"
      class="hs-shortcut"
      :class="item.type === 'promo' ? 'is-promo' : 'is-category'"
      type="button"
      @click="emit('activate', item)"
    >
      <span class="hs-shortcut__thumb">
        <template v-if="item.type === 'promo'">
          <svg v-if="item.icon === 'ticket'" viewBox="0 0 48 48" fill="none">
            <path d="M8 16H40V24C37.79 24 36 25.79 36 28C36 30.21 37.79 32 40 32V40H8V32C10.21 32 12 30.21 12 28C12 25.79 10.21 24 8 24V16Z" stroke="currentColor" stroke-width="2.6" />
            <path d="M18 19V24" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" />
            <path d="M18 32V37" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" />
            <path d="M30 20L20 30" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" />
            <path d="M28 30H28.02" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
            <path d="M20 22H20.02" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
          </svg>
          <svg v-else-if="item.icon === 'grid'" viewBox="0 0 48 48" fill="none">
            <rect x="8" y="10" width="12" height="12" rx="2.5" stroke="currentColor" stroke-width="2.6" />
            <rect x="28" y="10" width="12" height="12" rx="2.5" stroke="currentColor" stroke-width="2.6" />
            <rect x="8" y="26" width="12" height="12" rx="2.5" stroke="currentColor" stroke-width="2.6" />
            <rect x="28" y="26" width="12" height="12" rx="2.5" stroke="currentColor" stroke-width="2.6" />
          </svg>
          <svg v-else viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="13" stroke="currentColor" stroke-width="2.8" />
            <path d="M24 15V24L30 27" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" />
            <path d="M16 12H8M40 12H32" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" />
          </svg>
        </template>
        <img
          v-else
          :src="item.image"
          :alt="item.label"
          :style="{
            objectFit: item.imageFit || 'cover',
            objectPosition: item.imagePosition || 'center center',
            transform: item.imageScale ? `scale(${item.imageScale})` : undefined,
          }"
          loading="eager"
          decoding="async"
        />
      </span>
      <span class="hs-shortcut__label">{{ item.shortLabel ?? item.label }}</span>
    </button>
  </section>
</template>

<style scoped>
.hs-shortcuts {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 14px;
  width: min(1280px, calc(100% - 40px));
  margin: -26px auto 0;
  position: relative;
  z-index: 3;
}

.hs-shortcut {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 148px;
  border: 1px solid var(--hs-line);
  border-radius: 14px;
  background: #ffffff;
  padding: 16px 12px;
  cursor: pointer;
}

.hs-shortcut__thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 76px;
  overflow: hidden;
}

.hs-shortcut__thumb svg {
  width: 100%;
  height: 100%;
}

.hs-shortcut.is-category .hs-shortcut__thumb {
  border-radius: 10px;
  background: transparent;
  padding: 0;
}

.hs-shortcut__thumb img {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
  transition: transform 180ms ease;
}

.hs-shortcut__label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 38px;
  color: var(--hs-ink);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  text-align: center;
  word-break: keep-all;
}

.hs-shortcut.is-promo {
  background: var(--hs-soft);
}

.hs-shortcut.is-promo .hs-shortcut__thumb {
  color: var(--hs-blue);
}

@media (max-width: 1180px) {
  .hs-shortcuts {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hs-shortcuts {
    width: calc(100% - 24px);
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: -18px;
  }

  .hs-shortcut {
    min-height: 136px;
    padding: 14px 10px;
    gap: 10px;
  }

  .hs-shortcut__thumb {
    height: 84px;
  }

  .hs-shortcut.is-category .hs-shortcut__thumb {
    height: 92px;
  }

  .hs-shortcut.is-category .hs-shortcut__thumb img {
    object-fit: contain !important;
  }

  .hs-shortcut__label {
    min-height: auto;
    font-size: 13px;
  }
}

@media (max-width: 420px) {
  .hs-shortcuts {
    grid-template-columns: 1fr;
  }

  .hs-shortcut.is-category .hs-shortcut__thumb {
    height: 112px;
  }
}
</style>
