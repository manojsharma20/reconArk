import { describe, expect, it } from 'vitest';
import { schemaToFields } from './schema';

describe('schemaToFields', () => {
  it('maps a kernel ConfigSpec schema to form fields', () => {
    const schema =
      '{"title":"Kafka bus","type":"object","additionalProperties":false,"properties":{' +
      '"bootstrap-servers":{"type":"string","description":"host:port list"},' +
      '"max-deliveries":{"type":"integer","default":5},' +
      '"key-ref":{"type":"string","x-reconark-secret-ref":true},' +
      '"timeout":{"type":"string","format":"duration"}},"required":["bootstrap-servers","key-ref"]}';
    const fields = schemaToFields(schema);
    expect(fields.map((f) => f.name)).toEqual(['bootstrap-servers', 'max-deliveries', 'key-ref', 'timeout']);
    expect(fields[0]).toMatchObject({ required: true, type: 'string' });
    expect(fields[1]).toMatchObject({ required: false, defaultValue: 5, type: 'integer' });
    expect(fields[2]?.secretRef).toBe(true);
    expect(fields[3]?.format).toBe('duration');
  });

  it('handles schemas without properties', () => {
    expect(schemaToFields({})).toEqual([]);
  });
});
