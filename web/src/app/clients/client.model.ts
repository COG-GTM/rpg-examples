/** Contract B `Client` JSON (MBA-3805). Empty timestamps and users are `null`. */
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

/** Contract B error codes carried in the problem+json `code` member. */
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
