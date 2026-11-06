import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ApiError, mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { DNI_RANGE_MESSAGE, NAME_MAX_LENGTH, dniValidator, notBlankValidator } from '../dni.validator';
import { FlashService } from '../flash.service';

/** Create (`/clients/new`) and edit (`/clients/:dni/edit`) form. */
@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './client-form.component.html',
})
export class ClientFormComponent implements OnInit {
  private readonly service = inject(ClientService);
  private readonly router = inject(Router);
  private readonly flash = inject(FlashService);

  /** Bound from the `:dni` route parameter; absent on `/clients/new`. */
  readonly dni = input<string | undefined>(undefined);

  readonly nameMaxLength = NAME_MAX_LENGTH;
  readonly dniRangeMessage = DNI_RANGE_MESSAGE;

  readonly form = new FormGroup({
    dni: new FormControl('', { nonNullable: true, validators: [Validators.required, dniValidator] }),
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlankValidator, Validators.maxLength(NAME_MAX_LENGTH)],
    }),
  });

  readonly banner = signal<string | null>(null);
  readonly saving = signal(false);
  readonly loading = signal(false);

  get isEdit(): boolean {
    return this.dni() !== undefined;
  }

  ngOnInit(): void {
    const dni = this.dni();
    if (dni === undefined) {
      return;
    }
    this.form.controls.dni.setValue(dni);
    this.form.controls.dni.disable();
    this.loading.set(true);
    this.service.get(Number(dni)).subscribe({
      next: (client) => {
        this.form.controls.name.setValue(client.name);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.applyError(mapApiError(err));
      },
    });
  }

  submit(): void {
    this.banner.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { dni, name } = this.form.getRawValue();
    const dniNumber = Number(dni.trim());
    const trimmedName = name.trim();
    const request: Observable<Client> = this.isEdit
      ? this.service.update(dniNumber, trimmedName)
      : this.service.create(dniNumber, trimmedName);

    this.saving.set(true);
    request.subscribe({
      next: (client) => {
        this.saving.set(false);
        this.flash.show(`Client ${client.dni} ${this.isEdit ? 'updated' : 'created'}`);
        this.router.navigate(['/clients', client.dni]);
      },
      error: (err) => {
        this.saving.set(false);
        this.applyError(mapApiError(err));
      },
    });
  }

  /** Field errors go inline on their control; everything else is a banner. */
  applyError(error: ApiError): void {
    const control = error.field ? this.form.controls[error.field] : null;
    if (control && control.enabled) {
      control.setErrors({ ...(control.errors ?? {}), server: error.message });
      control.markAsTouched();
    } else {
      this.banner.set(error.message);
    }
  }

  showErrors(name: 'dni' | 'name'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }
}
