import httpRequester from '../libs/httpRequester';

export function getAdminProductCount() {
  return httpRequester.get('/admin/product/count');
}

export function getAdminOrderCount() {
  return httpRequester.get('/admin/order/count');
}

export function getAdminOrders(query) {
  return httpRequester.get('/admin/order', { params: query });
}

export function getAdminPayments() {
  return httpRequester.get('/admin/payment');
}

export function getAdminOrdersByStatus(status) {
  return httpRequester.get('/admin/order/status', { params: { status } });
}

export function getAdminMembers(query) {
  if (query?.keyword) {
    return httpRequester.get('/admin/member/search', { params: query });
  }

  return httpRequester.get('/admin/member');
}

export function getAdminMemberDetail(memberId) {
  return httpRequester.get(`/admin/member/${memberId}`);
}

export function updateAdminMemberRole(memberId, memberRole) {
  return httpRequester.patch(`/admin/member/${memberId}/role`, null, {
    params: { memberRole },
  });
}

export function deleteAdminMember(memberId) {
  return httpRequester.delete(`/admin/member/${memberId}`);
}

export function getAdminReviews() {
  return httpRequester.get('/admin/review');
}

export function getAdminQnas() {
  return httpRequester.get('/admin/qna');
}

function buildJsonPartFormData(dto, files = [], fileFieldName) {
  const formData = new FormData();
  formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));

  files.forEach((file) => {
    formData.append(fileFieldName, file);
  });

  return formData;
}

function buildAdminProductFormData(dto, payload = {}) {
  const formData = new FormData();
  formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));

  (payload.mainFiles ?? []).forEach((file) => {
    formData.append('imgFile', file);
  });

  (payload.galleryFiles ?? []).forEach((file) => {
    formData.append('galleryFiles', file);
  });

  const dimensionFile = payload.dimensionFiles?.[0];
  if (dimensionFile) {
    formData.append('dimensionFile', dimensionFile);
  }

  return formData;
}

function buildAdminQnaAnswerPayload(payload = {}) {
  return {
    title: String(payload.title ?? '').trim(),
    content: String(payload.content ?? '').trim(),
  };
}

export function getProductCatalog(query) {
  if (query?.keyword) {
    return httpRequester.get('/product/search', {
      params: { keyword: query.keyword },
    });
  }

  return httpRequester.get('/product');
}

export function createAdminProduct(payload) {
  const dto = {
    name: payload.name,
    price: Number(payload.price),
    categoryId: Number(payload.categoryId),
    originalPrice: payload.originalPrice,
    brand: payload.brand,
    badge: payload.badge,
    label: payload.label,
    typeSlug: payload.typeSlug,
    attributes: payload.attributes,
    detailContent: payload.detailContent,
  };

  return httpRequester.post(
    '/admin/product',
    buildAdminProductFormData(dto, payload),
  );
}

export function updateAdminProduct(productId, payload) {
  const dto = {
    name: payload.name,
    price: Number(payload.price),
    categoryId: Number(payload.categoryId),
    originalPrice: payload.originalPrice,
    brand: payload.brand,
    badge: payload.badge,
    label: payload.label,
    typeSlug: payload.typeSlug,
    attributes: payload.attributes,
    detailContent: payload.detailContent,
  };

  return httpRequester.put(
    `/admin/product/${productId}`,
    buildAdminProductFormData(dto, payload),
  );
}

export function deleteAdminProduct(productId) {
  return httpRequester.delete(`/admin/product/${productId}`);
}

export function updateAdminOrderStatus(orderId, status) {
  return httpRequester.patch(`/admin/order/${orderId}/status`, null, {
    params: { status },
  });
}

export function removeAdminReview(reviewId) {
  return httpRequester.delete(`/admin/review/${reviewId}`);
}

export function createAdminQnaAnswer(parentId, payload) {
  return httpRequester.post(
    `/admin/qna/${parentId}/answer`,
    buildAdminQnaAnswerPayload(payload),
  );
}

export function updateAdminQnaAnswer(qnaId, payload) {
  return httpRequester.put(
    `/admin/qna/${qnaId}/answer`,
    buildAdminQnaAnswerPayload(payload),
  );
}

export function deleteAdminQnaAnswer(qnaId) {
  return httpRequester.delete(`/admin/qna/${qnaId}/answer`);
}

export function getAdminNoticeList() {
  return httpRequester.get('/notice');
}

export function getAdminNoticeDetail(noticeId) {
  return httpRequester.get(`/notice/${noticeId}`);
}

export function createAdminNotice(payload) {
  const dto = {
    title: payload.title,
    content: payload.content,
    writer: payload.writer,
  };

  return httpRequester.post(
    '/admin/notice',
    buildJsonPartFormData(dto, payload.files ?? [], 'files'),
  );
}

export function updateAdminNotice(noticeId, payload) {
  const dto = {
    title: payload.title,
    content: payload.content,
    writer: payload.writer,
  };

  return httpRequester.patch(
    `/admin/notice/${noticeId}`,
    buildJsonPartFormData(dto, payload.files ?? [], 'files'),
  );
}

export function deleteAdminNotice(noticeId) {
  return httpRequester.delete(`/admin/notice/${noticeId}`);
}

