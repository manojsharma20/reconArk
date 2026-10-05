import type { ReconArkUiModule } from '@reconark/module-sdk';

/**
 * Every UI brick this shell build can load, by id. The UI manifest decides which of them are mounted, where, and for
 * whom. Adding a module = add a workspace package + one line here + a manifest entry. When teams need independent
 * deployment, entries become Module Federation remotes with the same contract (ADR-0032).
 */
export const moduleRegistry: Record<string, () => Promise<{ default: ReconArkUiModule }>> = {
  admin: () => import('@reconark/module-admin'),
  recon: () => import('@reconark/module-recon'),
  reports: () => import('@reconark/module-reports'),
  operations: () => import('@reconark/module-operations'),
};
