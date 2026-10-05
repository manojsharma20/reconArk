import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, Async, postJson } from '@reconark/module-sdk';

/** Request an asynchronous report; available formats are the active report-renderer bricks. */
export function ReportsPage() {
  const formats = useQuery({
    queryKey: ['reports', 'formats'],
    queryFn: () => api<string[]>('/api/reports/v1/formats'),
  });
  const [definition, setDefinition] = useState('daily-recon-summary');
  const request = useMutation({
    mutationFn: (format: string) =>
      postJson<{ requestId: string; status: string }>('/api/reports/v1/requests', { definition, format }),
  });
  return (
    <section>
      <h1>Reports</h1>
      <label className="field">
        <span className="field-name">Definition</span>
        <input value={definition} onChange={(e) => setDefinition(e.target.value)} />
      </label>
      <Async query={formats}>
        {(list) => (
          <div className="actions">
            {list.map((f) => (
              <button key={f} onClick={() => request.mutate(f)}>
                Request {f.toUpperCase()}
              </button>
            ))}
          </div>
        )}
      </Async>
      {request.data && (
        <p>
          Request <code>{request.data.requestId}</code> is {request.data.status}.
        </p>
      )}
    </section>
  );
}
