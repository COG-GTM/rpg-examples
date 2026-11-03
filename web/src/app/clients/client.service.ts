import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { ApiProblem, Client } from './client.model';

/** Base URL of the REST API; the dev server proxies /api -> http://localhost:8080. */
export const API_BASE = '/api/clients';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  list(): Observable<Client[]> {
    return this.http.get<Client[]>(API_BASE).pipe(catchError(toMessage));
  }

  get(dni: number): Observable<Client> {
    return this.http.get<Client>(`${API_BASE}/${dni}`).pipe(catchError(toMessage));
  }

  create(dni: number, name: string): Observable<Client> {
    return this.http.post<Client>(`${API_BASE}/${dni}`, { name }).pipe(catchError(toMessage));
  }

  update(dni: number, name: string): Observable<Client> {
    return this.http.put<Client>(`${API_BASE}/${dni}`, { name }).pipe(catchError(toMessage));
  }

  delete(dni: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/${dni}`).pipe(catchError(toMessage));
  }
}

/** Surface the CLIESRV message (e.g. "DNI already exists") as the error text. */
function toMessage(err: HttpErrorResponse): Observable<never> {
  const problem = err.error as ApiProblem | null;
  const message = problem?.detail ?? (err.status === 0 ? 'API not reachable' : err.message);
  return throwError(() => new Error(message));
}
