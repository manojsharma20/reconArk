import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { api } from '../../shared/api';
import { Card, ErrorBox } from '../../shared/components';
import type { ReconDefinition } from '../../shared/types';

/** Lists recon definitions. */
export function DefinitionsPage() {
  const definitions = useQuery({
    queryKey: ['definitions'],
    queryFn: () => api.get<ReconDefinition[]>('/api/definitions'),
  });

  return (
    <>
      <header className="page-header">
        <h1>Recon definitions</h1>
        <Link className="button" to="/definitions/new">
          New definition
        </Link>
      </header>
      <p className="muted">
        A definition says how to read two sources, which fields identify the same transaction, and how to
        compare the rest.
      </p>
      <ErrorBox error={definitions.error} />
      <Card title="All definitions">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Left</th>
              <th>Right</th>
              <th>Match on</th>
              <th>Compare</th>
            </tr>
          </thead>
          <tbody>
            {definitions.data?.map((d) => (
              <tr key={d.id}>
                <td>
                  <Link to={`/definitions/${d.id}`}>{d.name}</Link>
                </td>
                <td>
                  {d.left.name} <span className="muted">({d.left.format})</span>
                </td>
                <td>
                  {d.right.name} <span className="muted">({d.right.format})</span>
                </td>
                <td>{d.matchFields.join(', ')}</td>
                <td>{d.compareRules.map((r) => `${r.field} (${r.comparator})`).join(', ')}</td>
              </tr>
            ))}
            {definitions.data?.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No definitions yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </>
  );
}
