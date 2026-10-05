import { defineModule } from '@reconark/module-sdk';
import { PluginCatalogPage } from './PluginCatalogPage';
import { ProviderDraftPage } from './ProviderDraftPage';

export default defineModule({
  id: 'admin',
  title: 'Onboarding',
  routes: [
    { path: 'providers', title: 'Providers', component: ProviderDraftPage },
    { path: 'plugins', title: 'Plugin catalogue', component: PluginCatalogPage },
  ],
});
