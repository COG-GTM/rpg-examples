import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Client, ClientPayload } from './client.model';

export const CLIENTS_API_BASE = '/api/clients';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  list(): Observable<Client[]> {
    return this.http.get<Client[]>(CLIENTS_API_BASE);
  }

  get(dni: number): Observable<Client> {
    return this.http.get<Client>(`${CLIENTS_API_BASE}/${dni}`);
  }

  create(dni: number, payload: ClientPayload): Observable<Client> {
    return this.http.post<Client>(`${CLIENTS_API_BASE}/${dni}`, payload);
  }

  update(dni: number, payload: ClientPayload): Observable<Client> {
    return this.http.put<Client>(`${CLIENTS_API_BASE}/${dni}`, payload);
  }

  delete(dni: number): Observable<void> {
    return this.http.delete<void>(`${CLIENTS_API_BASE}/${dni}`);
  }
}
