import eslint from '@eslint/js';
import globals from 'globals';
import eslintPluginVue from 'eslint-plugin-vue';
import typescriptEslint from 'typescript-eslint';
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';

export default typescriptEslint.config({
  languageOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    globals: globals.browser,
    parserOptions: {
      parser: typescriptEslint.parser,
    },
  },
  extends: [
    eslint.configs.recommended,
    ...typescriptEslint.configs.recommended,
    ...eslintPluginVue.configs['flat/recommended'],
    eslintPluginPrettierRecommended,
  ],
  files: ['**/*.{ts,vue}'],
});