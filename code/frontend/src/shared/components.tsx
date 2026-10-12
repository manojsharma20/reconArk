import type { ReactNode } from 'react';

const statusColours: Record<string, string> = {
  MATCHED: 'good',
  COMPLETED: 'good',
  DONE: 'good',
  MISMATCHED: 'warn',
  LOADING: 'busy',
  MATCHING: 'busy',
  QUEUED: 'busy',
  ONLY_IN_LEFT: 'info',
  ONLY_IN_RIGHT: 'info',
  FAILED: 'bad',
};

/** A coloured label for any status value. */
export function StatusBadge({ status }: { status: string }) {
  return <span className={`badge ${statusColours[status] ?? ''}`}>{status.replaceAll('_', ' ')}</span>;
}

/** Shows an error from a failed request, or nothing. */
export function ErrorBox({ error }: { error: unknown }) {
  if (!error) return null;
  return <p className="error">{error instanceof Error ? error.message : String(error)}</p>;
}

export function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

export function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '—';
}

export function formatNumber(value: number): string {
  return value.toLocaleString();
}
