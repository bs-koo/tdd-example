export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, status: number, message?: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export async function throwApiError(response: Response): Promise<never> {
  let body: { code: string; message?: string };
  try {
    body = (await response.json()) as { code: string; message?: string };
  } catch {
    throw new ApiError('UNKNOWN', response.status);
  }
  throw new ApiError(body.code, response.status, body.message);
}
