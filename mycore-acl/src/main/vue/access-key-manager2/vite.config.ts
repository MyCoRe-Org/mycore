import vue from '@vitejs/plugin-vue';
import eslint from 'vite-plugin-eslint';
import path from 'path';
import externalGlobals from 'rollup-plugin-external-globals';

export default {
  plugins: [
    vue(),
    eslint(),
    externalGlobals({
      bootstrap: 'window.bootstrap',
    }),
  ],
  resolve: {
    alias: {
      '@': `${path.resolve(__dirname, './src')}/`,
    },
  },
  build: {
    outDir: '../../../../target/classes/META-INF/resources/access-key-manager',
  },
  base: './',
};
