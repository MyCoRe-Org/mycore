/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import {defineConfig} from 'vite';
import {viteStaticCopy} from 'vite-plugin-static-copy';

const inputs = {
    base: 'src/main/typescript/modules/base/module.ts',
    desktop: 'src/main/typescript/modules/desktop/module.ts',
    mets: 'src/main/typescript/modules/mets/module.ts',
    pdf: 'src/main/typescript/modules/pdf/module.ts',
    frame: 'src/main/typescript/modules/frame/module.ts',
    logo: 'src/main/typescript/modules/logo/module.ts',
    metadata: 'src/main/typescript/modules/metadata/module.ts',
    piwik: 'src/main/typescript/modules/piwik/module.ts',
    "toolbar-extender": 'src/main/typescript/modules/toolbar-extender/module.ts',
    epub: 'src/main/typescript/modules/epub/module.ts',
    iiif: 'src/main/typescript/modules/iiif/module.ts',
    style_default: 'src/main/less/templates/default/Iview.less',
    style_tei: 'src/main/less/tei.less',
};
export default defineConfig({
    plugins: [
        viteStaticCopy({

            targets: [
                {
                    src: 'node_modules/pdfjs-dist/build/pdf.worker.min.js',
                    dest: 'js/lib/'
                },
                {
                    src: 'node_modules/epubjs/dist/*',
                    dest: 'js/lib/epubjs/'
                },
                {
                    src: 'node_modules/jszip/dist/*',
                    dest: 'js/lib/jszip/'
                },
                {
                    src: 'node_modules/jquery/dist/*',
                    dest: 'js/lib/'
                },
                {
                    src: 'node_modules/pdfjs-dist/cmaps/*',
                    dest: 'cmaps/'
                }
            ]
        })
    ],
    build: {
        emptyOutDir: false,
        lib: {
            entry: inputs,
            fileName: (format, entryName) => {
                return "js/iview-client-" + entryName + "." + format + ".js";
            }
        },
        cssCodeSplit: true,
        rollupOptions: {
            input: inputs,
            output: {
                assetFileNames: (assetInfo) => {
                    switch (assetInfo.name) {
                        case "style_tei.css":
                            return "css/tei.css";
                        case "style_default.css":
                            return "css/default.css";
                    }
                }
            }

        },
        outDir: 'target/classes/META-INF/resources/modules/iview2/',
    },
});
