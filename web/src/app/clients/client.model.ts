/** Mirrors the Spring Boot `Client` DTO (one TRCLIE row). */
export interface Client {
  dni: number;
  name: string;
  createdAt: string | null;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string;
  deletedAt: string | null;
  deletedBy: string;
  active: boolean;
}

/** RFC 7807 body returned by the API; `code` is the CLIESRV status code. */
export interface ApiProblem {
  title?: string;
  detail?: string;
  code?: string;
  status?: number;
}
