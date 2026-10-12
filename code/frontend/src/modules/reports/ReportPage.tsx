import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox, formatDate, formatNumber, StatusBadge } from '../../shared/components';
import type { MatchStatus, PluginInfo, ResultPage, RunSummary } from '../../shared/types';

const filters: { label: string; status: MatchStatus | null }[] = [
  { label: 'All', status: null },
  { label: 'Mismatched', status: 'MISMATCHED' },
  { label: 'Only in left', status: 'ONLY_IN_LEFT' },
  { label: 'Only in right', status: 'ONLY_IN_RIGHT' },
  { label: 'Matched', status: 'MATCHED' },
];
const PAGE_SIZE = 50;

/** The report of one run: summary, results by status (paged) and downloads. */
export function ReportPage() {
  const { runId } = useParams();
  const [status, setStatus] = useState<MatchStatus | null>('MISMATCHED');
  const [page, setPage] = useState(0);

  const summary = useQuery({
    queryKey: ['summary', runId],
    queryFn: () => api.get<RunSummary>(`/api/reports/runs/${runId}/summary`),
  });
  const results = useQuery({
    queryKey: ['results', runId, status, page],
    queryFn: () =>
      api.get<ResultPage>(
        `/api/reports/runs/${runId}/results?page=${page}&size=${PAGE_SIZE}${status ? `&status=${status}` : ''}`,
      ),
  });
  const formats = useQuery({
    queryKey: ['report-formats'],
    queryFn: () => api.get<PluginInfo[]>('/api/reports/formats'),
  });

  if (summary.error) return <ErrorBox error={summary.error} />;
  if (!summary.data) return <p className="muted">Loading…</p>;
  const s = summary.data;
  const fields = s.fields;
  const pages = Math.max(1, Math.ceil((results.data?.total ?? 0) / PAGE_SIZE));

  return (
    <>
      <header className="page-header">
        <h1>
          {s.definitionName} <StatusBadge status={s.status} />
        </h1>
        <div className="actions">
          {formats.data?.map((f) => (
            <a
              key={f.id}
              className="button"
              href={`/api/reports/runs/${runId}/export?format=${f.id}${status ? `&status=${status}` : ''}`}
            >
              Download {f.id.toUpperCase()}
              {status ? ` (${status.toLowerCase().replaceAll('_', ' ')})` : ''}
            </a>
          ))}
        </div>
      </header>
      <p className="muted">Finished {formatDate(s.finishedAt)}</p>

      <div className="stats">
        <div className="stat highlight">
          <div className="stat-value">{s.matchRatePercent}%</div>
          <div className="muted">Match rate</div>
        </div>
        <Stat label="Matched" value={s.matched} />
        <Stat label="Mismatched" value={s.mismatched} />
        <Stat label="Only in left" value={s.onlyInLeft} />
        <Stat label="Only in right" value={s.onlyInRight} />
        <Stat label="Rejected rows" value={s.rejected} />
      </div>

      <Card title="Results">
        <div className="tabs">
          {filters.map((f) => (
            <button
              key={f.label}
              className={f.status === status ? 'active' : ''}
              onClick={() => {
                setStatus(f.status);
                setPage(0);
              }}
            >
              {f.label}
            </button>
          ))}
        </div>
        <ErrorBox error={results.error} />
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Match key</th>
                <th>Status</th>
                <th>Differences</th>
                {fields.map((f) => (
                  <th key={`l-${f}`}>Left {f}</th>
                ))}
                {fields.map((f) => (
                  <th key={`r-${f}`}>Right {f}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {results.data?.items.map((row, i) => (
                <tr key={`${row.matchKey}-${i}`}>
                  <td>{row.matchKey}</td>
                  <td>
                    <StatusBadge status={row.status} />
                  </td>
                  <td>
                    {row.differences.map((d) => (
                      <div key={d.field}>
                        <b>{d.field}</b>: {d.leftValue ?? '∅'} ≠ {d.rightValue ?? '∅'}
                      </div>
                    ))}
                  </td>
                  {fields.map((f) => (
                    <td key={`l-${f}`}>{row.left?.[f] ?? ''}</td>
                  ))}
                  {fields.map((f) => (
                    <td key={`r-${f}`}>{row.right?.[f] ?? ''}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="pager">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span>
            Page {page + 1} of {pages} · {formatNumber(results.data?.total ?? 0)} results
          </span>
          <button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>
            Next
          </button>
        </div>
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
