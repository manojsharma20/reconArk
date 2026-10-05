import { defineModule } from '@reconark/module-sdk';
import { PreviewPage } from './PreviewPage';

export default defineModule({
  id: 'recon',
  title: 'Recon workbench',
  routes: [{ path: 'preview', title: 'Rule-set preview', component: PreviewPage }],
});
