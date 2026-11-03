import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { mapApiError } from '../api-error';
import { Client } from '../client.model';
import { ClientService } from '../client.service';
import { NotificationService } from '../notification.service';

@Component({
  selector: 'app-client-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './client-detail.component.html',
})
export class ClientDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clients = inject(ClientService);
  private readonly notify = inject(NotificationService);

  readonly client = signal<Client | null>(null);
  readonly error = signal<string | null>(null);
  readonly dni = signal<number>(0);

  ngOnInit(): void {
    this.dni.set(Number(this.route.snapshot.paramMap.get('dni')));
    this.clients.get(this.dni()).subscribe({
      next: (client) => this.client.set(client),
      error: (err) => this.error.set(mapApiError(err).message),
    });
  }

  remove(client: Client): void {
    if (!window.confirm(`Delete client ${client.dni} - ${client.name}?`)) return;
    this.clients.delete(client.dni).subscribe({
      next: () => {
        this.notify.success(`Client ${client.dni} marked as deleted`);
        this.router.navigate(['/clients']);
      },
      error: (err) => this.notify.error(mapApiError(err).message),
    });
  }
}
