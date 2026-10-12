import type { UiModule } from '../../shell/UiModule';
import { ReportPage } from './ReportPage';
import { ReportsPage } from './ReportsPage';

export const reportsModule: UiModule = {
  id: 'reports',
  title: 'Reports',
  home: '/reports',
  routes: [
    { path: '/reports', Page: ReportsPage },
    { path: '/reports/:runId', Page: ReportPage },
  ],
};
