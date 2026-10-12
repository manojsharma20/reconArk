/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development each /api path goes to the service that owns it (the same routing nginx does in docker-compose).
const services = {
  config: 'http://localhost:8081',
  ingestion: 'http://localhost:8082',
  report: 'http://localhost:8083',
  manager: 'http://localhost:8090',
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/definitions': services.config,
      '/api/plugins': services.config,
      '/api/files': services.ingestion,
      '/api/reports': services.report,
      '/api/runs': services.manager,
    },
  },
  build: { sourcemap: true },
  test: { environment: 'jsdom' },
});
