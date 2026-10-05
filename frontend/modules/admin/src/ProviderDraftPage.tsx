import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, postJson } from '@reconark/module-sdk';

const SAMPLE_PIPELINE = JSON.stringify(
  [
    { key: 'parse', options: { format: 'delimited', 'format-options': { delimiter: ',' } } },
    {
      key: 'map',
      options: {
        fields: {
          partnerRef: { source: 'ref', required: true },
          amount: { source: 'amount', type: 'decimal', required: true },
          txDate: { source: 'date', type: 'date' },
        },
      },
    },
    { key: 'canonicalize', options: { 'record-key-field': 'partnerRef', 'business-date-field': 'txDate' } },
  ],
  null,
  2,
);

interface Version {
  version: number;
  state: string;
}
interface DryRun {
  read: number;
  accepted: number;
  rejected: number;
  rejects: { position: number; violations: string[] }[];
  preview: Record<string, string>[];
}

/** Configuration-only onboarding: declare the stage graph, dry-run it on a sample, then submit for approval. */
export function ProviderDraftPage() {
  const [code, setCode] = useState('ACQUIRER_A');
  const [pipeline, setPipeline] = useState(SAMPLE_PIPELINE);
  const [sample, setSample] = useState('ref,amount,date\nTX1,100.00,2026-10-01\nTX2,abc,2026-10-01\n');
  const [version, setVersion] = useState<Version | null>(null);

  const create = useMutation({
    mutationFn: () =>
      postJson<Version>(`/api/admin/v1/providers/${code}/versions`, {
        description: 'drafted in the UI',
        pipeline: JSON.parse(pipeline) as unknown,
      }),
    onSuccess: setVersion,
  });
  const dryRun = useMutation({
    mutationFn: () =>
      postJson<DryRun>(`/api/admin/v1/providers/${code}/versions/${version?.version}/dry-run`, { sample }),
  });
  const error = create.error ?? dryRun.error;

  return (
    <section>
      <h1>Provider configuration</h1>
      <div className="grid-2">
        <label className="field">
          <span className="field-name">Provider code</span>
          <input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} />
        </label>
        <div />
        <label className="field">
          <span className="field-name">Stage graph (pipeline-stage keys + options)</span>
          <textarea
            rows={18}
            value={pipeline}
            onChange={(e) => setPipeline(e.target.value)}
            spellCheck={false}
          />
        </label>
        <label className="field">
          <span className="field-name">Sample data</span>
          <textarea rows={18} value={sample} onChange={(e) => setSample(e.target.value)} spellCheck={false} />
        </label>
      </div>
      <div className="actions">
        <button onClick={() => create.mutate()} disabled={create.isPending}>
          Save draft
        </button>
        <button onClick={() => dryRun.mutate()} disabled={!version || dryRun.isPending}>
          Dry-run v{version?.version ?? '?'}
        </button>
        {version && (
          <span className="tag">
            v{version.version} · {version.state}
          </span>
        )}
      </div>
      {error && (
        <p className="error" role="alert">
          {error instanceof ApiError
            ? `${error.problem.code ?? error.status}: ${error.message}`
            : String(error)}
        </p>
      )}
      {dryRun.data && (
        <div>
          <p>
            read {dryRun.data.read} · accepted {dryRun.data.accepted} · rejected {dryRun.data.rejected}
          </p>
          <ul>
            {dryRun.data.rejects.map((r) => (
              <li key={r.position}>
                row {r.position}: {r.violations.join('; ')}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
