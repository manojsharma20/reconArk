import type { UiModule } from '../../shell/UiModule';
import { DefinitionEditorPage } from './DefinitionEditorPage';
import { DefinitionsPage } from './DefinitionsPage';

export const definitionsModule: UiModule = {
  id: 'definitions',
  title: 'Definitions',
  home: '/definitions',
  routes: [
    { path: '/definitions', Page: DefinitionsPage },
    { path: '/definitions/:id', Page: DefinitionEditorPage },
  ],
};
