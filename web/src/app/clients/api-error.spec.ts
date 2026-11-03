import { HttpErrorResponse } from '@angular/common/http';

import { API_ERROR_MESSAGES, mapApiError, NETWORK_ERROR_MESSAGE, PROXY_UNREACHABLE_CODE } from './api-error';

function problem(status: number, code: string, detail = 'server detail'): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    error: { type: 'about:blank', title: 'Error', status, detail, code },
  });
}

describe('mapApiError', () => {
  it('maps contract codes to friendly messages', () => {
    expect(mapApiError(problem(409, 'DUPDNI'))).toEqual(
      jasmine.objectContaining({ code: 'DUPDNI', message: 'DNI already exists', field: 'dni' }),
    );
    expect(mapApiError(problem(404, 'NOTFND')).message).toBe('DNI not exists');
    expect(mapApiError(problem(410, 'DELETED')).message).toBe('Client marked as deleted');
    expect(mapApiError(problem(400, 'INVDNI')).message).toBe('DNI not valid');
  });

  it('falls back to the problem detail for unmapped codes', () => {
    const info = mapApiError(problem(400, 'VALIDATION', 'name must not be blank'));
    expect(info.message).toBe('name must not be blank');
    expect(info.field).toBe('name');
  });

  it('parses problem+json delivered as a string body', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: JSON.stringify({ status: 409, detail: 'x', code: 'DUPDNI' }),
    });
    expect(mapApiError(err).message).toBe('DNI already exists');
  });

  it('handles network failures', () => {
    const info = mapApiError(new HttpErrorResponse({ status: 0 }));
    expect(info.code).toBe('NETWORK');
    expect(info.message).toBe(NETWORK_ERROR_MESSAGE);
  });

  it('treats a proxy-generated 5xx as the API being unreachable', () => {
    expect(mapApiError(new HttpErrorResponse({ status: 500, error: '' })).message).toBe(NETWORK_ERROR_MESSAGE);
    expect(mapApiError(new HttpErrorResponse({ status: 504, error: 'Error occurred while trying to proxy' })).code).toBe('NETWORK');
    expect(mapApiError(new HttpErrorResponse({ status: 502, error: { code: PROXY_UNREACHABLE_CODE } })).code).toBe('NETWORK');
  });

  it('keeps the server detail for a code-less 5xx problem from the API', () => {
    const info = mapApiError(
      new HttpErrorResponse({ status: 500, error: { title: 'Internal Server Error', detail: 'Database temporarily unavailable' } }),
    );
    expect(info.code).toBe('UNKNOWN');
    expect(info.message).toBe('Database temporarily unavailable');
  });

  it('keeps the IOERR message for a real 500 problem from the API', () => {
    const info = mapApiError(new HttpErrorResponse({ status: 500, error: { code: 'IOERR', detail: 'file error' } }));
    expect(info.code).toBe('IOERR');
    expect(info.message).toBe(API_ERROR_MESSAGES['IOERR']);
  });
});
