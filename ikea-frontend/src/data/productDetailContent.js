import { getProductDetailSeed } from './catalog';
import {
  buildProductOptionSummary,
  buildProductQuickFacts,
} from '../constants/productAttributeConfig';

const META_TEXT_PATTERNS = [
  /현재 사이트의 상세 구성에 맞춰/i,
  /현재 카탈로그 필터와 상세 요약에 맞춰/i,
  /IKEA 공식 상세 기준 최소 정보/i,
  /실제 리뷰 데이터 연결을 준비한 상태/i,
  /후속 리뷰 연동 시 실제 구매 리뷰로 교체/i,
  /대표 구매자 후기에서 먼저 보이는 반응만 최소한으로 정리/i,
  /리뷰 데이터 연결 준비/i,
  /대표 후기 준비 중/i,
  /기본 안내/i,
  /시스템 안내/i,
  /세부 치수 데이터는 후속 API/i,
  /API 연결을 위한 최소 상세 데이터 구성/i,
];

function sanitizeDetailText(value) {
  const text = String(value ?? '').trim();

  if (!text) {
    return '';
  }

  if (META_TEXT_PATTERNS.some((pattern) => pattern.test(text))) {
    return '';
  }

  return text;
}

function sanitizeTextList(values = []) {
  return values
    .map((value) => sanitizeDetailText(value))
    .filter(Boolean);
}

function sanitizeReviewHighlights(items = []) {
  return items.filter((item) => {
    const title = sanitizeDetailText(item?.title);
    const body = sanitizeDetailText(item?.body);
    const meta = sanitizeDetailText(item?.meta);

    return Boolean(title || body || meta);
  }).map((item) => ({
    ...item,
    title: sanitizeDetailText(item?.title),
    body: sanitizeDetailText(item?.body),
    meta: sanitizeDetailText(item?.meta),
  }));
}

function createMeasurementMap(measurements = []) {
  const aliasMap = {
    너비: 'width',
    길이: 'length',
    깊이: 'depth',
    높이: 'height',
    '등받이 높이': 'backHeight',
    등받이높이: 'backHeight',
    '시트 폭': 'seatWidth',
    '시트 깊이': 'seatDepth',
    '시트 높이': 'seatHeight',
    '팔걸이 높이': 'armHeight',
    '팔걸이 너비': 'armWidth',
    '가구 밑 자유공간': 'clearance',
    '가구 밑 여유공간': 'clearance',
    '책상 하중': 'shelfLoad',
    '최대 하중': 'maxLoad',
  };

  return measurements.reduce((result, item) => {
    const mappedKey = aliasMap[item.label];

    if (mappedKey) {
      result[mappedKey] = item.value;
    }

    return result;
  }, {});
}

function mergeQuickFacts(primaryFacts = [], secondaryFacts = []) {
  const merged = [];
  const seen = new Set();
  const aliasMap = {
    컬러: 'color',
    색상: 'color',
  };

  [...primaryFacts, ...secondaryFacts].forEach((fact) => {
    const label = fact?.label?.trim?.();
    const value = fact?.value?.trim?.();
    const normalizedLabel = aliasMap[label] ?? label;

    if (!label || !value || seen.has(normalizedLabel)) {
      return;
    }

    seen.add(normalizedLabel);
    merged.push({
      label,
      value,
    });
  });

  return merged;
}

function createFallbackDetail(product = {}) {
  const productLabel = product.label ?? product.categoryLabel ?? '대표 상품';
  const optionSummary = buildProductOptionSummary(product) || '기본 옵션';
  const galleryImages = Array.from(
    new Set([product.image, product.altImage].filter(Boolean)),
  );

  return {
    galleryImages,
    dimensionImage: null,
    useDimensionImage: false,
    heroHook: `${productLabel} 상품 정보입니다.`,
    description: [
      `${product.name} 상품 정보입니다.`,
      `${optionSummary} 구성을 확인해 보세요.`,
    ],
    highlights: [
      `${productLabel} 기준 대표 상품`,
      `현재 선택 옵션: ${optionSummary}`,
    ],
    quickFacts: buildProductQuickFacts(product),
    measurements: [],
    dimensions: {},
    dimensionCaption: '주요 치수를 확인해 보세요.',
    reviewIntro: '고객 리뷰를 확인해 보세요.',
    reviewHighlights: [],
  };
}

export function getProductDetailContent(product) {
  const fallback = createFallbackDetail(product);
  const override = product?.detailDraft ?? getProductDetailSeed(product?.id);

  if (!override) {
    return fallback;
  }

  const measurements = override.measurements ?? fallback.measurements;

  return {
    ...fallback,
    ...override,
    galleryImages: override.galleryImages ?? fallback.galleryImages,
    dimensionImage: override.dimensionImage ?? fallback.dimensionImage,
    useDimensionImage: override.useDimensionImage ?? false,
    heroHook: sanitizeDetailText(override.heroHook ?? fallback.heroHook) || fallback.heroHook,
    description: sanitizeTextList(override.description ?? fallback.description),
    highlights: sanitizeTextList(override.highlights ?? fallback.highlights),
    quickFacts: mergeQuickFacts(override.quickFacts ?? [], fallback.quickFacts),
    measurements,
    dimensions: createMeasurementMap(measurements),
    dimensionCaption: sanitizeDetailText(override.dimensionCaption ?? fallback.dimensionCaption)
      || '주요 치수를 확인해 보세요.',
    reviewIntro: sanitizeDetailText(override.reviewIntro ?? fallback.reviewIntro)
      || '고객 리뷰를 확인해 보세요.',
    reviewHighlights: sanitizeReviewHighlights(override.reviewHighlights ?? fallback.reviewHighlights),
  };
}
