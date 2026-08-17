# MyCoRe Vue

Shared integration layer between MyCoRe and Vue. This module provides:

* `MCRVueRootServlet`, the servlet that serves a built Vue app with a `createWebHistory()` router below a MyCoRe
  URL and wraps its `index.html` into the surrounding MyCoRe layout.
* The i18n keys used by the servlet and by the error pages of the Vue apps (`component.vue.error.*`).
* One shared Vue/Vite/TypeScript toolchain for all MyCoRe Vue apps: a single `package.json`, a single `yarn.lock`
  and a single `node_modules`.

The Vue apps themselves stay in the maven module they belong to. Only the toolchain is shared, so that dependency
and security updates happen in exactly one place.

Legacy frontends (AngularJS/Grunt based METS editor, WCMS2, classeditor, acl-editor2, `webtools/src/main/ts/upload`
and the viewer) are not part of this module. They keep their own `package.json`, lockfile and build.

## Layout

| File | Purpose |
| --- | --- |
| `package.json` | central dependency versions and one build script per app |
| `yarn.lock` | the single lockfile, also the target of the CI security scan |
| `.npmrc` | registry for the `@jsr` scope, required by `@jsr/mycore__js-common` |
| `vite.shared.ts` | shared vite configuration, see below |
| `tsconfig.json` | shared TypeScript compiler options, extended by the app configs |
| `env.d.ts` | shared ambient types, referenced by the `env.d.ts` of every app |
| `eslint.config.mjs` | shared flat eslint config, re-exported by the `eslint.config.mjs` of an app |
| `playwright.webcli.config.mts` | playwright setup of the webcli accessibility tests |
| `playwright.webcli.performance.config.mts` | playwright setup of the webcli performance probe |
| `testing/` | test servers that need a toolchain dependency, e.g. the webcli websocket stub |

## Adding an app to the shared toolchain

An app keeps its sources and its build output in its own maven module. It only loses its `package.json`,
its `yarn.lock` and its local `node_modules`.

1. Add the app dependencies to the `package.json` of this module and add the scripts for it. The texteditor is the
   reference: `build:<app>`, `build-only:<app>`, `type-check:<app>` and `dev:<app>`.
2. Replace the app's `vite.config.ts` by a `vite.config.mts` built on the shared configuration:

   ```ts
   import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

   export default defineMCRVueApp({
     configUrl: import.meta.url,
     name: 'texteditor',
     resourcePath: 'modules/webtools/texteditor',
   });
   ```

   `resourcePath` is the path below `target/classes/META-INF/resources` and has to match the `url-pattern` the app
   is bound to in the `web-fragment.xml` of the owning module. App specific settings are passed as a second
   argument and are merged on top of the shared defaults. vite 8 bundles with rolldown, so bundler settings go
   below `build.rolldownOptions`, not below the deprecated `build.rollupOptions`.

   The `.mts` extension and the explicit `.ts` extension in the import are required: without them vite warns that
   the config cannot be loaded natively, because after the migration there is no `package.json` next to the app
   that declares `"type": "module"`.
3. Reduce the app to a single `tsconfig.json` that extends the shared one and points TypeScript at the shared
   `node_modules`:

   ```json
   {
     "extends": "../../../../../mycore-vue/tsconfig.json",
     "include": ["env.d.ts", "src/**/*", "src/**/*.vue", "vite.config.mts"],
     "compilerOptions": {
       "baseUrl": ".",
       "typeRoots": ["../../../../../mycore-vue/node_modules/@types"],
       "paths": {
         "@/*": ["./src/*"],
         "*": [
           "../../../../../mycore-vue/node_modules/@types/*",
           "../../../../../mycore-vue/node_modules/*"
         ]
       }
     }
   }
   ```

   The `@types` entry has to come first, otherwise a package that ships no types of its own, like `prismjs`,
   resolves to its javascript and TypeScript never looks at its `@types` package.
4. Let the app's `env.d.ts` reference the shared one, which is the only place where `vite/client` resolves:

   ```ts
   /// <reference path="../../../../../mycore-vue/env.d.ts" />
   ```
5. If the app is linted, let its `eslint.config.mjs` re-export the shared one, so that the plugin imports resolve
   against the shared `node_modules` while the file patterns stay relative to the app:

   ```js
   export { default } from '../../../../../mycore-vue/eslint.config.mjs';
   ```

   The lint script then runs eslint with the app directory as working directory.
6. Point the `frontend-maven-plugin` execution of the owning module at this module, see the next section.

## Build integration rules

* `yarn install` runs exactly once, here in `mycore-vue`.
* Each app build stays an execution of the **owning** module's POM, with `workingDirectory` pointing at
  `mycore-vue` and the app specific script:

  ```xml
  <execution>
    <id>yarn-build-texteditor</id>
    <goals>
      <goal>yarn</goal>
    </goals>
    <phase>generate-resources</phase>
    <configuration>
      <workingDirectory>${basedir}/../mycore-vue</workingDirectory>
      <arguments>build:texteditor</arguments>
    </configuration>
  </execution>
  ```

  `mycore-vue` must never write into another module's `target/`, otherwise a later `mvn -pl <module> clean install`
  deletes those files.
* Every app gets its own vite `cacheDir` below the shared `node_modules/.vite`, so that concurrent app builds under
  `mvn -T` do not write into the same cache. `defineMCRVueApp` derives it from the app name.
* Modules that build an app declare a maven dependency on `mycore-vue`, so that `mvn -pl <module> -am install`
  installs the toolchain first. Without `-am` an existing `mycore-vue/node_modules` is required.

## Module resolution

The app sources live outside this module, so the default resolution would never find the shared `node_modules`: it
only walks up the directory tree of the importing file. Both toolchains have to be told about it:

* vite: `vite.shared.ts` registers a small resolver plugin that resolves package imports against `mycore-vue`
  instead of against the importing app.
* TypeScript: the app's `tsconfig.json` maps every package onto the shared `node_modules` via `paths` and
  `typeRoots`, see above.

## Notes

* The apps are migrated one at a time in separate tickets. A `build:<app>` script only works once the matching
  `vite.config.mts` exists.
* Tests, coverage and linting of an app are also driven from here. webcli is the reference:
  `test:webcli`, `test-coverage:webcli`, `lint:webcli`, `typecheck:webcli`, `ci-check:webcli`,
  `test-a11y:webcli` and `test-performance:webcli`. The vitest configuration stays in the app's `vite.config.mts`, its paths are relative to the
  app root, which `defineMCRVueApp` sets.
* The playwright configuration lives here rather than next to the tests it runs. Playwright loads a `.mts` config
  as a real ES module, and node would not resolve `@playwright/test` from the app directory. The tests themselves
  stay in the app. A test server that playwright starts as a plain node process follows the same rule: the webcli
  websocket stub imports `ws` and therefore lives in `testing/`, while the a11y static server only uses node
  builtins and stays in the app.
* `vue-i18n` is pinned to the exact version `11.4.2`. From `11.4.3` on it requires node 22, while the reactor POM
  pins `node.version` to `v20.19.0`. The pin can be dropped as soon as the node version is raised.
* `vite-plugin-eslint` (access-key-manager2) is unmaintained and is not part of the shared toolchain. Linting runs
  as a standalone script.
* `rollup-plugin-external-globals` (access-key-manager2) is not part of the shared toolchain either. vite 8 bundles
  with rolldown, so the way that app externalizes bootstrap has to be decided during its migration.
