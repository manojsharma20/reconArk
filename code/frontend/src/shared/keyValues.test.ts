import { describe, expect, it } from 'vitest';
import { formatKeyValues, parseKeyValues, parseList } from './keyValues';

describe('key=value helpers', () => {
  it('parses lines and ignores noise', () => {
    expect(parseKeyValues('delimiter=;\n\n header = true \nnot a pair\n=x\ncolumns=a:1:2,b:3:4')).toEqual({
      delimiter: ';',
      header: 'true',
      columns: 'a:1:2,b:3:4',
    });
  });

  it('round-trips', () => {
    const values = { tolerance: '0.01', format: 'yyyy-MM-dd' };
    expect(parseKeyValues(formatKeyValues(values))).toEqual(values);
  });

  it('splits comma-separated lists', () => {
    expect(parseList(' reference , currency,, ')).toEqual(['reference', 'currency']);
  });
});
