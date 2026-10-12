import type { UiModule } from '../../shell/UiModule';
import { RunDetailPage } from './RunDetailPage';
import { RunsPage } from './RunsPage';

export const runsModule: UiModule = {
  id: 'runs',
  title: 'Runs',
  home: '/runs',
  routes: [
    { path: '/runs', Page: RunsPage },
    { path: '/runs/:runId', Page: RunDetailPage },
  ],
};
