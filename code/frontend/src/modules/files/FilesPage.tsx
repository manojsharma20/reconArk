import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { api } from '../../shared/api';
import { Card, ErrorBox, formatDate, formatNumber } from '../../shared/components';
import type { ReconDefinition, Side, UploadedFile } from '../../shared/types';

/** Upload the two files to reconcile, and see recent uploads. */
export function FilesPage() {
  const queryClient = useQueryClient();
  const [definitionId, setDefinitionId] = useState('');
  const [side, setSide] = useState<Side>('LEFT');
  const [file, setFile] = useState<File | null>(null);

  const definitions = useQuery({
    queryKey: ['definitions'],
    queryFn: () => api.get<ReconDefinition[]>('/api/definitions'),
  });
  const files = useQuery({
    queryKey: ['files'],
    queryFn: () => api.get<UploadedFile[]>('/api/files'),
  });
  const upload = useMutation({
    mutationFn: () => {
      const form = new FormData();
      form.append('file', file!);
      form.append('definitionId', definitionId);
      form.append('side', side);
      return api.upload<UploadedFile>('/api/files', form);
    },
    onSuccess: () => {
      setFile(null);
      void queryClient.invalidateQueries({ queryKey: ['files'] });
    },
  });

  const definitionName = (id: string) => definitions.data?.find((d) => d.id === id)?.name ?? id;
  const selected = definitions.data?.find((d) => d.id === definitionId);

  return (
    <>
      <header className="page-header">
        <h1>Files</h1>
      </header>
      <ErrorBox error={definitions.error ?? files.error} />
      <Card title="Upload a file">
        <form
          className="inline-form"
          onSubmit={(e) => {
            e.preventDefault();
            upload.mutate();
          }}
        >
          <label>
            Definition
            <select value={definitionId} onChange={(e) => setDefinitionId(e.target.value)} required>
              <option value="">Choose…</option>
              {definitions.data?.map((d) => (
                <option key={d.id} value={d.id!}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Side
            <select value={side} onChange={(e) => setSide(e.target.value as Side)}>
              <option value="LEFT">Left{selected ? ` — ${selected.left.name}` : ''}</option>
              <option value="RIGHT">Right{selected ? ` — ${selected.right.name}` : ''}</option>
            </select>
          </label>
          <label>
            File
            <input
              key={file ? 'chosen' : 'empty'}
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
            />
          </label>
          <button type="submit" className="primary" disabled={!file || !definitionId || upload.isPending}>
            {upload.isPending ? 'Uploading…' : 'Upload'}
          </button>
        </form>
        <ErrorBox error={upload.error} />
        {upload.data && <p className="ok">Uploaded {upload.data.originalName}.</p>}
      </Card>

      <Card title="Recent uploads">
        <table>
          <thead>
            <tr>
              <th>File</th>
              <th>Definition</th>
              <th>Side</th>
              <th className="num">Size (bytes)</th>
              <th>Uploaded</th>
            </tr>
          </thead>
          <tbody>
            {files.data?.map((f) => (
              <tr key={f.id}>
                <td title={`sha256 ${f.sha256}`}>{f.originalName}</td>
                <td>{definitionName(f.definitionId)}</td>
                <td>{f.side}</td>
                <td className="num">{formatNumber(f.sizeBytes)}</td>
                <td>{formatDate(f.uploadedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}
