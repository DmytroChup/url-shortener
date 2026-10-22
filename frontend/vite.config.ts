import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '^/[a-zA-Z0-9]{7}(\\?.*)?$': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})