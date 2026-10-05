import { useQuery } from '@tanstack/react-query';
import { api, Async } from '@reconark/module-sdk';

interface Entry {
  id: string;
  version: string;
  state: string;
  trustTier: string;
  health: { state: string; detail: string };
}

/** Live plugin inventory and health from operations-api (internal listener, OPERATOR role). */
export function PluginHealthPage() {
  const q = useQuery({
    queryKey: ['ops', 'plugins'],
    queryFn: () => api<Entry[]>('/api/ops/v1/plugins'),
    refetchInterval: 15_000,
  });
  return (
    <section>
      <h1>Plugin health</h1>
      <Async query={q}>
        {(list) => (
          <table>
            <thead>
              <tr>
                <th>Plugin</th>
                <th>Version</th>
                <th>Tier</th>
                <th>State</th>
                <th>Health</th>
              </tr>
            </thead>
            <tbody>
              {list.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.version}</td>
                  <td>{p.trustTier}</td>
                  <td>{p.state}</td>
                  <td>
                    <span className={`status status-${p.health.state.toLowerCase()}`}>{p.health.state}</span>{' '}
                    {p.health.detail}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
    </section>
  );
}
