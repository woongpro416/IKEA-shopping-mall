import {
  backendCategories as catalogCategories,
  catalogProducts,
} from '../catalog';

export const categoryMeta = {
  sofa: {
    label: '소파',
    heroEyebrow: 'Living room edit',
    heroTitle: '많이 찾는 소파를 편하게 둘러보세요.',
    heroDescription:
      '2인용부터 모듈형까지, 거실 분위기를 바꾸기 좋은 대표 소파만 간결하게 모았습니다.',
    heroImage: 'https://www.ikea.com/ext/ingkadam/m/315ad03372ced6ee/original/PH200496.jpg?f=sg',
    bannerTitle: '소파 인기 셀렉션',
    bannerSubtitle: '가장 많이 보는 소파 구성을 먼저 비교해 보세요.',
    bannerImage: '/content-images/modular-sofa.jpg',
  },
  'bed-mattress': {
    label: '침대/매트리스',
    heroEyebrow: 'Sleep essentials',
    heroTitle: '침실 기본 구성을 차분하게 살펴보세요.',
    heroDescription:
      '침대, 매트리스, 협탁까지 이어지는 수면 공간의 핵심 아이템만 한 번에 비교할 수 있게 정리했습니다.',
    heroImage: 'https://www.ikea.com/images/idanaes-326210c2a4fb53795c76e65b072f07d9.jpg?f=sg',
    bannerTitle: '침실 기본 구성',
    bannerSubtitle: '침대와 매트리스, 협탁까지 한 번에 비교해 보세요.',
    bannerImage: '/theme-images/sleep-wellness.jpg',
  },
  dining: {
    label: '식탁/테이블/의자',
    heroEyebrow: 'Dining setup',
    heroTitle: '식탁과 의자를 함께 보고 다이닝을 완성해보세요.',
    heroDescription:
      '테이블과 의자를 중심으로 식사 공간에 잘 맞는 조합을 빠르게 비교할 수 있도록 구성했습니다.',
    heroImage: 'https://www.ikea.com/images/roenninge-roenninge-tarbaek-8f33d2b520571cac4d1e4b37f0b5ee88.jpg?f=sg',
    bannerTitle: '다이닝 인기 상품',
    bannerSubtitle: '식사 공간을 완성하는 대표 상품을 먼저 확인해 보세요.',
    bannerImage: '/content-images/showroom-furniture.jpg',
  },
  desk: {
    label: '책상',
    heroEyebrow: 'Workstation guide',
    heroTitle: '작업과 집중이 잘되는 책상 구성을 가볍게 둘러보세요.',
    heroDescription:
      '컴퓨터 책상, 회의용 테이블, 게임용 책상, 오피스 책상을 한 화면에서 비교할 수 있도록 구성했습니다.',
    heroImage: '/content-images/office-desk.jpg',
    bannerTitle: '책상 베스트 조합',
    bannerSubtitle: '집중이 필요한 작업 공간을 먼저 살펴보세요.',
    bannerImage: '/theme-images/worklife-studio.jpg',
  },
  'kitchen-furniture': {
    label: '주방가구',
    heroEyebrow: 'Kitchen setup',
    heroTitle: '주방 동선을 간결하게 만드는 기본 가구를 모았습니다.',
    heroDescription:
      '주방가전, 정리용품, 선반과 카트를 현재 카테고리 흐름에 맞춰 빠르게 비교할 수 있습니다.',
    heroImage: '/content-images/kitchen-remodel.jpg',
    bannerTitle: '주방가구 추천',
    bannerSubtitle: '정리와 수납을 함께 보는 주방 구성을 확인해 보세요.',
    bannerImage: '/content-images/kitchen-remodel.jpg',
  },
  kitchenware: {
    label: '주방용품',
    heroEyebrow: 'Kitchenware picks',
    heroTitle: '매일 자주 쓰는 주방용품을 한 번에 확인해 보세요.',
    heroDescription:
      '조리도구, 냄비와 팬, 도마와 칼, 유리컵, 커트러리를 현재 카탈로그 기준으로 정리했습니다.',
    heroImage: '/theme-images/crafted-wood-premium.jpg',
    bannerTitle: '주방용품 셀렉션',
    bannerSubtitle: '자주 쓰는 기본 아이템부터 먼저 비교해 보세요.',
    bannerImage: 'https://www.ikea.com/ext/ingkadam/m/21acd2023e85b901/original/PH207436.jpg',
    bannerImagePosition: 'center center',
  },
  plant: {
    label: '화분/식물',
    heroEyebrow: 'Green styling',
    heroTitle: '작은 초록 포인트로 공간 분위기를 바꿔보세요.',
    heroDescription:
      '식물과 조화, 화분, 스탠드, 원예용품, 물뿌리개까지 이어지는 흐름으로 정리했습니다.',
    heroImage: '/theme-images/editorial-earth.jpg',
    bannerTitle: '화분/식물 추천',
    bannerSubtitle: '실내 분위기를 바꾸는 가벼운 포인트부터 살펴보세요.',
    bannerImage: 'https://www.ikea.com/kr/ko/images/products/sojaboena-plant-pot-white__1156984_pe887411_s5.jpg',
    bannerImagePosition: '58% center',
  },
};

