import type { ReactNode } from 'react';
import { ApiError } from './api';

/** Uniform loading / error / data rendering for query results. */
export function Async<T>({
  query,
  children,
}: {
  query: { isPending: boolean; error: unknown; data: T | undefined };
  children: (data: T) => ReactNode;
}) {
  if (query.isPending) return <p className="muted">Loading…</p>;
  if (query.error) {
    const e = query.error;
    const msg = e instanceof ApiError ? `${e.problem.code ?? e.status}: ${e.message}` : String(e);
    return (
      <p className="error" role="alert">
        {msg}
      </p>
    );
  }
  return <>{query.data !== undefined && children(query.data)}</>;
}
