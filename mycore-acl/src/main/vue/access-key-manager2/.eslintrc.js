module.exports = {
  root: true,
  env: {
    node: true,
    'vue/setup-compiler-macros': true,
  },
  extends: [
    'plugin:vue/vue3-essential',
    '@vue/airbnb',
    '@vue/typescript/recommended',
  ],
  parserOptions: {
    ecmaVersion: 2020,
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'import/prefer-default-export': 'off',
    'vue/multi-word-component-names': 'off',
    "no-shadow": "off",
    "no-underscore-dangle": "off",
    "@typescript-eslint/no-shadow": "error",
    "@typescript-eslint/no-namespace": "off",
    "max-len": ["error", 120],
  },
};
