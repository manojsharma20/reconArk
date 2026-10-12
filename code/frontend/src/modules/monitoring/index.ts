import type { UiModule } from '../../shell/UiModule';
import { MonitoringPage } from './MonitoringPage';

export const monitoringModule: UiModule = {
  id: 'monitoring',
  title: 'Monitoring',
  home: '/monitoring',
  routes: [{ path: '/monitoring', Page: MonitoringPage }],
};
