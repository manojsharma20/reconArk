import { formatKeyValues, parseKeyValues, parseList } from '../../shared/keyValues';
import type { ReconDefinition, SourceSpec } from '../../shared/types';

// The editor works on plain text fields; these functions convert between the form and the API's ReconDefinition.

export interface SourceDraft {
  name: string;
  format: string;
  formatOptions: string; // key=value lines
  fieldMapping: string; // field=column lines
}

export interface RuleDraft {
  field: string;
  comparator: string;
  options: string; // key=value lines
}

export interface DefinitionDraft {
  name: string;
  left: SourceDraft;
  right: SourceDraft;
  matchFields: string; // comma-separated
  rules: RuleDraft[];
}

export const emptyDraft = (): DefinitionDraft => ({
  name: '',
  left: { name: 'Bank statement', format: 'csv', formatOptions: '', fieldMapping: '' },
  right: { name: 'Ledger', format: 'csv', formatOptions: '', fieldMapping: '' },
  matchFields: 'reference',
  rules: [{ field: 'amount', comparator: 'number', options: 'tolerance=0.01' }],
});

const toSource = (draft: SourceDraft): SourceSpec => ({
  name: draft.name.trim(),
  format: draft.format,
  formatOptions: parseKeyValues(draft.formatOptions),
  fieldMapping: parseKeyValues(draft.fieldMapping),
});

const toSourceDraft = (source: SourceSpec): SourceDraft => ({
  name: source.name,
  format: source.format,
  formatOptions: formatKeyValues(source.formatOptions),
  fieldMapping: formatKeyValues(source.fieldMapping),
});

export function toDefinition(draft: DefinitionDraft, id: string | null): ReconDefinition {
  return {
    id,
    name: draft.name.trim(),
    left: toSource(draft.left),
    right: toSource(draft.right),
    matchFields: parseList(draft.matchFields),
    compareRules: draft.rules
      .filter((rule) => rule.field.trim())
      .map((rule) => ({
        field: rule.field.trim(),
        comparator: rule.comparator,
        options: parseKeyValues(rule.options),
      })),
  };
}

export function toDraft(definition: ReconDefinition): DefinitionDraft {
  return {
    name: definition.name,
    left: toSourceDraft(definition.left),
    right: toSourceDraft(definition.right),
    matchFields: definition.matchFields.join(', '),
    rules: definition.compareRules.map((rule) => ({
      field: rule.field,
      comparator: rule.comparator,
      options: formatKeyValues(rule.options),
    })),
  };
}
