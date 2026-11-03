import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Client } from './client.model';
import { ClientService } from './client.service';

const client: Client = {
  dni: 12345678,
  name: 'ANA PEREZ',
  createdAt: '2026-11-03T10:15:30.123456',
  createdBy: 'QUSER',
  updatedAt: null,
  updatedBy: null,
  deletedAt: null,
  deletedBy: null,
  active: true,
};

describe('ClientService', () => {
  let service: ClientService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() GETs /api/clients', () => {
    let result: Client[] | undefined;
    service.list().subscribe((r) => (result = r));
    const req = http.expectOne('/api/clients');
    expect(req.request.method).toBe('GET');
    req.flush([client]);
    expect(result).toEqual([client]);
  });

  it('get() GETs /api/clients/{dni}', () => {
    let result: Client | undefined;
    service.get(12345678).subscribe((r) => (result = r));
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('GET');
    req.flush(client);
    expect(result).toEqual(client);
  });

  it('create() POSTs the name to /api/clients/{dni}', () => {
    let result: Client | undefined;
    service.create(12345678, { name: 'ANA PEREZ' }).subscribe((r) => (result = r));
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'ANA PEREZ' });
    req.flush(client, { status: 201, statusText: 'Created', headers: { Location: '/clients/12345678' } });
    expect(result).toEqual(client);
  });

  it('update() PUTs the name to /api/clients/{dni}', () => {
    service.update(12345678, { name: 'ANA P' }).subscribe();
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'ANA P' });
    req.flush({ ...client, name: 'ANA P' });
  });

  it('delete() DELETEs /api/clients/{dni}', () => {
    let done = false;
    service.delete(12345678).subscribe({ complete: () => (done = true) });
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(done).toBeTrue();
  });

  it('surfaces problem+json errors to the caller', () => {
    let status = 0;
    let code: string | undefined;
    service.create(12345678, { name: 'X' }).subscribe({
      error: (err) => {
        status = err.status;
        code = err.error.code;
      },
    });
    http
      .expectOne('/api/clients/12345678')
      .flush(
        { title: 'Conflict', status: 409, detail: 'DNI already exists', code: 'DUPDNI' },
        { status: 409, statusText: 'Conflict', headers: { 'Content-Type': 'application/problem+json' } },
      );
    expect(status).toBe(409);
    expect(code).toBe('DUPDNI');
  });
});
