import type { ComponentType } from 'react';

/** One page a module contributes. `path` is relative to the module's mount path from the UI manifest. */
export interface ModuleRoute {
  path: string;
  title: string;
  component: ComponentType;
}

/**
 * A UI brick. The shell mounts it at the path the UI manifest gives it, shows its nav items and enforces its roles.
 * A module never imports another module; shared code lives in this SDK.
 */
export interface ReconArkUiModule {
  id: string;
  title: string;
  routes: ModuleRoute[];
}

/** Identity helper that gives modules type-checking and a single place to evolve the contract. */
export function defineModule(module: ReconArkUiModule): ReconArkUiModule {
  if (!/^[a-z][a-z0-9-]*$/.test(module.id)) {
    throw new Error(`Module id must be kebab-case: ${module.id}`);
  }
  if (module.routes.length === 0) {
    throw new Error(`Module ${module.id} contributes no routes`);
  }
  return module;
}

/** An entry of the UI manifest served by web-bff (`GET /bff/ui-manifest`). */
export interface UiManifestEntry {
  id: string;
  title: string;
  path: string;
  roles: string[];
  enabled: boolean;
  order: number;
  flag?: string | null;
}

export interface UiManifest {
  environment: string;
  modules: UiManifestEntry[];
}
