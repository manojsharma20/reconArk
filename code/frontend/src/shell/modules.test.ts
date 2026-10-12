import { describe, expect, it } from 'vitest';
import type { UiModule } from './UiModule';
import { enabledModules } from './modules';

const module = (id: string): UiModule => ({ id, title: id, home: `/${id}`, routes: [] });

describe('enabledModules', () => {
  it('keeps the configured order and skips modules that are switched off', () => {
    const result = enabledModules([module('a'), module('b'), module('c')], ['c', 'a']);
    expect(result.modules.map((m) => m.id)).toEqual(['c', 'a']);
    expect(result.unknown).toEqual([]);
  });

  it('reports ids this build does not contain', () => {
    expect(enabledModules([module('a')], ['a', 'x']).unknown).toEqual(['x']);
  });
});
