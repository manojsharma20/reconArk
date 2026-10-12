import { describe, expect, it } from 'vitest';
import { emptyDraft, toDefinition, toDraft } from './definitionForm';

describe('definition form', () => {
  it('turns the text fields into a definition', () => {
    const draft = emptyDraft();
    draft.name = ' Bank vs Ledger ';
    draft.left.fieldMapping = 'reference=REF\namount=AMT';
    draft.right.formatOptions = 'delimiter=;';
    draft.matchFields = 'reference, currency';
    draft.rules.push({ field: '  ', comparator: 'exact', options: '' }); // empty rule is dropped

    const definition = toDefinition(draft, null);
    expect(definition.name).toBe('Bank vs Ledger');
    expect(definition.left.fieldMapping).toEqual({ reference: 'REF', amount: 'AMT' });
    expect(definition.right.formatOptions).toEqual({ delimiter: ';' });
    expect(definition.matchFields).toEqual(['reference', 'currency']);
    expect(definition.compareRules).toEqual([
      { field: 'amount', comparator: 'number', options: { tolerance: '0.01' } },
    ]);
  });

  it('round-trips through the draft', () => {
    const definition = toDefinition({ ...emptyDraft(), name: 'X' }, 'id-1');
    expect(toDefinition(toDraft(definition), 'id-1')).toEqual(definition);
  });
});
