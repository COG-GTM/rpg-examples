import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { NotificationService } from '../notification.service';

@Component({
  selector: 'app-client-list',
  imports: [RouterLink, DatePipe],
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit {
  private readonly clients = inject(ClientService);
  private readonly notify = inject(NotificationService);

  readonly items = signal<Client[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.clients.list().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(mapApiError(err).message);
        this.loading.set(false);
      },
    });
  }

  remove(client: Client): void {
    if (!window.confirm(`Delete client ${client.dni} - ${client.name}?`)) return;
    this.clients.delete(client.dni).subscribe({
      next: () => {
        this.notify.success(`Client ${client.dni} marked as deleted`);
        this.load();
      },
      error: (err) => this.notify.error(mapApiError(err).message),
    });
  }
}
