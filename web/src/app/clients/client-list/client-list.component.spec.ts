import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { ClientListComponent } from './client-list.component';

const make = (dni: number, name: string): Client => ({
  dni,
  name,
  createdAt: '2026-10-01T09:15:00.000000',
  createdBy: 'QPGMR',
  updatedAt: null,
  updatedBy: null,
  deletedAt: null,
  deletedBy: null,
  active: true,
});

describe('ClientListComponent', () => {
  let fixture: ComponentFixture<ClientListComponent>;
  let service: jasmine.SpyObj<ClientService>;
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    service = jasmine.createSpyObj<ClientService>('ClientService', ['list', 'delete']);
    TestBed.configureTestingModule({
      imports: [ClientListComponent],
      providers: [provideRouter([]), { provide: ClientService, useValue: service }],
    });
  });

  it('renders the active clients', () => {
    service.list.and.returnValue(of([make(12345678, 'JUAN PEREZ'), make(23456789, 'MARIA GOMEZ')]));
    fixture = TestBed.createComponent(ClientListComponent);
    fixture.detectChanges();
    expect(el().querySelectorAll('tbody tr').length).toBe(2);
  });

  it('deletes only after confirmation, then reloads the list', () => {
    service.list.and.returnValues(of([make(12345678, 'JUAN PEREZ')]), of([]));
    service.delete.and.returnValue(of(undefined));
    fixture = TestBed.createComponent(ClientListComponent);
    fixture.detectChanges();

    el().querySelector<HTMLButtonElement>('tbody .btn-danger')!.click();
    fixture.detectChanges();
    expect(el().querySelector('[role="alertdialog"]')).not.toBeNull();
    expect(service.delete).not.toHaveBeenCalled();

    el().querySelector<HTMLButtonElement>('[role="alertdialog"] .btn-danger')!.click();
    fixture.detectChanges();
    expect(service.delete).toHaveBeenCalledOnceWith(12345678);
    expect(service.list).toHaveBeenCalledTimes(2);
    expect(el().querySelector('[role="alertdialog"]')).toBeNull();
    expect(el().textContent).toContain('No active clients.');
  });

  it('cancelling the confirm dialog does not delete', () => {
    service.list.and.returnValue(of([make(12345678, 'JUAN PEREZ')]));
    fixture = TestBed.createComponent(ClientListComponent);
    fixture.detectChanges();
    el().querySelector<HTMLButtonElement>('tbody .btn-danger')!.click();
    fixture.detectChanges();
    el().querySelector<HTMLButtonElement>('[role="alertdialog"] .btn:not(.btn-danger)')!.click();
    fixture.detectChanges();
    expect(service.delete).not.toHaveBeenCalled();
    expect(el().querySelector('[role="alertdialog"]')).toBeNull();
  });

  it('shows "API unreachable" when the API is down', () => {
    service.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fixture = TestBed.createComponent(ClientListComponent);
    fixture.detectChanges();
    expect(el().querySelector('[role="alert"]')?.textContent?.trim()).toBe('API unreachable');
  });
});
