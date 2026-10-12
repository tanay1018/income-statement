import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // The backend runs on :8080. Forwarding the request avoids CORS in development.
    proxy: {
      '/income-statement': 'http://localhost:8080',
    },
  },
})
