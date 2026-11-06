import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { FlashService } from '../flash.service';
import { ClientFormComponent } from './client-form.component';

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

function problemError(status: number, code: string, detail: string): Observable<never> {
  return throwError(
    () => new HttpErrorResponse({ status, statusText: 'x', error: { status, code, detail, title: 'x' } }),
  );
}

describe('ClientFormComponent', () => {
  let fixture: ComponentFixture<ClientFormComponent>;
  let component: ClientFormComponent;
  let service: jasmine.SpyObj<ClientService>;
  let router: Router;

  const el = () => fixture.nativeElement as HTMLElement;
  const text = (selector: string) => el().querySelector(selector)?.textContent?.trim() ?? null;

  function setup(dni?: string): void {
    fixture = TestBed.createComponent(ClientFormComponent);
    component = fixture.componentInstance;
    if (dni !== undefined) {
      fixture.componentRef.setInput('dni', dni);
    }
    fixture.detectChanges();
  }

  function fill(dni: string, name: string): void {
    component.form.controls.dni.setValue(dni);
    component.form.controls.name.setValue(name);
  }

  beforeEach(() => {
    service = jasmine.createSpyObj<ClientService>('ClientService', ['get', 'create', 'update']);
    TestBed.configureTestingModule({
      imports: [ClientFormComponent],
      providers: [provideRouter([]), { provide: ClientService, useValue: service }],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  describe('create mode', () => {
    beforeEach(() => setup());

    it('shows the DNI range message inline and does not call the API', () => {
      fill('999999', 'Ana');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="dni-error"]')).toBe('DNI must be between 1,000,000 and 99,999,999');
      expect(service.create).not.toHaveBeenCalled();
    });

    it('rejects a blank or too-long name', () => {
      fill('12345678', '   ');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="name-error"]')).toBe('Name is required');

      component.form.controls.name.setValue('x'.repeat(31));
      fixture.detectChanges();
      expect(text('[data-testid="name-error"]')).toBe('Name must be at most 30 characters');
      expect(service.create).not.toHaveBeenCalled();
    });

    it('creates the client, flashes and navigates to the detail', () => {
      service.create.and.returnValue(of(CLIENT));
      fill('12345678', '  JUAN PEREZ ');
      component.submit();
      expect(service.create).toHaveBeenCalledOnceWith(12345678, 'JUAN PEREZ');
      expect(router.navigate).toHaveBeenCalledWith(['/clients', 12345678]);
      expect(TestBed.inject(FlashService).message()).toBe('Client 12345678 created');
    });

    it('shows DUPDNI inline on the DNI field', () => {
      service.create.and.returnValue(problemError(409, 'DUPDNI', 'DNI already exists'));
      fill('12345678', 'Dup');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="dni-error"]')).toBe('DNI already exists');
      expect(text('[data-testid="banner"]')).toBeNull();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('shows INVDNI inline on the DNI field', () => {
      service.create.and.returnValue(problemError(400, 'INVDNI', 'DNI not valid'));
      fill('12345678', 'Ana');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="dni-error"]')).toBe('DNI not valid');
    });

    it('shows VALIDATION inline on the name field', () => {
      service.create.and.returnValue(problemError(400, 'VALIDATION', 'name: must not be blank'));
      fill('12345678', 'Ana');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="name-error"]')).toBe('name: must not be blank');
    });

    it('clears a server field error once the user edits the field', () => {
      service.create.and.returnValue(problemError(409, 'DUPDNI', 'DNI already exists'));
      fill('12345678', 'Dup');
      component.submit();
      component.form.controls.dni.setValue('12345679');
      fixture.detectChanges();
      expect(component.form.controls.dni.valid).toBeTrue();
      expect(text('[data-testid="dni-error"]')).toBeNull();
    });

    it('shows "API unreachable" as a banner for a body-less 500', () => {
      service.create.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error', error: null })),
      );
      fill('12345678', 'Ana');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="banner"]')).toBe('API unreachable');
    });

    it('shows a real IOERR problem detail as a banner', () => {
      service.create.and.returnValue(problemError(500, 'IOERR', 'TRCLIE I/O error'));
      fill('12345678', 'Ana');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="banner"]')).toBe('TRCLIE I/O error');
    });
  });

  describe('edit mode', () => {
    it('loads the client and makes the DNI read-only', () => {
      service.get.and.returnValue(of(CLIENT));
      setup('12345678');
      fixture.detectChanges();
      const dniInput = el().querySelector<HTMLInputElement>('#dni')!;
      expect(service.get).toHaveBeenCalledOnceWith(12345678);
      expect(dniInput.readOnly).toBeTrue();
      expect(dniInput.disabled).toBeTrue();
      expect(dniInput.value).toBe('12345678');
      expect(component.form.controls.name.value).toBe('JUAN PEREZ');
    });

    it('updates with the route DNI and new name', () => {
      service.get.and.returnValue(of(CLIENT));
      service.update.and.returnValue(of({ ...CLIENT, name: 'JUAN P. PEREZ' }));
      setup('12345678');
      component.form.controls.name.setValue('JUAN P. PEREZ');
      component.submit();
      expect(service.update).toHaveBeenCalledOnceWith(12345678, 'JUAN P. PEREZ');
      expect(router.navigate).toHaveBeenCalledWith(['/clients', 12345678]);
    });

    it('shows DELETED on load as a banner', () => {
      service.get.and.returnValue(problemError(410, 'DELETED', 'Client marked as deleted'));
      setup('12345678');
      fixture.detectChanges();
      expect(text('[data-testid="banner"]')).toBe('Client marked as deleted');
    });

    it('shows NOTFND on save as a banner', () => {
      service.get.and.returnValue(of(CLIENT));
      service.update.and.returnValue(problemError(404, 'NOTFND', 'DNI not exists'));
      setup('12345678');
      component.submit();
      fixture.detectChanges();
      expect(text('[data-testid="banner"]')).toBe('DNI not exists');
    });
  });
});
