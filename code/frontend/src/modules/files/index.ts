import type { UiModule } from '../../shell/UiModule';
import { FilesPage } from './FilesPage';

export const filesModule: UiModule = {
  id: 'files',
  title: 'Files',
  home: '/files',
  routes: [{ path: '/files', Page: FilesPage }],
};
