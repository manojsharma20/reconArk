import type { UiModule } from './UiModule';
import { definitionsModule } from '../modules/definitions';
import { filesModule } from '../modules/files';
import { runsModule } from '../modules/runs';
import { reportsModule } from '../modules/reports';
import { monitoringModule } from '../modules/monitoring';

/** Every module this build contains. Which of them are shown is decided by public/config.json. */
export const allModules: UiModule[] = [
  definitionsModule,
  filesModule,
  runsModule,
  reportsModule,
  monitoringModule,
];

/** The modules switched on in the config, in the config's order. Unknown ids are reported, not fatal. */
export function enabledModules(
  available: UiModule[],
  enabledIds: string[],
): { modules: UiModule[]; unknown: string[] } {
  const byId = new Map(available.map((m) => [m.id, m]));
  return {
    modules: enabledIds.flatMap((id) => byId.get(id) ?? []),
    unknown: enabledIds.filter((id) => !byId.has(id)),
  };
}
