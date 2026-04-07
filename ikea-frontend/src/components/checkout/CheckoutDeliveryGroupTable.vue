<script setup>
defineProps({
  group: {
    type: Object,
    required: true,
  },
  formatPrice: {
    type: Function,
    required: true,
  },
  openShippingGuide: {
    type: Function,
    required: true,
  },
});
</script>

<template>
  <table class="checkout-group-table">
    <colgroup>
      <col />
      <col style="width: 128px" />
      <col style="width: 168px" />
      <col style="width: 188px" />
    </colgroup>
    <tbody>
      <tr v-for="entry in group.items" :key="entry.key" class="checkout-item-row">
        <td class="checkout-item__info-cell">
          <div class="checkout-item__info">
            <RouterLink :to="entry.item.detailPath" class="checkout-item__thumb">
              <img :src="entry.item.image" :alt="entry.item.name" />
            </RouterLink>
            <div class="checkout-item__copy">
              <div class="checkout-item__meta">
                <strong>{{ entry.item.brand }}</strong>
                <span>{{ entry.item.seller }}</span>
              </div>
              <h2>
                <RouterLink :to="entry.item.detailPath">{{ entry.item.name }}</RouterLink>
              </h2>
              <p>{{ entry.item.option }}</p>
            </div>
          </div>
        </td>

        <td class="checkout-item__qty-cell">
          <div class="checkout-item__qty">{{ entry.item.quantity }}</div>
        </td>

        <td class="checkout-item__price-cell">
          <div class="checkout-item__price">
            <strong>{{ formatPrice(entry.item.price * entry.item.quantity) }}</strong>
            <span v-if="(entry.item.originalPrice ?? entry.item.price) > entry.item.price">
              {{ formatPrice((entry.item.originalPrice ?? entry.item.price) * entry.item.quantity) }}
            </span>
          </div>
        </td>

        <td
          v-if="!entry.skipShippingCell"
          class="checkout-item__shipping-cell"
          :class="{ 'is-merged': entry.showMergedShippingInfo }"
          :rowspan="entry.showMergedShippingInfo ? entry.mergedShippingRowSpan : 1"
        >
          <div v-if="entry.showShippingInfo || entry.showMergedShippingInfo" class="checkout-item__shipping">
            <button
              class="checkout-shipping-trigger"
              type="button"
              @click="openShippingGuide(entry.deliveryGuide.modalTitle, entry.deliveryGuide.modalBody)"
            >
              {{ entry.deliveryGuide.shippingText }}
            </button>
            <p>{{ entry.deliveryGuide.shippingSubText }}</p>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.checkout-group-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.checkout-group-table td {
  padding: 22px 0;
  border-bottom: 1px solid #eceff3;
  vertical-align: middle;
  background: #ffffff;
}

.checkout-item__qty-cell,
.checkout-item__price-cell,
.checkout-item__shipping-cell {
  text-align: center;
}

.checkout-item__info {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.checkout-item__thumb {
  display: block;
}

.checkout-item__thumb img {
  width: 94px;
  height: 94px;
  object-fit: contain;
  border: 1px solid #f0f0f0;
  background: #ffffff;
}

.checkout-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666666;
  font-size: 13px;
}

.checkout-item__copy {
  min-width: 0;
}

.checkout-item__copy h2 {
  margin: 8px 0 6px;
  font-size: 18px;
  line-height: 1.45;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.checkout-item__copy a {
  color: inherit;
}

.checkout-item__copy p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.checkout-item__qty,
.checkout-item__price {
  display: grid;
  justify-items: center;
  gap: 8px;
  text-align: center;
}

.checkout-item__qty {
  font-size: 16px;
  font-weight: 600;
}

.checkout-item__price strong {
  color: #111111;
  font-weight: 800;
  letter-spacing: -0.03em;
  font-size: 24px;
}

.checkout-item__price span {
  color: #9ca3af;
  font-size: 13px;
  text-decoration: line-through;
}

.checkout-item__shipping-cell {
  padding: 0;
  vertical-align: middle;
}

.checkout-item__shipping-cell.is-merged {
  vertical-align: middle;
}

.checkout-item__shipping {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 100%;
  text-align: center;
}

.checkout-shipping-trigger {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  color: #0058a3;
  font-size: 16px;
  font-weight: 700;
}

.checkout-item__shipping p {
  margin: 0;
  white-space: pre-line;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}
</style>
