/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

import path from 'node:path';
import { fileURLToPath } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { mergeConfig, type Plugin, type UserConfig } from 'vite';

/**
 * Importer used to look up package imports. It has to be an existing file inside the mycore-vue module: node
 * resolution starts in the shared node_modules, and the vite resolver falls back to the app directory for an
 * importer that does not exist on disk, which would defeat the whole lookup for packages without an exports map.
 */
const TOOLCHAIN_ANCHOR = fileURLToPath(import.meta.url);

/**
 * Directory of this file, which is the root of the mycore-vue module and therefore the place where the shared
 * node_modules lives.
 */
const TOOLCHAIN_DIR = path.dirname(TOOLCHAIN_ANCHOR);

/** Path of the web resource root inside the maven output directory of the owning module. */
const WEB_RESOURCE_DIR = 'target/classes/META-INF/resources';

/** Number of directory levels between an app directory and the maven module it belongs to. */
const APP_DIR_DEPTH = '../../../..';

/**
 * Tells whether a module specifier addresses a package instead of a file. Relative and absolute paths, rollup
 * internal ids, package internal imports and everything with a protocol like <code>node:</code> are handled by the
 * default resolution.
 *
 * @param source the module specifier
 */
function isPackageImport(source: string): boolean {
  if (source.startsWith('.') || source.startsWith('/') || source.startsWith('\0') || source.startsWith('#')) {
    return false;
  }
  if (path.isAbsolute(source)) {
    return false;
  }
  return !/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(source);
}

/**
 * Tells whether a file already lives inside the mycore-vue module, which includes everything below the shared
 * node_modules. Default resolution is correct for those, and redirecting them would flatten a nested dependency
 * onto the hoisted one, so a package that pins its own version of a transitive dependency would silently get the
 * hoisted version instead.
 *
 * @param importer the absolute path of the importing file
 */
function isToolchainFile(importer: string | undefined): boolean {
  if (importer === undefined) {
    return false;
  }
  const relativeToToolchain = path.relative(TOOLCHAIN_DIR, importer);
  return relativeToToolchain !== '' && !relativeToToolchain.startsWith('..')
    && !path.isAbsolute(relativeToToolchain);
}

/**
 * Resolves package imports against the shared node_modules of mycore-vue. The apps keep their sources in their own
 * maven module, where node resolution would never find the shared toolchain, because it only walks up the directory
 * tree of the importing file.
 */
function sharedToolchainResolver(): Plugin {
  return {
    name: 'mycore:shared-toolchain-resolver',
    enforce: 'pre',
    async resolveId(source, importer, options) {
      if (isToolchainFile(importer) || !isPackageImport(source)) {
        return null;
      }
      return await this.resolve(source, TOOLCHAIN_ANCHOR, { ...options, skipSelf: true });
    },
  };
}

/** Module specifier of the bootstrap package. */
const BOOTSTRAP_MODULE = 'bootstrap';

/** Virtual module that reads the bootstrap components off the global object. */
const BOOTSTRAP_GLOBAL_MODULE = '\0mycore:bootstrap-global';

/** The complete export surface of bootstrap 5. Named exports have to be known statically to the bundler. */
const BOOTSTRAP_EXPORTS = ['Alert', 'Button', 'Carousel', 'Collapse', 'Dropdown', 'Modal', 'Offcanvas', 'Popover',
  'ScrollSpy', 'Tab', 'Toast', 'Tooltip'];

/**
 * Resolves imports of the bootstrap package to the <code>window.bootstrap</code> global, which the surrounding
 * MyCoRe page provides, so that bootstrap is not shipped a second time inside an app bundle. Imports of individual
 * files, like the bootstrap stylesheet, are left alone.
 *
 * This replaces rollup-plugin-external-globals, which does not work with the rolldown bundler of vite 8.
 *
 * Only applied to builds. The dev server has no MyCoRe page around the app and therefore no global to read, so
 * <code>yarn dev:&lt;app&gt;</code> uses the bootstrap package from the shared node_modules instead.
 */
export function bootstrapFromWindow(): Plugin {
  return {
    name: 'mycore:bootstrap-from-window',
    enforce: 'pre',
    apply: 'build',
    resolveId(source) {
      return source === BOOTSTRAP_MODULE ? BOOTSTRAP_GLOBAL_MODULE : null;
    },
    load(id) {
      if (id !== BOOTSTRAP_GLOBAL_MODULE) {
        return null;
      }
      return [
        'const bootstrap = window.bootstrap;',
        `export const { ${BOOTSTRAP_EXPORTS.join(', ')} } = bootstrap;`,
        'export default bootstrap;',
      ].join('\n');
    },
  };
}

export interface MCRVueAppOptions {
  /**
   * Location of the app's own vite config, always passed as `import.meta.url`. Everything else is derived from it,
   * so a build does not depend on the current working directory.
   */
  configUrl: string;

  /**
   * Unique name of the app. Used as the vite cache directory below the shared node_modules, so that concurrent app
   * builds do not write into the same cache.
   */
  name: string;

  /**
   * Path of the app below the web resource root, e.g. `modules/webtools/texteditor`. It has to match the
   * url-pattern the app is bound to in the web-fragment.xml of the owning module.
   */
  resourcePath: string;

  /**
   * Directory of the maven module that owns the app, relative to the app directory. Only needed for apps that do not
   * live in the usual `src/main/vue/<app>` location.
   */
  moduleDir?: string;
}

/**
 * Builds the vite configuration for a MyCoRe Vue app. The app keeps its sources and its build output in its own
 * maven module, only the toolchain is shared. The returned configuration can be refined with app specific settings,
 * which are merged on top of the shared defaults.
 *
 * @param options the app description
 * @param overrides app specific configuration merged on top of the shared defaults
 */
export function defineMCRVueApp(options: MCRVueAppOptions, overrides: UserConfig = {}): UserConfig {
  const appDir = path.dirname(fileURLToPath(options.configUrl));
  const moduleDir = path.resolve(appDir, options.moduleDir ?? APP_DIR_DEPTH);

  const sharedConfig: UserConfig = {
    root: appDir,
    base: './',
    cacheDir: path.resolve(TOOLCHAIN_DIR, 'node_modules/.vite', options.name),
    plugins: [sharedToolchainResolver(), vue()],
    resolve: {
      alias: {
        '@': path.resolve(appDir, 'src'),
      },
    },
    build: {
      outDir: path.resolve(moduleDir, WEB_RESOURCE_DIR, options.resourcePath),
      // the output directory is outside of the app root, so vite only empties it when told to
      emptyOutDir: true,
    },
  };

  return mergeConfig(sharedConfig, overrides);
}
