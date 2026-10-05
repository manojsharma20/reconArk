import { defineModule } from '@reconark/module-sdk';
import { PluginHealthPage } from './PluginHealthPage';

export default defineModule({
  id: 'operations',
  title: 'Operations',
  routes: [{ path: 'plugins', title: 'Plugin health', component: PluginHealthPage }],
});
