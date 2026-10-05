import { useQuery } from '@tanstack/react-query';
import { api, Async, SchemaForm } from '@reconark/module-sdk';

interface PluginEntry {
  id: string;
  version: string;
  name: string;
  trustTier: string;
  state: string;
  contributions: Record<string, string[]>;
  health: { state: string; detail: string };
  remote: boolean;
  configSchema: string;
}

/** The Lego catalogue: every installed brick, which sockets it fills, its health and its configuration form. */
export function PluginCatalogPage() {
  const plugins = useQuery({
    queryKey: ['admin', 'plugins'],
    queryFn: () => api<PluginEntry[]>('/api/admin/v1/plugins'),
  });
  return (
    <section>
      <h1>Plugin catalogue</h1>
      <p className="muted">
        Bricks installed in admin-api. Which ones are active is set by the environment composition; business
        configuration can only select active keys.
      </p>
      <Async query={plugins}>
        {(list) => (
          <div className="cards">
            {list.map((p) => (
              <article key={p.id} className={`card state-${p.state.toLowerCase()}`}>
                <header>
                  <h2>{p.name}</h2>
                  <span className="tag">
                    {p.id}@{p.version}
                  </span>
                  <span className={`tag tier-${p.trustTier.toLowerCase()}`}>{p.trustTier}</span>
                  <span className="tag">{p.state}</span>
                </header>
                <dl>
                  {Object.entries(p.contributions).map(([point, keys]) => (
                    <div key={point}>
                      <dt>{point}</dt>
                      <dd>{keys.join(', ')}</dd>
                    </div>
                  ))}
                </dl>
                <details>
                  <summary>Configuration</summary>
                  <SchemaForm schema={p.configSchema} readOnly />
                </details>
              </article>
            ))}
          </div>
        )}
      </Async>
    </section>
  );
}
