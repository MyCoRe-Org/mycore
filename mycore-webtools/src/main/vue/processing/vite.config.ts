import { fileURLToPath } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    outDir: "../../../../target/classes/META-INF/resources/modules/webtools/processing",
    rollupOptions: {
      external: ['bootstrap']
    }
  },
  base: "./"
})
