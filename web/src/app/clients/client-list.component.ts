import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Client } from './client.model';
import { ClientService } from './client.service';

@Component({
  selector: 'app-client-list',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="toolbar">
      <h2>Active clients</h2>
      <a class="button primary" routerLink="/clients/new">New client</a>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (clients().length === 0 && !error()) {
      <p class="muted">No active clients.</p>
    } @else {
      <table>
        <thead>
          <tr><th>DNI</th><th>Name</th><th>Created</th><th>Created by</th><th></th></tr>
        </thead>
        <tbody>
          @for (c of clients(); track c.dni) {
            <tr>
              <td><a [routerLink]="['/clients', c.dni]">{{ c.dni }}</a></td>
              <td>{{ c.name }}</td>
              <td>{{ c.createdAt | date: 'medium' }}</td>
              <td>{{ c.createdBy }}</td>
              <td class="actions">
                <a [routerLink]="['/clients', c.dni, 'edit']">Edit</a>
                <button type="button" class="link danger" (click)="remove(c)">Delete</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class ClientListComponent implements OnInit {
  private readonly service = inject(ClientService);
  readonly clients = signal<Client[]>([]);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.service.list().subscribe({
      next: (list) => {
        this.clients.set(list);
        this.error.set(null);
      },
      error: (e: Error) => this.error.set(e.message),
    });
  }

  remove(c: Client): void {
    if (!confirm(`Delete client ${c.dni} - ${c.name}?`)) {
      return;
    }
    this.service.delete(c.dni).subscribe({
      next: () => this.load(),
      error: (e: Error) => this.error.set(e.message),
    });
  }
}
