import eslint from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import eslintPluginVue from 'eslint-plugin-vue';

/**
 * Shared flat config for all MyCoRe Vue apps. An app re-exports it from its own eslint.config.mjs, so that the
 * plugin imports above resolve against the shared node_modules while the file patterns stay relative to the app.
 */
export default tseslint.config(
  {
    ignores: ['**/node_modules/**', '**/coverage/**', '**/target/**'],
  },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  ...eslintPluginVue.configs['flat/recommended'],
  {
    files: ['**/*.{ts,mts,vue}'],
    languageOptions: {
      ecmaVersion: 2025,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.vitest,
      },
      parserOptions: {
        parser: tseslint.parser,
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/attributes-order': 'off',
      'vue/html-self-closing': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
    },
  },
  {
    // declaration files are exactly the place where triple slash references belong, and the apps use one to pull
    // in the ambient types of the shared toolchain
    files: ['**/*.d.ts'],
    rules: {
      '@typescript-eslint/triple-slash-reference': 'off',
    },
  }
);