export const categorySlugOrder = [
  'sofa',
  'bed-mattress',
  'dining',
  'desk',
  'kitchen-furniture',
  'kitchenware',
  'plant',
];

export const categoryMap = new Map(catalogCategories.map((category) => [category.slug, category]));

const productsByCategory = new Map(
  catalogCategories.map((category) => [
    category.slug,
    catalogProducts.filter((product) => product.categorySlug === category.slug),
  ]),
);

function formatPrice(value) {
  return `${Number(value ?? 0).toLocaleString('ko-KR')}원`;
}

function formatReviewMeta(product) {
  if (!product) {
    return '';
  }

  if (product.rating) {
    return `평점 ${Number(product.rating).toFixed(1)} · 리뷰 ${Number(
      product.reviews ?? 0,
    ).toLocaleString('ko-KR')}개`;
  }

  return `리뷰 ${Number(product.reviews ?? 0).toLocaleString('ko-KR')}개`;
}

function sortProducts(products = []) {
  return [...products].sort((left, right) => {
    const reviewGap = Number(right.reviews ?? 0) - Number(left.reviews ?? 0);
    if (reviewGap !== 0) {
      return reviewGap;
    }

    const ratingGap = Number(right.rating ?? 0) - Number(left.rating ?? 0);
    if (ratingGap !== 0) {
      return ratingGap;
    }

    return Number(right.price ?? 0) - Number(left.price ?? 0);
  });
}

function getCategoryProducts(categorySlug) {
  return productsByCategory.get(categorySlug) ?? [];
}

export function getTopProducts(categorySlug, limit = 4) {
  return sortProducts(getCategoryProducts(categorySlug)).slice(0, limit);
}

export function getNaturalProducts(categorySlug, limit = 4) {
  return getCategoryProducts(categorySlug).slice(0, limit);
}

export function getCategoryLabel(categorySlug) {
  return categoryMeta[categorySlug]?.label ?? categoryMap.get(categorySlug)?.label ?? '';
}

export function findPrimaryProduct(categorySlug, position = 0) {
  return getTopProducts(categorySlug, position + 1)[position] ?? getCategoryProducts(categorySlug)[0] ?? null;
}

export function toProductCard(product, overrides = {}) {
  if (!product) {
    return null;
  }

  return {
    id: overrides.id ?? `product-${product.id}`,
    productId: String(product.id),
    categorySlug: product.categorySlug,
    typeSlug: product.typeSlug,
    brand: product.brand,
    title: product.name,
    image: overrides.image ?? product.image,
    badge: overrides.badge ?? product.label ?? getCategoryLabel(product.categorySlug),
    price: formatPrice(product.price),
    originalPrice: product.originalPrice ? formatPrice(product.originalPrice) : '',
    discount:
      overrides.discount ??
      (Number(product.discountRate ?? 0) > 0 ? `${product.discountRate}%` : ''),
    metaText: overrides.metaText ?? formatReviewMeta(product),
    tags: overrides.tags ?? (product.features ?? []).slice(0, 2),
  };
}

export function toPickCard(product, overrides = {}) {
  if (!product) {
    return null;
  }

  return {
    id: overrides.id ?? `pick-${product.id}`,
    productId: String(product.id),
    categorySlug: product.categorySlug,
    typeSlug: product.typeSlug,
    brand: product.brand,
    title: product.name,
    image: overrides.image ?? product.altImage ?? product.image,
    badge: overrides.badge ?? getCategoryLabel(product.categorySlug),
    accent: overrides.accent ?? 'blue',
    price: formatPrice(product.price),
    discount:
      overrides.discount ??
      (Number(product.discountRate ?? 0) > 0 ? `${product.discountRate}%` : ''),
    rating: product.rating ? Number(product.rating).toFixed(1) : null,
    tags: overrides.tags ?? (product.features ?? []).slice(0, 2),
  };
}
