import { HttpErrorResponse } from '@angular/common/http';

import { Problem } from './client.model';

export const API_UNREACHABLE = 'API unreachable';

/** Legacy CLIECRUD messages keyed by problem+json `code`. */
export const LEGACY_MESSAGES: Readonly<Record<string, string>> = {
  DUPDNI: 'DNI already exists',
  NOTFND: 'DNI not exists',
  DELETED: 'Client marked as deleted',
  INVDNI: 'DNI not valid',
};

const FIELD_BY_CODE: Readonly<Record<string, ApiErrorField>> = {
  DUPDNI: 'dni',
  INVDNI: 'dni',
  VALIDATION: 'name',
};

export type ApiErrorField = 'dni' | 'name';

export interface ApiError {
  /** Form control the error belongs to; absent for banner (non-field) errors. */
  field?: ApiErrorField;
  code?: string;
  status: number;
  message: string;
}

/** Parses the error body into a problem object, or `null` when there isn't one. */
export function toProblem(body: unknown): Problem | null {
  if (typeof body === 'string') {
    if (!body.trim()) {
      return null;
    }
    try {
      return toProblem(JSON.parse(body));
    } catch {
      return null;
    }
  }
  if (body && typeof body === 'object' && !Array.isArray(body)) {
    const problem = body as Problem;
    if (problem.code !== undefined || problem.detail !== undefined || problem.title !== undefined) {
      return problem;
    }
  }
  return null;
}

/**
 * Maps an HTTP failure to a user-facing error.
 *
 * Status 0 (network failure) and a body-less 5xx are both "API unreachable": the
 * Angular dev proxy answers HTTP 500 with an empty body when the target is down.
 * A real 5xx problem+json keeps its `detail`.
 */
export function mapApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: -1, message: error instanceof Error ? error.message : String(error) };
  }
  const problem = toProblem(error.error);
  if (error.status === 0 || (error.status >= 500 && !problem)) {
    return { status: error.status, message: API_UNREACHABLE };
  }
  const code = problem?.code;
  const message =
    (code && LEGACY_MESSAGES[code]) ||
    problem?.detail ||
    problem?.title ||
    error.statusText ||
    `HTTP ${error.status}`;
  const field = code ? FIELD_BY_CODE[code] : undefined;
  return { status: error.status, code, message, ...(field ? { field } : {}) };
}
