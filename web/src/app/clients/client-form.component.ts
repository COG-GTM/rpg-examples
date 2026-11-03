import { Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Client } from './client.model';
import { ClientService } from './client.service';
import { DNI_MAX, DNI_MIN, NAME_MAX_LENGTH, dniValidator } from './dni.validator';

/** Create (/clients/new) and edit (/clients/:dni/edit) form. DNI is the key and is read-only on edit. */
@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a routerLink="/clients">&larr; Back to list</a>
    <h2>{{ isEdit() ? 'Edit client ' + dni() : 'New client' }}</h2>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <label>
        DNI
        <input type="number" formControlName="dni" [min]="dniMin" [max]="dniMax" [readonly]="isEdit()" />
      </label>
      @if (form.controls.dni.touched && form.controls.dni.errors; as errs) {
        <p class="field-error">
          @if (errs['required']) { DNI is required } @else { DNI not valid (must be {{ dniMin }} - {{ dniMax }}) }
        </p>
      }
      <label>
        Name
        <input type="text" formControlName="name" [maxlength]="nameMax" />
      </label>
      @if (form.controls.name.touched && form.controls.name.errors) {
        <p class="field-error">Name is required (max {{ nameMax }} characters)</p>
      }
      <div class="toolbar">
        <button type="submit" class="button primary" [disabled]="saving()">{{ isEdit() ? 'Save' : 'Create' }}</button>
        <a routerLink="/clients">Cancel</a>
      </div>
    </form>
  `,
})
export class ClientFormComponent implements OnInit {
  private readonly service = inject(ClientService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Route param; undefined on /clients/new. */
  readonly dni = input<string>();
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly dniMin = DNI_MIN;
  readonly dniMax = DNI_MAX;
  readonly nameMax = NAME_MAX_LENGTH;

  readonly form = this.fb.group({
    dni: this.fb.control<number | null>(null, [Validators.required, dniValidator]),
    name: this.fb.control('', [Validators.required, Validators.maxLength(NAME_MAX_LENGTH)]),
  });

  isEdit(): boolean {
    return !!this.dni();
  }

  ngOnInit(): void {
    if (this.isEdit()) {
      this.service.get(Number(this.dni())).subscribe({
        next: (c) => this.form.setValue({ dni: c.dni, name: c.name }),
        error: (e: Error) => this.error.set(e.message),
      });
    }
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const { dni, name } = this.form.getRawValue();
    const call: Observable<Client> = this.isEdit()
      ? this.service.update(Number(dni), name)
      : this.service.create(Number(dni), name);
    this.saving.set(true);
    call.subscribe({
      next: (c) => this.router.navigate(['/clients', c.dni]),
      error: (e: Error) => {
        this.error.set(e.message);
        this.saving.set(false);
      },
    });
  }
}
