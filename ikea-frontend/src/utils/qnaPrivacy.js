const EMAIL_PATTERN = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i;
const PHONE_PATTERN = /(?:01[016789]|02|0[3-9]\d)[-\s]?\d{3,4}[-\s]?\d{4}/;

export function detectSensitiveQnaContent(values = []) {
  const text = values
    .map((value) => String(value ?? '').trim())
    .filter(Boolean)
    .join('\n');

  if (!text) {
    return '';
  }

  if (EMAIL_PATTERN.test(text)) {
    return '이메일 주소는 공개 QnA에 작성할 수 없습니다.';
  }

  if (PHONE_PATTERN.test(text)) {
    return '전화번호는 공개 QnA에 작성할 수 없습니다.';
  }

  return '';
}
///이거 이제 필요없지 않나??
