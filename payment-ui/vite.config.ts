/**
 * Configurazione Vite: dev server sulla porta 3000 con proxy BFF verso payment-service.
 * In sviluppo inietta X-Api-Key dalla variabile PAYMENT_API_KEY (mai esposta al browser).
 */
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const paymentApiKey = env.PAYMENT_API_KEY ?? 'pgw-demo-key-32chars-minimum!!';

  return {
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('react-dom') || id.includes('/react/')) return 'react';
              if (id.includes('@fontsource')) return 'fonts';
            }
            if (id.includes('/src/components/TestCollection') || id.includes('/src/testCollection')) {
              return 'collection';
            }
          },
        },
      },
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.setHeader('X-Api-Key', paymentApiKey);
            });
          },
        },
      },
    },
  };
});
