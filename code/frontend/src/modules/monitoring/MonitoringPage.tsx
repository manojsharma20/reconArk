import { Card } from '../../shared/components';
import { useConfig } from '../../shell/useConfig';

const tools = [
  {
    key: 'grafana',
    name: 'Grafana',
    what: 'Dashboards: runs, jobs, records per second, worker health, JVM and database pool.',
  },
  {
    key: 'prometheus',
    name: 'Prometheus',
    what: 'Raw metrics and alerts. Every service exposes /actuator/prometheus.',
  },
  {
    key: 'kafkaUi',
    name: 'Kafka UI',
    what: 'Topics, messages and consumer lag (are the workers keeping up?).',
  },
];

/** Links to the monitoring tools (addresses come from public/config.json). */
export function MonitoringPage() {
  const config = useConfig();
  const links = config.data?.links ?? {};
  return (
    <>
      <header className="page-header">
        <h1>Monitoring</h1>
      </header>
      <div className="two-columns">
        {tools.map((t) => (
          <Card key={t.key} title={t.name}>
            <p>{t.what}</p>
            {links[t.key] ? (
              <a className="button" href={links[t.key]} target="_blank" rel="noreferrer">
                Open {t.name}
              </a>
            ) : (
              <p className="muted">Not configured in config.json.</p>
            )}
          </Card>
        ))}
      </div>
    </>
  );
}
