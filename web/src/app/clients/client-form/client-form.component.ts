import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiErrorInfo, mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';
import {
  NAME_MAX_LENGTH,
  dniErrorMessage,
  dniValidators,
  nameErrorMessage,
  nameValidators,
} from '../dni.validator';
import { NotificationService } from '../notification.service';

export type ClientFormMode = 'create' | 'edit';

@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './client-form.component.html',
})
export class ClientFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clients = inject(ClientService);
  private readonly notify = inject(NotificationService);

  readonly nameMaxLength = NAME_MAX_LENGTH;
  readonly mode = signal<ClientFormMode>('create');
  readonly submitting = signal(false);
  readonly loadError = signal<string | null>(null);
  /** Last API error that is not tied to a specific field. */
  readonly formError = signal<string | null>(null);

  readonly form = new FormGroup({
    dni: new FormControl<string>('', { nonNullable: true, validators: dniValidators }),
    name: new FormControl<string>('', { nonNullable: true, validators: nameValidators }),
  });

  ngOnInit(): void {
    this.mode.set(this.route.snapshot.data['mode'] === 'edit' ? 'edit' : 'create');
    if (this.mode() === 'edit') {
      const dni = Number(this.route.snapshot.paramMap.get('dni'));
      this.form.controls.dni.setValue(String(dni));
      this.form.controls.dni.disable();
      this.clients.get(dni).subscribe({
        next: (client) => this.form.controls.name.setValue(client.name),
        error: (err) => this.loadError.set(mapApiError(err).message),
      });
    }
  }

  get dniError(): string | null {
    const control = this.form.controls.dni;
    if (control.errors?.['api']) return control.errors['api'];
    return control.touched || control.dirty ? dniErrorMessage(control) : null;
  }

  get nameError(): string | null {
    const control = this.form.controls.name;
    if (control.errors?.['api']) return control.errors['api'];
    return control.touched || control.dirty ? nameErrorMessage(control) : null;
  }

  submit(): void {
    this.formError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dni = Number(this.form.controls.dni.value);
    const payload = { name: this.form.controls.name.value.trim() };
    const request$ =
      this.mode() === 'edit' ? this.clients.update(dni, payload) : this.clients.create(dni, payload);

    this.submitting.set(true);
    request$.subscribe({
      next: (client: Client) => {
        this.submitting.set(false);
        this.notify.success(
          this.mode() === 'edit' ? `Client ${client.dni} updated` : `Client ${client.dni} created`,
        );
        this.router.navigate(['/clients', client.dni]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.applyApiError(mapApiError(err));
      },
    });
  }

  /** Shows the mapped API error inline on the related field (or as a form banner) and as a toast. */
  applyApiError(info: ApiErrorInfo): void {
    const control = info.field ? this.form.controls[info.field] : null;
    if (control && control.enabled) {
      control.setErrors({ ...(control.errors ?? {}), api: info.message });
      control.markAsTouched();
    } else {
      this.formError.set(info.message);
    }
    this.notify.error(info.message);
  }
}
