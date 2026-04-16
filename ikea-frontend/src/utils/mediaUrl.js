import { API_BASE_URL } from '../libs/apiClient';

const API_ORIGIN = String(API_BASE_URL ?? '').replace(/\/api\/?$/, '');

export function resolveMediaUrl(value) {
  const raw = String(value ?? '').trim();

  if (!raw) {
    return '';
  }

  if (/^(https?:)?\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
    return raw;
  }

  const normalizedPath = raw.replace(/\\/g, '/');

  if (normalizedPath.startsWith('/uploads/products/')) {
    return `${API_ORIGIN}${normalizedPath}`;
  }

  if (normalizedPath.startsWith('/uploads/')) {
    return `${API_ORIGIN}${normalizedPath.replace('/uploads/', '/uploads/products/')}`;
  }

  if (normalizedPath.startsWith('uploads/products/')) {
    return `${API_ORIGIN}/${normalizedPath}`;
  }

  if (normalizedPath.startsWith('uploads/')) {
    return `${API_ORIGIN}/${normalizedPath.replace('uploads/', 'uploads/products/')}`;
  }

  return normalizedPath;
}
