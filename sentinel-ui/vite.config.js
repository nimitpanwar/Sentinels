import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Allow tunnel-hosted requests (ngrok/cloudflared) to reach the dev server.
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      '.ngrok-free.app',
      '.ngrok.app',
      '.trycloudflare.com',
      "dropkick-recolor-deceiving.ngrok-free.dev",
    ],
    proxy: {
      // All /api/* calls from the React dev server are forwarded to Spring Boot.
      // This means no CORS issues during development and no need to hard-code
      // http://localhost:8080 anywhere in the frontend code.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
