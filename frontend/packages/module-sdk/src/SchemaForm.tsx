import { useState } from 'react';
import { schemaToFields } from './schema';

interface Props {
  schema: string;
  onSubmit?: (values: Record<string, unknown>) => void;
  readOnly?: boolean;
}

/** Renders a configuration form from a plugin's JSON Schema. Secret fields accept reference names only. */
export function SchemaForm({ schema, onSubmit, readOnly = false }: Props) {
  const fields = schemaToFields(schema);
  const [values, setValues] = useState<Record<string, unknown>>(() =>
    Object.fromEntries(
      fields.filter((f) => f.defaultValue !== undefined).map((f) => [f.name, f.defaultValue]),
    ),
  );
  if (fields.length === 0) return <p className="muted">No configuration.</p>;
  return (
    <form
      className="schema-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit?.(values);
      }}
    >
      {fields.map((f) => (
        <label key={f.name} className="field">
          <span className="field-name">
            {f.name}
            {f.required && <abbr title="required"> *</abbr>}
            {f.secretRef && <span className="tag">secret ref</span>}
          </span>
          {f.type === 'boolean' ? (
            <input
              type="checkbox"
              disabled={readOnly}
              checked={values[f.name] === true}
              onChange={(e) => setValues({ ...values, [f.name]: e.target.checked })}
            />
          ) : (
            <input
              type={f.type === 'integer' || f.type === 'number' ? 'number' : 'text'}
              disabled={readOnly}
              placeholder={
                f.secretRef
                  ? 'e.g. prod/reconark/acquirer-a/sftp-key'
                  : f.format === 'duration'
                    ? 'PT30S'
                    : ''
              }
              value={String(values[f.name] ?? '')}
              onChange={(e) => setValues({ ...values, [f.name]: e.target.value })}
            />
          )}
          {f.description && <small className="muted">{f.description}</small>}
        </label>
      ))}
      {!readOnly && onSubmit && <button type="submit">Save</button>}
    </form>
  );
}
