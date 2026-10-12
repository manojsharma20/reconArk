import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox, formatDate } from '../../shared/components';
import type { Run } from '../../shared/types';

/** Completed runs, each linking to its report. */
export function ReportsPage() {
  const runs = useQuery({ queryKey: ['runs'], queryFn: () => api.get<Run[]>('/api/runs') });
  const completed = runs.data?.filter((r) => r.status === 'COMPLETED') ?? [];
  return (
    <>
      <header className="page-header">
        <h1>Reports</h1>
      </header>
      <ErrorBox error={runs.error} />
      <Card title="Completed runs">
        <ul className="plain">
          {completed.map((r) => (
            <li key={r.id}>
              <Link to={`/reports/${r.id}`}>
                {r.definitionName} — {formatDate(r.finishedAt)}
              </Link>
            </li>
          ))}
          {runs.isSuccess && completed.length === 0 && <li className="muted">No completed runs yet.</li>}
        </ul>
      </Card>
    </>
  );
}
