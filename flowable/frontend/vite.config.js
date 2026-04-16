import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // During `npm run dev`, proxy API calls to the running Spring Boot server
    proxy: {
      '/loans': 'http://localhost:8080',
    },
  },
})
