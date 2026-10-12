import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox, formatDate, formatNumber, StatusBadge } from '../../shared/components';
import type { ReconDefinition, Run, UploadedFile } from '../../shared/types';

/** Start a run and follow the latest runs. The list refreshes every 3 seconds while a run is busy. */
export function RunsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [definitionId, setDefinitionId] = useState('');
  const [leftFileId, setLeftFileId] = useState('');
  const [rightFileId, setRightFileId] = useState('');

  const definitions = useQuery({
    queryKey: ['definitions'],
    queryFn: () => api.get<ReconDefinition[]>('/api/definitions'),
  });
  const files = useQuery({
    queryKey: ['files', definitionId],
    queryFn: () => api.get<UploadedFile[]>(`/api/files?definitionId=${encodeURIComponent(definitionId)}`),
    enabled: definitionId !== '',
  });
  const runs = useQuery({
    queryKey: ['runs'],
    queryFn: () => api.get<Run[]>('/api/runs'),
    refetchInterval: (query) =>
      query.state.data?.some((r) => r.status === 'LOADING' || r.status === 'MATCHING') ? 3000 : false,
  });
  const start = useMutation({
    mutationFn: () => api.post<Run>('/api/runs', { definitionId, leftFileId, rightFileId }),
    onSuccess: (run) => {
      void queryClient.invalidateQueries({ queryKey: ['runs'] });
      void navigate(`/runs/${run.id}`);
    },
  });

  const leftFiles = files.data?.filter((f) => f.side === 'LEFT') ?? [];
  const rightFiles = files.data?.filter((f) => f.side === 'RIGHT') ?? [];

  return (
    <>
      <header className="page-header">
        <h1>Runs</h1>
      </header>
      <ErrorBox error={definitions.error ?? runs.error} />
      <Card title="Start a run">
        <form
          className="inline-form"
          onSubmit={(e) => {
            e.preventDefault();
            start.mutate();
          }}
        >
          <label>
            Definition
            <select
              value={definitionId}
              onChange={(e) => {
                setDefinitionId(e.target.value);
                setLeftFileId('');
                setRightFileId('');
              }}
              required
            >
              <option value="">Choose…</option>
              {definitions.data?.map((d) => (
                <option key={d.id} value={d.id!}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <FileSelect label="Left file" files={leftFiles} value={leftFileId} onChange={setLeftFileId} />
          <FileSelect label="Right file" files={rightFiles} value={rightFileId} onChange={setRightFileId} />
          <button type="submit" className="primary" disabled={!leftFileId || !rightFileId || start.isPending}>
            Start run
          </button>
        </form>
        <ErrorBox error={start.error} />
      </Card>

      <Card title="Latest runs">
        <table>
          <thead>
            <tr>
              <th>Started</th>
              <th>Definition</th>
              <th>Status</th>
              <th className="num">Matched</th>
              <th className="num">Mismatched</th>
              <th className="num">Only left</th>
              <th className="num">Only right</th>
            </tr>
          </thead>
          <tbody>
            {runs.data?.map((r) => (
              <tr key={r.id}>
                <td>
                  <Link to={`/runs/${r.id}`}>{formatDate(r.createdAt)}</Link>
                </td>
                <td>{r.definitionName}</td>
                <td>
                  <StatusBadge status={r.status} />
                </td>
                <td className="num">{formatNumber(r.matched)}</td>
                <td className="num">{formatNumber(r.mismatched)}</td>
                <td className="num">{formatNumber(r.onlyInLeft)}</td>
                <td className="num">{formatNumber(r.onlyInRight)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}

function FileSelect(props: {
  label: string;
  files: UploadedFile[];
  value: string;
  onChange: (id: string) => void;
}) {
  return (
    <label>
      {props.label}
      <select value={props.value} onChange={(e) => props.onChange(e.target.value)} required>
        <option value="">{props.files.length ? 'Choose…' : 'Upload a file first'}</option>
        {props.files.map((f) => (
          <option key={f.id} value={f.id}>
            {f.originalName} ({new Date(f.uploadedAt).toLocaleString()})
          </option>
        ))}
      </select>
    </label>
  );
}
