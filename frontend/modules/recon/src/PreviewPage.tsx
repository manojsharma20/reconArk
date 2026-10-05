import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, postJson } from '@reconark/module-sdk';

const SAMPLE = {
  ruleSet: {
    id: 'RS-ACQ-A',
    leftSource: 'PAYMENTS_CORE',
    rightSource: 'ACQUIRER_A',
    matchKey: { leftFields: ['txnRef'], rightFields: ['partnerRef'], normalizations: ['TRIM', 'UPPER_CASE'] },
    strategy: 'one-to-one',
    strategyOptions: {},
    compareRules: [
      {
        leftField: 'amt',
        rightField: 'amount',
        comparator: 'numeric-tolerance',
        parameters: { scale: 2 },
        severity: 'BLOCKING',
        sensitive: false,
      },
      {
        leftField: 'status',
        rightField: 'status',
        comparator: 'value-map',
        parameters: { mapping: { '00': 'SUCCESS' } },
        severity: 'BLOCKING',
        sensitive: false,
      },
    ],
  },
  left: [
    {
      sourceId: 'PAYMENTS_CORE',
      recordKey: '1',
      businessDate: '2026-10-01',
      fields: { txnRef: 'tx1', amt: '100.00', status: 'SUCCESS' },
    },
    {
      sourceId: 'PAYMENTS_CORE',
      recordKey: '2',
      businessDate: '2026-10-01',
      fields: { txnRef: 'TX2', amt: '50.00', status: 'SUCCESS' },
    },
  ],
  right: [
    {
      sourceId: 'ACQUIRER_A',
      recordKey: 'TX1',
      businessDate: '2026-10-01',
      fields: { partnerRef: 'TX1', amount: '100.00', status: '00' },
    },
    {
      sourceId: 'ACQUIRER_A',
      recordKey: 'TX2',
      businessDate: '2026-10-01',
      fields: { partnerRef: 'TX2', amount: '50.50', status: '00' },
    },
  ],
};

interface Outcome {
  matchKey: string;
  status: string;
  leftKeys: string[];
  rightKeys: string[];
  diffs: { leftField: string; comparator: string; severity: string; explanation: string }[];
}

/** Rule-set preview: the production recon engine, run on a sample, before a rule-set version is submitted. */
export function PreviewPage() {
  const [input, setInput] = useState(JSON.stringify(SAMPLE, null, 2));
  const run = useMutation({
    mutationFn: () => postJson<Outcome[]>('/api/recon/v1/preview', JSON.parse(input) as unknown),
  });
  return (
    <section>
      <h1>Rule-set preview</h1>
      <label className="field">
        <span className="field-name">Rule set and sample records</span>
        <textarea rows={22} value={input} onChange={(e) => setInput(e.target.value)} spellCheck={false} />
      </label>
      <div className="actions">
        <button onClick={() => run.mutate()} disabled={run.isPending}>
          Reconcile
        </button>
      </div>
      {run.error && (
        <p className="error" role="alert">
          {run.error instanceof ApiError ? run.error.message : String(run.error)}
        </p>
      )}
      {run.data && (
        <table>
          <thead>
            <tr>
              <th>Key</th>
              <th>Outcome</th>
              <th>Differences</th>
            </tr>
          </thead>
          <tbody>
            {run.data.map((o) => (
              <tr key={o.matchKey}>
                <td>{o.matchKey}</td>
                <td>
                  <span className={`status status-${o.status.toLowerCase()}`}>{o.status}</span>
                </td>
                <td>
                  {o.diffs.map((d) => `${d.leftField} (${d.comparator}): ${d.explanation}`).join(' · ')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
