import { HttpErrorResponse } from '@angular/common/http';

import { API_UNREACHABLE, mapApiError, toProblem } from './api-error';

function httpError(status: number, error: unknown, statusText = 'Error'): HttpErrorResponse {
  return new HttpErrorResponse({ status, statusText, error, url: '/api/clients/12345678' });
}

function problem(status: number, code: string, detail: string) {
  return { type: 'about:blank', title: 'x', status, code, detail };
}

describe('mapApiError', () => {
  it('maps DUPDNI to the legacy message on the dni field', () => {
    expect(mapApiError(httpError(409, problem(409, 'DUPDNI', 'whatever')))).toEqual({
      status: 409,
      code: 'DUPDNI',
      message: 'DNI already exists',
      field: 'dni',
    });
  });

  it('maps INVDNI to "DNI not valid" on the dni field', () => {
    const result = mapApiError(httpError(400, problem(400, 'INVDNI', 'DNI not valid')));
    expect(result.message).toBe('DNI not valid');
    expect(result.field).toBe('dni');
  });

  it('maps NOTFND to "DNI not exists" as a banner error', () => {
    const result = mapApiError(httpError(404, problem(404, 'NOTFND', 'nope')));
    expect(result.message).toBe('DNI not exists');
    expect(result.field).toBeUndefined();
  });

  it('maps DELETED to "Client marked as deleted" as a banner error', () => {
    const result = mapApiError(httpError(410, problem(410, 'DELETED', 'gone')));
    expect(result.message).toBe('Client marked as deleted');
    expect(result.field).toBeUndefined();
  });

  it('puts VALIDATION on the name field using the detail', () => {
    const result = mapApiError(httpError(400, problem(400, 'VALIDATION', 'name: must not be blank')));
    expect(result).toEqual({ status: 400, code: 'VALIDATION', message: 'name: must not be blank', field: 'name' });
  });

  it('uses detail for other codes, including a real IOERR 5xx problem', () => {
    const result = mapApiError(httpError(500, problem(500, 'IOERR', 'TRCLIE I/O error')));
    expect(result).toEqual({ status: 500, code: 'IOERR', message: 'TRCLIE I/O error' });
  });

  it('parses a problem+json body delivered as a string', () => {
    const body = JSON.stringify(problem(409, 'DUPDNI', 'x'));
    expect(mapApiError(httpError(409, body)).message).toBe('DNI already exists');
  });

  it('treats status 0 as API unreachable', () => {
    const result = mapApiError(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }));
    expect(result.message).toBe(API_UNREACHABLE);
  });

  it('treats a body-less 500 (dev proxy, target down) as API unreachable', () => {
    expect(mapApiError(httpError(500, null)).message).toBe(API_UNREACHABLE);
    expect(mapApiError(httpError(500, '')).message).toBe(API_UNREACHABLE);
    expect(mapApiError(httpError(504, 'Gateway Timeout text')).message).toBe(API_UNREACHABLE);
  });

  it('falls back to the status text for a body-less 4xx', () => {
    expect(mapApiError(httpError(405, null, 'Method Not Allowed')).message).toBe('Method Not Allowed');
  });

  it('handles non-HTTP errors', () => {
    expect(mapApiError(new Error('boom')).message).toBe('boom');
  });
});

describe('toProblem', () => {
  it('returns null for empty or non-problem bodies', () => {
    expect(toProblem(null)).toBeNull();
    expect(toProblem('')).toBeNull();
    expect(toProblem('<html>')).toBeNull();
    expect(toProblem([])).toBeNull();
    expect(toProblem({ foo: 1 })).toBeNull();
  });
});
