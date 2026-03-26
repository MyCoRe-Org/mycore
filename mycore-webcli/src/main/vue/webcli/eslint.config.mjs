import eslint from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import eslintPluginVue from 'eslint-plugin-vue';

export default tseslint.config(
  {
    ignores: ['node_modules/**', 'coverage/**', '../../../../target/**'],
  },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  ...eslintPluginVue.configs['flat/recommended'],
  {
    files: ['src/**/*.{ts,vue}', 'tests/**/*.ts', '*.config.ts'],
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
  }
);
