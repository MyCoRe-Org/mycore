module.exports = function (grunt) {
    grunt.loadNpmTasks('grunt-ts');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-html2js');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-jasmine');
    grunt.loadNpmTasks('grunt-tslint');
    grunt.loadNpmTasks('grunt-contrib-uglify');

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        properties: {
            targetPath: "./target", // used for documentation
            webVisiblePath: "./target/classes/META-INF/resources",
            outputPath: "./target/classes/META-INF/resources/module/mets" // used for things which should be included in .jar file
        }, /*    webVisiblePath: "../archive/docportal/build/webapps/",
         outputPath: "../archive/docportal/build/webapps/module/mets"*/
        clean: {
            compiled: ["<%= properties.outputPath %>/js", "target/css"],
            copied: ["<%= properties.outputPath %>/example", "target/json"],
            all: ["<%= properties.outputPath %>/"]
        },
        ts: { // Compiles Typescript files
            MetsEditorClient: {
                src: ["node_modules/@types/**/*.d.ts", "src/main/ts/MetsEditor.ts"],
                out: "<%= properties.outputPath %>/js/MetsEditor.js",
                options: {
                    target: 'es5',
                    declaration: true,
                    comments: true,
                    ignoreError: false,
                    sourceMap: false,
                    references: [
                        "node_modules/@types/**/*.d.ts"
                    ]
                }
            },
            MetsEditorClientTests: {
                src: ["node_modules/@types/**/*.d.ts", "<%= properties.outputPath %>/js/MetsEditor.d.ts", "src/test/ts/**/*.ts"],
                out: "<%= properties.outputPath %>/js/MetsEditor-tests.js",
                options: {
                    target: 'es5',
                    declaration: true,
                    comments: true,
                    ignoreError: false,
                    sourceMap: false,
                }
            }
        },
        tslint: { // Checks Typescript code for common mistakes
            options: {
                configuration: grunt.file.readJSON("../tslint.json")
            },
            MetsEditorClient: {
                src: ['src/main/ts/**/*.ts']
            },
            MetsEditorClientTests: {
                src: ['src/test/ts/**/*.ts']
            }
        },
        html2js: { // Compiles the Angular templates to js
            options: {
                base: "src/main/template",
                module: "MetsEditorTemplates",
                singleModule: "true"
            },
            templates: {
                src: ['src/main/template/**/*.html'],
                dest: '<%= properties.outputPath %>/js/MetsEditor-templates.js'
            }
        },
        copy: { // Copys files to the target folder
            sources: {
                expand: true,
                cwd: 'src/main/',
                src: [
                    "json/**",
                    "example/**",
                    "img/**",
                    "lib/**"
                ],
                dest: '<%= properties.outputPath %>/'
            },
            frontendFiles: {
                expand: true,
                cwd: './node_modules',
                src: [
                    "jquery/dist/jquery.js",
                    "angular/angular.js",
                    "angular-resource/angular-resource.js",
                    "angular-bootstrap/ui-bootstrap-tpls.js",
                    "bootstrap/dist/**",
                    "angular-mocks/angular-mocks.js",
                    "angular-hotkeys/build/hotkeys.min.js",
                    "angular-hotkeys/build/hotkeys.min.css",
                    "angular-borderlayout2/dist/borderLayout.js",
                    "angular-borderlayout2/dist/borderLayout.css",
                    "angular-lazy-image/release/lazy-image.js"
                ],
                dest: '<%= properties.webVisiblePath %>/shared/'
            }
        },
        less: {
            MetsEditorClient: {
                files: {
                    '<%= properties.outputPath %>/css/mets-editor.css': 'src/main/less/mets-editor.less'
                }
            }
        },
        watch: { // Compiles less to css
            default: {
                files: ['src/main/**/*'],
                tasks: ["compile", "tslint"],
                options: {
                    spawn: true
                }
            },
            tests: {
                files: ['src/test/ts/*'],
                tasks: ["compileTests", "jasmine"],
                options: {
                    spawn: true
                }
            }
        },
        jasmine: { // runs javascript unit Tests
            pivotal: {
                src: ['<%= properties.webVisiblePath %>/shared/angular/angular.js',
                    '<%= properties.webVisiblePath %>/shared/angular-mocks/angular-mocks.js',
                    '<%= properties.outputPath %>/js/MetsEditor.js'],
                options: {
                    specs: '<%= properties.outputPath %>/js/MetsEditor-tests.js'
                }
            }
        },
        uglify: {
            MetsEditorClient : {
                options: {
                    sourceMap: true,
                },
                files: {
                    '<%= properties.outputPath %>/js/MetsEditor.min.js': ['<%= properties.outputPath %>/js/MetsEditor.js']
                }
            },
            "MetsEditorClient-Templates": {
                options: {
                    sourceMap: true
                },
                files: {
                    '<%= properties.outputPath %>/js/MetsEditor-templates.min.js': ['<%= properties.outputPath %>/js/MetsEditor-templates.js']
                }
            }
        }
    });

    grunt.task.registerTask("dependencies", ["copy:frontendFiles"]);
    grunt.task.registerTask("compile", ["ts:MetsEditorClient", "less:MetsEditorClient", "html2js:templates", "uglify"]);
    grunt.task.registerTask("compileTests", ["ts:MetsEditorClientTests"]);
    grunt.task.registerTask("default", ["dependencies", "compile", "compileTests", "tslint",
        "copy:sources"]);

    grunt.task.registerTask("developer", function () {
        var connect = require('connect');
        var serveStatic = require('serve-static');
        connect().use(serveStatic(grunt.config("properties.webVisiblePath"))).listen(8080);
        grunt.task.run("watch:default");
    });

    grunt.task.registerTask("testDeveloper", function () {
        var connect = require('connect');
        var serveStatic = require('serve-static');
        connect().use(serveStatic(grunt.config("properties.webVisiblePath"))).listen(8080);
        grunt.task.run("watch:tests");
    });

};
