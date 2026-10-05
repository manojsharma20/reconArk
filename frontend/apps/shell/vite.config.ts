import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the shell talks to web-bff on :8080, exactly like production (same-origin /bff and /api).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/bff': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
  build: { sourcemap: true },
});
