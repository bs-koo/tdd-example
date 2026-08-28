import { describe, it, expect } from 'vitest';
import { ApiError } from './client';

describe('ApiError', () => {
  it('code와 status를 생성자 인자로 그대로 보관한다', () => {
    const error = new ApiError('OVERLAPPING_RESERVATION', 409);

    expect(error.code).toBe('OVERLAPPING_RESERVATION');
    expect(error.status).toBe(409);
  });

  it('Error의 인스턴스다 — 호출부가 catch(e)에서 표준 처리를 할 수 있어야 한다', () => {
    const error = new ApiError('ROOM_NOT_FOUND', 404);

    expect(error).toBeInstanceOf(Error);
  });

  it('message를 넘기면 error.message에 담긴다', () => {
    const error = new ApiError('ROOM_NOT_FOUND', 404, '회의실을 찾을 수 없습니다.');

    expect(error.message).toBe('회의실을 찾을 수 없습니다.');
  });

  it('message를 생략해도 인스턴스가 생성되고 code·status는 유지된다', () => {
    const error = new ApiError('UNKNOWN', 500);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('UNKNOWN');
    expect(error.status).toBe(500);
  });
});
