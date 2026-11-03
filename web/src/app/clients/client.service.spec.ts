import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE, ClientService } from './client.service';

describe('ClientService', () => {
  let service: ClientService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('POSTs /clients/{dni} with the name', () => {
    service.create(12345678, 'Ana').subscribe();
    const req = http.expectOne(`${API_BASE}/12345678`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Ana' });
    req.flush({});
  });

  it('surfaces the CLIESRV message from the problem detail', (done) => {
    service.get(12345678).subscribe({
      error: (e: Error) => {
        expect(e.message).toBe('Client marked as deleted');
        done();
      },
    });
    http.expectOne(`${API_BASE}/12345678`).flush(
      { title: 'DELETED', code: 'DELETED', detail: 'Client marked as deleted' },
      { status: 410, statusText: 'Gone' },
    );
  });
});
