import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { ClientDetailComponent } from './client-detail.component';

const deleted: Client = {
  dni: 31222333,
  name: 'MARIA LOPEZ',
  createdAt: '2026-10-01T09:15:00.000000',
  createdBy: 'QPGMR',
  updatedAt: null,
  updatedBy: null,
  deletedAt: '2026-10-05T10:00:00.000000',
  deletedBy: 'QPGMR',
  active: false,
};

const goneError = () =>
  throwError(
    () => new HttpErrorResponse({ status: 410, error: { code: 'DELETED', detail: 'Client marked as deleted' } }),
  );

describe('ClientDetailComponent', () => {
  let fixture: ComponentFixture<ClientDetailComponent>;
  let service: jasmine.SpyObj<ClientService>;
  const el = () => fixture.nativeElement as HTMLElement;

  const render = (dni: number) => {
    fixture = TestBed.createComponent(ClientDetailComponent);
    fixture.componentRef.setInput('dni', dni);
    fixture.detectChanges();
  };

  beforeEach(() => {
    service = jasmine.createSpyObj<ClientService>('ClientService', ['get']);
    TestBed.configureTestingModule({
      imports: [ClientDetailComponent],
      providers: [provideRouter([]), { provide: ClientService, useValue: service }],
    });
  });

  afterEach(() => history.replaceState(null, ''));

  it('renders an active client', () => {
    service.get.and.returnValue(of({ ...deleted, deletedAt: null, deletedBy: null, active: true }));
    render(31222333);
    expect(el().querySelector('h2')?.textContent).toContain('MARIA LOPEZ');
    expect(el().querySelector('[role=alert]')).toBeNull();
  });

  it('shows only the error for a deleted client opened directly', () => {
    service.get.and.returnValue(goneError());
    render(31222333);
    expect(el().querySelector('[role=alert]')?.textContent).toContain('Client marked as deleted');
    expect(el().querySelector('.card')).toBeNull();
  });

  it('shows the deleted record passed from the list, read-only', () => {
    history.replaceState({ client: deleted }, '');
    service.get.and.returnValue(goneError());
    render(31222333);
    expect(el().querySelector('[role=alert]')?.textContent).toContain('Client marked as deleted');
    expect(el().querySelector('h2')?.textContent).toContain('MARIA LOPEZ');
    expect(el().querySelector('.badge-deleted')).not.toBeNull();
    expect(el().textContent).not.toContain('Edit');
  });

  it('ignores list state for a different DNI', () => {
    history.replaceState({ client: deleted }, '');
    service.get.and.returnValue(goneError());
    render(27654321);
    expect(el().querySelector('.card')).toBeNull();
  });
});
