import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router';
import { allModules, enabledModules } from './shell/modules';
import { useConfig } from './shell/useConfig';

/** The shell: a menu with one entry per enabled module, and the pages of those modules. */
export function App() {
  const config = useConfig();
  if (config.isPending) return <p className="muted pad">Loading…</p>;
  if (config.isError) return <p className="error pad">Could not load config.json: {config.error.message}</p>;

  const { modules, unknown } = enabledModules(allModules, config.data.modules);

  return (
    <BrowserRouter>
      <div className="layout">
        <aside className="sidebar">
          <div className="brand">
            recon<b>Ark</b>
          </div>
          <nav>
            {modules.map((m) => (
              <NavLink key={m.id} to={m.home}>
                {m.title}
              </NavLink>
            ))}
          </nav>
          <footer className="muted">env: {config.data.environment}</footer>
        </aside>
        <main className="content">
          {unknown.length > 0 && (
            <p className="error">config.json lists unknown modules: {unknown.join(', ')}</p>
          )}
          <Routes>
            {modules.flatMap((m) =>
              m.routes.map(({ path, Page }) => <Route key={path} path={path} element={<Page />} />),
            )}
            <Route
              path="*"
              element={modules[0] ? <Navigate to={modules[0].home} replace /> : <p>No modules enabled.</p>}
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
