import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { NotificationService } from '../notification.service';
import { ClientFormComponent } from './client-form.component';

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

function problem(status: number, code: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, detail: 'raw', code } });
}

describe('ClientFormComponent', () => {
  let fixture: ComponentFixture<ClientFormComponent>;
  let component: ClientFormComponent;
  let service: jasmine.SpyObj<ClientService>;
  let notify: NotificationService;

  function setup(mode: 'create' | 'edit', dni?: string): void {
    service = jasmine.createSpyObj<ClientService>('ClientService', ['get', 'create', 'update']);
    service.get.and.returnValue(of(client));
    TestBed.configureTestingModule({
      imports: [ClientFormComponent],
      providers: [
        provideRouter([]),
        { provide: ClientService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { mode }, paramMap: convertToParamMap(dni ? { dni } : {}) } },
        },
      ],
    });
    notify = TestBed.inject(NotificationService);
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ClientFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const text = (testId: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${testId}"]`)?.textContent?.trim();

  function fill(dni: string, name: string): void {
    component.form.controls.dni.setValue(dni);
    component.form.controls.name.setValue(name);
    component.form.markAllAsTouched();
  }

  it('shows the DNI range message and does not call the API for an out-of-range DNI', () => {
    setup('create');
    fill('999999', 'ANA PEREZ');
    component.submit();
    fixture.detectChanges();
    expect(text('dni-error')).toBe('DNI must be between 1,000,000 and 99,999,999');
    expect(service.create).not.toHaveBeenCalled();
  });

  it('maps a 409 DUPDNI problem to an inline "DNI already exists" error and a toast', () => {
    setup('create');
    service.create.and.returnValue(throwError(() => problem(409, 'DUPDNI')));
    fill('12345678', 'ANA PEREZ');
    component.submit();
    fixture.detectChanges();
    expect(service.create).toHaveBeenCalledWith(12345678, { name: 'ANA PEREZ' });
    expect(text('dni-error')).toBe('DNI already exists');
    expect(notify.toasts().map((t) => t.text)).toContain('DNI already exists');
  });

  it('clears the API error once the user edits the DNI', () => {
    setup('create');
    service.create.and.returnValue(throwError(() => problem(409, 'DUPDNI')));
    fill('12345678', 'ANA PEREZ');
    component.submit();
    component.form.controls.dni.setValue('12345679');
    fixture.detectChanges();
    expect(text('dni-error')).toBeUndefined();
  });

  it('shows DELETED as a form-level error on edit (DNI is read-only)', () => {
    setup('edit', '12345678');
    service.update.and.returnValue(throwError(() => problem(410, 'DELETED')));
    component.form.controls.name.setValue('ANA P');
    component.submit();
    fixture.detectChanges();
    expect(text('form-error')).toBe('Client marked as deleted');
  });

  it('shows NOTFND as a form-level error on edit', () => {
    setup('edit', '12345678');
    service.update.and.returnValue(throwError(() => problem(404, 'NOTFND')));
    component.form.controls.name.setValue('ANA P');
    component.submit();
    fixture.detectChanges();
    expect(text('form-error')).toBe('DNI not exists');
  });

  it('loads the client in edit mode with a read-only DNI and saves the trimmed name', () => {
    setup('edit', '12345678');
    expect(service.get).toHaveBeenCalledWith(12345678);
    expect(component.form.controls.dni.disabled).toBeTrue();
    const dniInput = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#dni')!;
    expect(dniInput.readOnly).toBeTrue();
    expect(component.form.controls.name.value).toBe('ANA PEREZ');

    service.update.and.returnValue(of({ ...client, name: 'ANA P' }));
    component.form.controls.name.setValue('  ANA P  ');
    component.submit();
    expect(service.update).toHaveBeenCalledWith(12345678, { name: 'ANA P' });
  });

  it('requires a non-blank name of at most 30 characters', () => {
    setup('create');
    fill('12345678', '   ');
    component.submit();
    fixture.detectChanges();
    expect(text('name-error')).toBe('Name is required');
    component.form.controls.name.setValue('A'.repeat(31));
    fixture.detectChanges();
    expect(text('name-error')).toBe('Name must be at most 30 characters');
    expect(service.create).not.toHaveBeenCalled();
  });
});
