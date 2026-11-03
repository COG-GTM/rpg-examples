import { HttpErrorResponse } from '@angular/common/http';

import { ApiProblem } from './client.model';

export const API_ERROR_MESSAGES: Readonly<Record<string, string>> = {
  DUPDNI: 'DNI already exists',
  NOTFND: 'DNI not exists',
  DELETED: 'Client marked as deleted',
  INVDNI: 'DNI not valid',
  IOERR: 'The client file could not be accessed. Please try again later.',
};

export const NETWORK_ERROR_MESSAGE = 'Cannot reach the clients API. Is it running?';

export interface ApiErrorInfo {
  code: string;
  status: number;
  message: string;
  /** Form control the error relates to, so it can be shown inline next to the field. */
  field: 'dni' | 'name' | null;
}

const FIELD_BY_CODE: Readonly<Record<string, 'dni' | 'name'>> = {
  DUPDNI: 'dni',
  INVDNI: 'dni',
  NOTFND: 'dni',
  VALIDATION: 'name',
};

function asProblem(body: unknown): ApiProblem | null {
  if (body && typeof body === 'object') return body as ApiProblem;
  if (typeof body === 'string') {
    try {
      const parsed = JSON.parse(body);
      return parsed && typeof parsed === 'object' ? (parsed as ApiProblem) : null;
    } catch {
      return null;
    }
  }
  return null;
}

/** Maps an HTTP error carrying an RFC 7807 problem+json body to a user-friendly message by `code`. */
export function mapApiError(error: unknown): ApiErrorInfo {
  if (!(error instanceof HttpErrorResponse)) {
    return { code: 'UNKNOWN', status: 0, message: 'Unexpected error', field: null };
  }
  const problem = asProblem(error.error);
  const code = problem?.code?.trim() || 'UNKNOWN';
  // Status 0 = browser could not connect; 5xx without a problem `code` = dev proxy could not reach the API.
  if (error.status === 0 || (error.status >= 500 && code === 'UNKNOWN')) {
    return { code: 'NETWORK', status: error.status, message: NETWORK_ERROR_MESSAGE, field: null };
  }
  const message =
    API_ERROR_MESSAGES[code] ??
    problem?.detail ??
    problem?.title ??
    `Request failed (HTTP ${error.status})`;
  return { code, status: error.status, message, field: FIELD_BY_CODE[code] ?? null };
}
