import { useQuery } from '@tanstack/react-query';
import { Suspense, useMemo } from 'react';
import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router';
import type { ReconArkUiModule, UiManifestEntry } from '@reconark/module-sdk';
import { loadManifest, resolveManifest } from './manifest';
import { moduleRegistry } from './registry';

function useModules(entries: UiManifestEntry[]) {
  return useQuery({
    queryKey: ['modules', entries.map((e) => e.id).join(',')],
    queryFn: async () => {
      const loaded = await Promise.all(
        entries.map(async (e) => ({ entry: e, module: (await moduleRegistry[e.id]!()).default })),
      );
      return loaded as { entry: UiManifestEntry; module: ReconArkUiModule }[];
    },
    enabled: entries.length > 0,
  });
}

export function App() {
  const manifest = useQuery({ queryKey: ['ui-manifest'], queryFn: loadManifest, staleTime: Infinity });
  const resolved = useMemo(
    () =>
      resolveManifest(
        manifest.data ?? { environment: '', modules: [] },
        new Set(Object.keys(moduleRegistry)),
      ),
    [manifest.data],
  );
  const modules = useModules(resolved.mounted);

  return (
    <BrowserRouter>
      <div className="layout">
        <aside className="sidebar">
          <div className="brand">
            recon<b>Ark</b>
          </div>
          <nav>
            {(modules.data ?? []).map(({ entry, module }) => (
              <div key={entry.id} className="nav-group">
                <div className="nav-title">{entry.title}</div>
                {module.routes.map((r) => (
                  <NavLink key={r.path} to={`${entry.path}/${r.path}`}>
                    {r.title}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>
          <footer className="muted">env: {manifest.data?.environment ?? '…'}</footer>
        </aside>
        <main className="content">
          {resolved.unknown.length > 0 && (
            <p className="warning">
              Manifest lists modules this build does not contain: {resolved.unknown.join(', ')}
            </p>
          )}
          <Suspense fallback={<p className="muted">Loading…</p>}>
            <Routes>
              {(modules.data ?? []).flatMap(({ entry, module }) =>
                module.routes.map((r) => {
                  const Page = r.component;
                  return (
                    <Route
                      key={`${entry.id}/${r.path}`}
                      path={`${entry.path}/${r.path}`}
                      element={<Page />}
                    />
                  );
                }),
              )}
              <Route path="*" element={<Home first={modules.data?.[0]} />} />
            </Routes>
          </Suspense>
        </main>
      </div>
    </BrowserRouter>
  );
}

function Home({ first }: { first: { entry: UiManifestEntry; module: ReconArkUiModule } | undefined }) {
  if (!first) return <p className="muted">No modules are enabled for you in this environment.</p>;
  return <Navigate to={`${first.entry.path}/${first.module.routes[0]!.path}`} replace />;
}
