import type { ComponentType } from 'react';

/**
 * A UI module is one feature area of the screen (a "Lego brick" of the frontend): it brings its own pages and one
 * menu entry. To add a module: create a folder under src/modules, export a UiModule from its index.ts, add it to
 * src/shell/modules.ts, and switch it on in public/config.json.
 */
export interface UiModule {
  /** Matches the id in public/config.json. */
  id: string;
  /** Menu label. */
  title: string;
  /** Where the menu entry points to. */
  home: string;
  /** Every page of the module, e.g. { path: '/runs/:runId', Page: RunDetailPage }. */
  routes: { path: string; Page: ComponentType }[];
}

/** Runtime settings in public/config.json: change them without rebuilding the frontend. */
export interface UiConfig {
  environment: string;
  modules: string[];
  links: Record<string, string>;
}
