import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // import.meta.dirname rather than __dirname: the latter is unsupported by Vite's native
    // config loader, which is slated to become the default.
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    port: 5173,
    // The backend already allows http://localhost:5173 via CorsConfig, so direct calls work.
    // The proxy exists so requests are same-origin in dev anyway - that keeps CORS out of the
    // picture entirely while debugging, and matches how this would sit behind one host in prod.
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
