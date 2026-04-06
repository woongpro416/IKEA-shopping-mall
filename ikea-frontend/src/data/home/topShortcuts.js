import bedMattressShortcutImage from '../../assets/home/shortcuts/bed-mattress.jpg';
import diningShortcutImage from '../../assets/home/shortcuts/dining.jpg';
import kitchenFurnitureShortcutImage from '../../assets/home/shortcuts/kitchen-furniture.jpg';
import plantShortcutImage from '../../assets/home/shortcuts/plant.jpg';
import sofaShortcutImage from '../../assets/home/shortcuts/sofa.jpg';

const deskShortcutImage = 'https://www.ikea.com/kr/ko/images/products/lagkapten-adils-desk-white__0977229_pe813472_s5.jpg';

const topShortcutImageMap = {
  sofa: sofaShortcutImage,
  'bed-mattress': bedMattressShortcutImage,
  dining: diningShortcutImage,
  desk: deskShortcutImage,
  'kitchen-furniture': kitchenFurnitureShortcutImage,
  plant: plantShortcutImage,
};

export const topShortcutBoxes = [
  { id: 'shortcut-best', type: 'promo', icon: 'ticket', label: '이번 주 추천', anchorId: 'weekly-picks' },
  {
    id: 'shortcut-category',
    type: 'promo',
    icon: 'grid',
    shortLabel: '카테고리 보기',
    label: '카테고리 둘러보기',
    anchorId: 'category-focus',
  },
  {
    id: 'shortcut-sofa',
    type: 'category',
    image: topShortcutImageMap.sofa,
    label: '소파',
    categorySlug: 'sofa',
  },
  {
    id: 'shortcut-bed',
    type: 'category',
    image: topShortcutImageMap['bed-mattress'],
    label: '침대/매트리스',
    categorySlug: 'bed-mattress',
  },
  {
    id: 'shortcut-dining',
    type: 'category',
    image: topShortcutImageMap.dining,
    imageFit: 'contain',
    imagePosition: 'center center',
    shortLabel: '식탁/의자',
    label: '식탁/테이블/의자',
    categorySlug: 'dining',
  },
  {
    id: 'shortcut-desk',
    type: 'category',
    image: topShortcutImageMap.desk,
    imageFit: 'contain',
    imagePosition: 'center center',
    label: '책상',
    categorySlug: 'desk',
  },
  {
    id: 'shortcut-kitchen-furniture',
    type: 'category',
    image: topShortcutImageMap['kitchen-furniture'],
    imageFit: 'contain',
    imagePosition: 'center center',
    label: '주방가구',
    categorySlug: 'kitchen-furniture',
  },
  {
    id: 'shortcut-kitchenware',
    type: 'category',
    image: 'https://www.ikea.com/kr/ko/images/products/ikea-365-pot-with-lid-stainless-steel__1006171_pe825756_s5.jpg',
    imageFit: 'contain',
    imagePosition: 'center center',
    imageScale: 1.18,
    label: '주방용품',
    categorySlug: 'kitchenware',
  },
  {
    id: 'shortcut-plant',
    type: 'category',
    image: topShortcutImageMap.plant,
    imageFit: 'contain',
    imagePosition: 'center center',
    label: '화분/식물',
    categorySlug: 'plant',
  },
];
