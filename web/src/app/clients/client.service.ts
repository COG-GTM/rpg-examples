import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Client } from './client.model';

export const CLIENTS_API = '/api/clients';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  list(): Observable<Client[]> {
    return this.http.get<Client[]>(CLIENTS_API);
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

  delete(dni: number): Observable<void> {
    return this.http.delete<void>(`${CLIENTS_API}/${dni}`);
  }
}
