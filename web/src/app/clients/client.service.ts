import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Client } from './client.model';

export const CLIENTS_API = '/api/clients';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  /** Active clients only unless `includeDeleted` (soft-deleted rows stay in TRCLIE). */
  list(includeDeleted = false): Observable<Client[]> {
    return includeDeleted
      ? this.http.get<Client[]>(CLIENTS_API, { params: { includeDeleted: 'true' } })
      : this.http.get<Client[]>(CLIENTS_API);
  }

  get(dni: number): Observable<Client> {
    return this.http.get<Client>(`${CLIENTS_API}/${dni}`);
  }

  create(dni: number, name: string): Observable<Client> {
    return this.http.post<Client>(`${CLIENTS_API}/${dni}`, { name });
  }

  update(dni: number, name: string): Observable<Client> {
    return this.http.put<Client>(`${CLIENTS_API}/${dni}`, { name });
  }

  /** Soft delete; resolves with the record stamped with `deletedAt`/`deletedBy`. */
  delete(dni: number): Observable<Client> {
    return this.http.delete<Client>(`${CLIENTS_API}/${dni}`);
  }
}
