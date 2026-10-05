import { defineModule } from '@reconark/module-sdk';
import { ReportsPage } from './ReportsPage';

export default defineModule({
  id: 'reports',
  title: 'Reports',
  routes: [{ path: 'requests', title: 'Requests', component: ReportsPage }],
});
