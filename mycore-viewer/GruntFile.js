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

module.exports = function (grunt) {
    grunt.loadNpmTasks('grunt-ts');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-maven');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-uglify');

    var globalConfig = {
        projectBase: grunt.option('projectBase') || ''
    };

    grunt
        .initConfig({
            globalConfig: globalConfig,
            pkg: grunt.file.readJSON('package.json'),
            uglify: {
                pdfjs: {
                    mangle: false,
                    files: {
                        '<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/lib/pdf.min.js': '<%= globalConfig.projectBase %>src/main/resources/META-INF/resources/modules/iview2/js/lib/pdf.js',
                        '<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/lib/pdf.min.worker.js': '<%= globalConfig.projectBase %>src/main/resources/META-INF/resources/modules/iview2/js/lib/pdf.worker.js'
                    }
                },
                viewer: {
                    mangle: true,
                    files: {
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-desktop.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-desktop.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-frame.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-frame.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-logo.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-logo.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-metadata.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-metadata.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mets.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mets.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mobile.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mobile.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-pdf.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-pdf.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-piwik.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-piwik.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-toolbar-extender.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-toolbar-extender.js",
                        "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-tei.min.js":"<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-tei.js"

                    }
                }
            },
            watch: {
                /*ts: {
                 files: [ '<%= globalConfig.projectBase %>src/main/typescript/*.ts' ],
                 tasks: 'default',
                 options: {
                 forever: false,
                 livereload: true
                 }
                 }*/
            },
            less: {
                development: {
                    files: {
                        '<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/css/default.css': "<%= globalConfig.projectBase %>src/main/less/templates/default/Iview.less",
                        '<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/css/mobile.css': "<%= globalConfig.projectBase %>src/main/less/templates/mobile/Iview.less",
                        '<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/css/tei.css': "<%= globalConfig.projectBase %>src/main/less/tei.less"
                    }
                }
            },
            ts: {
                viewer_base: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.js",
                    src:"<%= globalConfig.projectBase %>src/main/typescript/modules/base/module.ts",
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_desktop: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-desktop.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/desktop/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_frame: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-frame.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/frame/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_logo: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-logo.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/logo/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_metadata: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-metadata.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/metadata/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_mets: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mets.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/mets/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_mobile: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-mobile.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/mobile/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_pdf: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-pdf.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/pdf/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_piwik: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-piwik.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/piwik/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                viewer_toolbar_extender: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-toolbar-extender.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/toolbar-extender/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                },
                tei: {
                    out: "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-tei.js",
                    src:["<%= globalConfig.projectBase %>src/main/typescript/modules/tei/module.ts", "<%= globalConfig.projectBase %>target/classes/META-INF/resources/modules/iview2/js/iview-client-base.d.ts"],
                    options: {
                        declaration: true,
                        sourceMap: false
                    }
                }
            }
        });


        grunt.registerTask('default', ['ts', 'less', 'uglify']);

};

