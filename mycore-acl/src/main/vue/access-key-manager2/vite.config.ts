import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
// @ts-expect-error See https://github.com/gxmari007/vite-plugin-eslint/issues/79
import eslint from 'vite-plugin-eslint';
import path from 'path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), eslint()],
  resolve: {
    alias: {
      '@': `${path.resolve(__dirname, './src')}/`,
    },
  },
  build: {
    outDir: '../../../../target/classes/META-INF/resources/access-key-manager',
  },
  base: './',
});
