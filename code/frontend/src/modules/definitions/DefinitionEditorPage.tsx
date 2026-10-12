import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox } from '../../shared/components';
import type { PluginInfo, ReconDefinition } from '../../shared/types';
import { emptyDraft, toDefinition, toDraft, type DefinitionDraft, type SourceDraft } from './definitionForm';

/** Create (/definitions/new) or edit (/definitions/:id) a recon definition. */
export function DefinitionEditorPage() {
  const { id } = useParams();
  const isNew = !id || id === 'new';
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<DefinitionDraft>(emptyDraft);

  const plugins = useQuery({ queryKey: ['plugins'], queryFn: () => api.get<PluginInfo[]>('/api/plugins') });
  const existing = useQuery({
    queryKey: ['definition', id],
    queryFn: () => api.get<ReconDefinition>(`/api/definitions/${id}`),
    enabled: !isNew,
  });
  useEffect(() => {
    if (existing.data) setDraft(toDraft(existing.data));
  }, [existing.data]);

  const save = useMutation({
    mutationFn: (definition: ReconDefinition) =>
      isNew
        ? api.post<ReconDefinition>('/api/definitions', definition)
        : api.put<ReconDefinition>(`/api/definitions/${id}`, definition),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['definitions'] });
      void navigate('/definitions');
    },
  });
  const remove = useMutation({
    mutationFn: () => api.delete(`/api/definitions/${id}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['definitions'] });
      void navigate('/definitions');
    },
  });

  const formats = plugins.data?.filter((p) => p.kind === 'format') ?? [];
  const comparators = plugins.data?.filter((p) => p.kind === 'comparator') ?? [];
  const update = (change: Partial<DefinitionDraft>) => setDraft((d) => ({ ...d, ...change }));

  return (
    <>
      <header className="page-header">
        <h1>{isNew ? 'New definition' : `Edit: ${draft.name}`}</h1>
      </header>
      <ErrorBox error={existing.error ?? plugins.error} />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save.mutate(toDefinition(draft, isNew ? null : (id ?? null)));
        }}
      >
        <Card title="General">
          <label>
            Name
            <input value={draft.name} onChange={(e) => update({ name: e.target.value })} required />
          </label>
          <label>
            Match fields{' '}
            <span className="muted">(comma-separated; together they identify a transaction)</span>
            <input
              value={draft.matchFields}
              onChange={(e) => update({ matchFields: e.target.value })}
              required
            />
          </label>
        </Card>

        <div className="two-columns">
          <SourceEditor
            title="Left source"
            source={draft.left}
            formats={formats}
            onChange={(left) => update({ left })}
          />
          <SourceEditor
            title="Right source"
            source={draft.right}
            formats={formats}
            onChange={(right) => update({ right })}
          />
        </div>

        <Card title="Compare rules">
          <p className="muted">
            After two records are paired by their match fields, these fields are compared.
          </p>
          {draft.rules.map((rule, index) => (
            <div className="rule" key={index}>
              <input
                placeholder="field, e.g. amount"
                value={rule.field}
                onChange={(e) =>
                  update({ rules: replaceAt(draft.rules, index, { ...rule, field: e.target.value }) })
                }
              />
              <select
                value={rule.comparator}
                onChange={(e) =>
                  update({ rules: replaceAt(draft.rules, index, { ...rule, comparator: e.target.value }) })
                }
              >
                {comparators.map((c) => (
                  <option key={c.id} value={c.id} title={c.description}>
                    {c.id}
                  </option>
                ))}
              </select>
              <textarea
                placeholder="options, one per line, e.g. tolerance=0.01"
                rows={2}
                value={rule.options}
                onChange={(e) =>
                  update({ rules: replaceAt(draft.rules, index, { ...rule, options: e.target.value }) })
                }
              />
              <button
                type="button"
                className="link"
                onClick={() => update({ rules: draft.rules.filter((_, i) => i !== index) })}
              >
                Remove
              </button>
            </div>
          ))}
          <button
            type="button"
            onClick={() =>
              update({ rules: [...draft.rules, { field: '', comparator: 'exact', options: '' }] })
            }
          >
            Add rule
          </button>
          <ul className="muted small">
            {comparators.map((c) => (
              <li key={c.id}>
                <b>{c.id}</b>: {c.description}
              </li>
            ))}
          </ul>
        </Card>

        <ErrorBox error={save.error ?? remove.error} />
        <div className="actions">
          <button type="submit" className="primary" disabled={save.isPending}>
            Save
          </button>
          {!isNew && (
            <button
              type="button"
              className="danger"
              onClick={() => remove.mutate()}
              disabled={remove.isPending}
            >
              Delete
            </button>
          )}
        </div>
      </form>
    </>
  );
}

function SourceEditor(props: {
  title: string;
  source: SourceDraft;
  formats: PluginInfo[];
  onChange: (source: SourceDraft) => void;
}) {
  const { source, onChange } = props;
  const description = props.formats.find((f) => f.id === source.format)?.description;
  return (
    <Card title={props.title}>
      <label>
        Name
        <input value={source.name} onChange={(e) => onChange({ ...source, name: e.target.value })} required />
      </label>
      <label>
        File format
        <select value={source.format} onChange={(e) => onChange({ ...source, format: e.target.value })}>
          {props.formats.map((f) => (
            <option key={f.id} value={f.id}>
              {f.id}
            </option>
          ))}
        </select>
      </label>
      {description && <p className="muted small">{description}</p>}
      <label>
        Format options <span className="muted">(one per line, e.g. delimiter=;)</span>
        <textarea
          rows={3}
          value={source.formatOptions}
          onChange={(e) => onChange({ ...source, formatOptions: e.target.value })}
        />
      </label>
      <label>
        Field mapping{' '}
        <span className="muted">(field=column, e.g. amount=AMT; unmapped columns keep their name)</span>
        <textarea
          rows={3}
          value={source.fieldMapping}
          onChange={(e) => onChange({ ...source, fieldMapping: e.target.value })}
        />
      </label>
    </Card>
  );
}

function replaceAt<T>(items: T[], index: number, item: T): T[] {
  return items.map((existing, i) => (i === index ? item : existing));
}
