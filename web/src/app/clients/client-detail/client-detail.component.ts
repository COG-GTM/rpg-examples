import { Component, inject, input, numberAttribute, OnChanges, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';

@Component({
  selector: 'app-client-detail',
  imports: [RouterLink],
  templateUrl: './client-detail.component.html',
})
export class ClientDetailComponent implements OnChanges {
  private readonly service = inject(ClientService);

  /** Bound from the `:dni` route parameter. */
  readonly dni = input.required({ transform: numberAttribute });

  readonly client = signal<Client | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnChanges(): void {
    this.loading.set(true);
    this.error.set(null);
    this.client.set(null);
    this.service.get(this.dni()).subscribe({
      next: (client) => {
        this.client.set(client);
        this.loading.set(false);
      },
      error: (err) => {
        const mapped = mapApiError(err);
        this.error.set(mapped.message);
        // CLIENTSV read rejects soft-deleted rows; show the record the list passed in, read-only.
        const fromList = (history.state as { client?: Client } | null)?.client;
        if (mapped.code === 'DELETED' && fromList?.dni === this.dni() && !fromList.active) {
          this.client.set(fromList);
        }
        this.loading.set(false);
      },
    });
  }
}
