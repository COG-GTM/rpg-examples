import { HttpErrorResponse } from '@angular/common/http';

import { mapApiError } from './api-error';

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
    expect(mapApiError(new HttpErrorResponse({ status: 0 })).code).toBe('NETWORK');
  });
});
