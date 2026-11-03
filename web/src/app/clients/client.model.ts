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

export interface ClientPayload {
  name: string;
}

/** RFC 7807 problem+json body returned by the clients API, with the extra `code` property. */
export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
}
