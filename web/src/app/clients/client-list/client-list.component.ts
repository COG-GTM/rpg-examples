import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { FlashService } from '../flash.service';

@Component({
  selector: 'app-client-list',
  imports: [RouterLink, DatePipe, ConfirmDialogComponent],
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit {
  private readonly clients$ = inject(ClientService);
  private readonly flash = inject(FlashService);

  readonly clients = signal<Client[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly pendingDelete = signal<Client | null>(null);
  readonly deleting = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.clients$.list().subscribe({
      next: (clients) => {
        this.clients.set(clients);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(mapApiError(err).message);
        this.loading.set(false);
      },
    });
  }

  askDelete(client: Client): void {
    this.flash.clear();
    this.pendingDelete.set(client);
  }

  cancelDelete(): void {
    this.pendingDelete.set(null);
  }

  confirmDelete(): void {
    const client = this.pendingDelete();
    if (!client) {
      return;
    }
    this.deleting.set(true);
    this.clients$.delete(client.dni).subscribe({
      next: () => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.flash.show(`Client ${client.dni} marked as deleted`);
        this.load();
      },
      error: (err) => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.error.set(mapApiError(err).message);
        this.load();
      },
    });
  }
}
