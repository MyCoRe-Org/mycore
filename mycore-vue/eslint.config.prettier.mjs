import prettierRecommended from 'eslint-plugin-prettier/recommended';
import tseslint from 'typescript-eslint';

import sharedConfig from './eslint.config.mjs';

/**
 * Shared flat config plus prettier, for apps that enforce their formatting with prettier. The prettier options stay
 * with the app, prettier picks them up from the prettier.config.mjs next to the linted sources.
 */
export default tseslint.config(...sharedConfig, prettierRecommended);
