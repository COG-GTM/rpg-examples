import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Client } from './client.model';
import { ClientService } from './client.service';

@Component({
  selector: 'app-client-detail',
  imports: [RouterLink, DatePipe],
  template: `
    <a routerLink="/clients">&larr; Back to list</a>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (client(); as c) {
      <h2>{{ c.name }} <span class="muted">#{{ c.dni }}</span></h2>
      <dl>
        <dt>DNI</dt><dd>{{ c.dni }}</dd>
        <dt>Name</dt><dd>{{ c.name }}</dd>
        <dt>Status</dt><dd>{{ c.active ? 'Active' : 'Deleted' }}</dd>
        <dt>Created</dt><dd>{{ c.createdAt | date: 'medium' }} by {{ c.createdBy }}</dd>
        <dt>Last update</dt>
        <dd>@if (c.updatedAt) { {{ c.updatedAt | date: 'medium' }} by {{ c.updatedBy }} } @else { &mdash; }</dd>
      </dl>
      <div class="toolbar">
        <a class="button primary" [routerLink]="['/clients', c.dni, 'edit']">Edit</a>
        <button type="button" class="button danger" (click)="remove(c)">Delete</button>
      </div>
    }
  `,
})
export class ClientDetailComponent implements OnInit {
  private readonly service = inject(ClientService);
  private readonly router = inject(Router);
  readonly dni = input.required<string>();
  readonly client = signal<Client | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.service.get(Number(this.dni())).subscribe({
      next: (c) => this.client.set(c),
      error: (e: Error) => this.error.set(e.message),
    });
  }

  remove(c: Client): void {
    if (!confirm(`Delete client ${c.dni} - ${c.name}?`)) {
      return;
    }
    this.service.delete(c.dni).subscribe({
      next: () => this.router.navigate(['/clients']),
      error: (e: Error) => this.error.set(e.message),
    });
  }
}
