import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox, formatDate, formatNumber, StatusBadge } from '../../shared/components';
import type { Job, Run } from '../../shared/types';

const busy = (run?: Run) => run?.status === 'LOADING' || run?.status === 'MATCHING';

/** One run: progress, counts and its jobs. Refreshes every 2 seconds until the run is finished. */
export function RunDetailPage() {
  const { runId } = useParams();
  const run = useQuery({
    queryKey: ['run', runId],
    queryFn: () => api.get<Run>(`/api/runs/${runId}`),
    refetchInterval: (query) => (busy(query.state.data) ? 2000 : false),
  });
  const jobs = useQuery({
    queryKey: ['run-jobs', runId],
    queryFn: () => api.get<Job[]>(`/api/runs/${runId}/jobs`),
    refetchInterval: busy(run.data) ? 2000 : false,
  });

  if (run.error) return <ErrorBox error={run.error} />;
  if (!run.data) return <p className="muted">Loading…</p>;
  const r = run.data;
  const done = jobs.data?.filter((j) => j.status === 'DONE').length ?? 0;

  return (
    <>
      <header className="page-header">
        <h1>
          {r.definitionName} <StatusBadge status={r.status} />
        </h1>
        {r.status === 'COMPLETED' && (
          <Link className="button primary" to={`/reports/${r.id}`}>
            Open report
          </Link>
        )}
      </header>
      {r.error && <p className="error">{r.error}</p>}
      <div className="stats">
        <Stat label="Left loaded" value={r.leftLoaded} />
        <Stat label="Right loaded" value={r.rightLoaded} />
        <Stat label="Rejected rows" value={r.rejected} />
        <Stat label="Matched" value={r.matched} />
        <Stat label="Mismatched" value={r.mismatched} />
        <Stat label="Only in left" value={r.onlyInLeft} />
        <Stat label="Only in right" value={r.onlyInRight} />
      </div>
      <p className="muted">
        Started {formatDate(r.createdAt)} · finished {formatDate(r.finishedAt)} · {r.bucketCount} buckets ·
        jobs done {done}/{jobs.data?.length ?? 0}
      </p>
      <Card title="Jobs">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Side / bucket</th>
              <th>Status</th>
              <th className="num">Attempts</th>
              <th className="num">Read</th>
              <th className="num">Rejected</th>
              <th className="num">Results</th>
              <th>Worker</th>
              <th>Error</th>
            </tr>
          </thead>
          <tbody>
            {jobs.data?.map((j) => (
              <tr key={j.id}>
                <td>{j.type}</td>
                <td>{j.side ?? `bucket ${j.bucket}`}</td>
                <td>
                  <StatusBadge status={j.status} />
                </td>
                <td className="num">{j.attempts}</td>
                <td className="num">{formatNumber(j.recordsRead)}</td>
                <td className="num">{formatNumber(j.recordsRejected)}</td>
                <td className="num">{formatNumber(j.resultsWritten)}</td>
                <td>{j.workerId ?? '—'}</td>
                <td className="error-text">{j.error ?? ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat">
      <div className="stat-value">{formatNumber(value)}</div>
      <div className="muted">{label}</div>
    </div>
  );
}
