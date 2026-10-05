import { describe, expect, it } from 'vitest';
import { resolveManifest } from './manifest';

const entry = (id: string, order: number, enabled = true) => ({
  id,
  title: id,
  path: `/${id}`,
  roles: [],
  enabled,
  order,
});

describe('resolveManifest', () => {
  it('mounts enabled, known modules in manifest order and reports unknown ones', () => {
    const { mounted, unknown } = resolveManifest(
      {
        environment: 'test',
        modules: [entry('reports', 30), entry('admin', 10), entry('beta', 5), entry('ops', 1, false)],
      },
      new Set(['admin', 'reports', 'ops']),
    );
    expect(mounted.map((m) => m.id)).toEqual(['admin', 'reports']);
    expect(unknown).toEqual(['beta']);
  });
});
