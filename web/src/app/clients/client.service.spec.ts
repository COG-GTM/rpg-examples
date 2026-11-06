import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Client } from './client.model';
import { CLIENTS_API, ClientService } from './client.service';

const CLIENT: Client = {
  dni: 12345678,
  name: 'JUAN PEREZ',
  createdAt: '2026-10-01T09:15:00.000000',
  createdBy: 'QPGMR',
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
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses /api/clients as the base URL', () => {
    expect(CLIENTS_API).toBe('/api/clients');
  });

  it('list() GETs /api/clients', () => {
    let result: Client[] | undefined;
    service.list().subscribe((clients) => (result = clients));
    const req = http.expectOne('/api/clients');
    expect(req.request.method).toBe('GET');
    req.flush([CLIENT]);
    expect(result).toEqual([CLIENT]);
  });

  it('get() GETs /api/clients/{dni}', () => {
    let result: Client | undefined;
    service.get(12345678).subscribe((client) => (result = client));
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('GET');
    req.flush(CLIENT);
    expect(result).toEqual(CLIENT);
  });

  it('create() POSTs {name} to /api/clients/{dni}', () => {
    let result: Client | undefined;
    service.create(12345678, 'JUAN PEREZ').subscribe((client) => (result = client));
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'JUAN PEREZ' });
    req.flush(CLIENT, { status: 201, statusText: 'Created', headers: { Location: '/clients/12345678' } });
    expect(result).toEqual(CLIENT);
  });

  it('update() PUTs {name} to /api/clients/{dni}', () => {
    service.update(12345678, 'JUAN P. PEREZ').subscribe();
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'JUAN P. PEREZ' });
    req.flush({ ...CLIENT, name: 'JUAN P. PEREZ' });
  });

  it('delete() DELETEs /api/clients/{dni} and completes', () => {
    let completed = false;
    service.delete(12345678).subscribe({ complete: () => (completed = true) });
    const req = http.expectOne('/api/clients/12345678');
    expect(req.request.method).toBe('DELETE');
    req.flush({ dni: 12345678, deletedBy: 'WEBAPP', active: false });
    expect(completed).toBeTrue();
  });

  it('list(true) asks for soft-deleted clients too', () => {
    service.list(true).subscribe();
    const req = http.expectOne((r) => r.url === '/api/clients' && r.params.get('includeDeleted') === 'true');
    req.flush([]);
  });

  it('surfaces problem+json errors to the caller', () => {
    let status = 0;
    service.get(12345678).subscribe({ error: (err) => (status = err.status) });
    http
      .expectOne('/api/clients/12345678')
      .flush({ status: 410, code: 'DELETED', detail: 'Client marked as deleted' }, { status: 410, statusText: 'Gone' });
    expect(status).toBe(410);
  });
});
