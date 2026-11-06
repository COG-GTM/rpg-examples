/** Client JSON returned by the clients API (CLIENTSV `client_t` / TRCLIE). Unset audit fields are `null`. */
export interface Client {
  dni: number;
  name: string;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
  deletedAt: string | null;
  deletedBy: string | null;
  active: boolean;
}

/** CLIENTSV result codes carried in the problem+json `code` member. */
export type ProblemCode = 'INVDNI' | 'VALIDATION' | 'NOTFND' | 'DUPDNI' | 'DELETED' | 'IOERR' | 'INVOP';

/** RFC 7807 problem details with the extra `code` member. */
export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: ProblemCode | string;
}
