/** A field derived from a plugin's JSON Schema (ConfigSpec.toJsonSchema on the server). */
export interface SchemaField {
  name: string;
  type: 'string' | 'integer' | 'number' | 'boolean' | 'array' | 'object';
  required: boolean;
  description: string;
  defaultValue: unknown;
  secretRef: boolean;
  format?: string;
}

interface JsonSchema {
  title?: string;
  properties?: Record<
    string,
    {
      type?: string;
      description?: string;
      default?: unknown;
      format?: string;
      'x-reconark-secret-ref'?: boolean;
    }
  >;
  required?: string[];
}

/** Turns a plugin config schema into form fields, so every new brick gets its configuration form for free. */
export function schemaToFields(schema: string | JsonSchema): SchemaField[] {
  const s: JsonSchema = typeof schema === 'string' ? (JSON.parse(schema) as JsonSchema) : schema;
  const required = new Set(s.required ?? []);
  return Object.entries(s.properties ?? {}).map(([name, p]) => ({
    name,
    type: (p.type ?? 'string') as SchemaField['type'],
    required: required.has(name),
    description: p.description ?? '',
    defaultValue: p.default,
    secretRef: p['x-reconark-secret-ref'] === true,
    ...(p.format ? { format: p.format } : {}),
  }));
}
