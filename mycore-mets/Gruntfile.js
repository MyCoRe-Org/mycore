module.exports = function (grunt) {
    grunt.loadNpmTasks('grunt-ts');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-bower-task');
    grunt.loadNpmTasks('grunt-tsd');
    grunt.loadNpmTasks('grunt-html2js');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-jasmine');
    grunt.loadNpmTasks('grunt-tslint');
    grunt.loadNpmTasks('grunt-contrib-uglify');

    const COMPONENTS_PATH = "bower_components/";

    /**
     * Gets the version of a package by package name
     * @param pkg the name of the package
     * @return {*} the version of the bower package
     */
    var getVersion = function (pkg) {
        var possiblePaths = [COMPONENTS_PATH + pkg + '/bower.json', COMPONENTS_PATH + pkg + '/.bower.json'];
        var versionString, existingFiles = possiblePaths.filter(function (path, i, arr) {
            return grunt.file.exists(path);
        });
        if (existingFiles.length === 0) {
            throw "no bower.json ";
        }
        existingFiles.map(function (file) {
            return grunt.file.readJSON(file);
        }).filter(function (json) {
            return "version" in json;
        }).forEach(function (json) {
            versionString = json.version;
        });
        return versionString;
    };

    const SHARED_LAYOUT = function (type, pkg, sourceDir) { // this function tells the plugin where to put the files
        /*
         Example:
         sourceDir: bower_components/jquery/dist/jquery.js
         type: __untyped__ // type resolution of this plugin is crap
         pkg: jquery

         Every Package has files which are declared as "main" (see bower.json of the packages).
         We only need these.
         */
        var versionString = getVersion(pkg);

        // get relative path in component.
        var relativePath = sourceDir.substr(COMPONENTS_PATH.length + pkg.length + 1);
        // +1 because / ist not in package name

        var prefixFolder = "shared/";

        // relativePath ist now dist/jquery.js but we only need dist/
        var relativePathWithoutFileName = relativePath.split("/").filter(function (e, i, a) {
            /*
             we can do some advanced filtering like skip "dist/" or skip "src/"
             But its better if we keep the original structure
             */
            return i !== a.length - 1;
        }).join("/");


        var path = require("path");
        return path.join(prefixFolder + pkg + "/" + versionString + "/" + relativePathWithoutFileName);
    };

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        properties: {
            targetPath: "./target", // used for documentation
            webVisiblePath: "./target/classes/META-INF/resources",
            outputPath: "./target/classes/META-INF/resources/module/mets" // used for things which should be included in .jar file
        }, /*    webVisiblePath: "../archive/docportal/build/webapps/",
         outputPath: "../archive/docportal/build/webapps/module/mets"*/
        bower: { // Downloads libs like jquery, angularjs or bootstrap
            install: {
                options: {
                    targetDir: '<%= properties.webVisiblePath %>',
                    install: true,
                    verbose: false,
                    cleanTargetDir: true,
                    cleanBowerDir: false,
                    bowerOptions: {},
                    layout: SHARED_LAYOUT
                }
            }
        },
        tsd: { // Downloads .d.ts files which are needed for libs like angularjs or jquery
            refresh: {
                options: {
                    command: 'reinstall',
                    latest: false,
                    config: 'tsd.json',
                    opts: {}
                }
            }
        },
        clean: {
            dependecies: ["bower_components/", "typings/"],
            compiled: ["<%= properties.outputPath %>/js", "target/css"],
            copied: ["<%= properties.outputPath %>/example", "target/json"],
            all: ["<%= properties.outputPath %>/", "typings/", "bower_components/"]
        },
        ts: { // Compiles Typescript files
            MetsEditorClient: {
                src: "src/main/ts/MetsEditor.ts",
                out: "<%= properties.outputPath %>/js/MetsEditor.js",
                options: {
                    target: 'es5',
                    declaration: true,
                    comments: true,
                    ignoreError: false,
                    sourceMap: false,
                    references: [
                        "typings/**/*.d.ts"
                    ]
                }
            },
            MetsEditorClientTests: {
                src: ["<%= properties.outputPath %>/js/MetsEditor.d.ts", "src/test/ts/**/*.ts"],
                out: "<%= properties.outputPath %>/js/MetsEditor-tests.js",
                options: {
                    target: 'es5',
                    declaration: true,
                    comments: true,
                    ignoreError: false,
                    sourceMap: false,
                    references: [
                        "typings/**/*.d.ts"
                    ]
                }
            }
        },
        tslint: { // Checks Typescript code for common mistakes
            options: {
                configuration: grunt.file.readJSON("tslint.json")
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
            exampleJson: {
                expand: true,
                cwd: 'src/main/',
                src: ["json/**"],
                dest: '<%= properties.outputPath %>/'
            },
            exampleHtml: {
                expand: true,
                cwd: 'src/main/',
                src: ["example/**"],
                dest: '<%= properties.outputPath %>/'
            },
            images: {
                expand: true,
                cwd: 'src/main/',
                src: ["img/**"],
                dest: '<%= properties.outputPath %>/'
            },
            lib: {
                expand: true,
                cwd: 'src/main/',
                src: ["lib/**"],
                dest: '<%= properties.outputPath %>/'
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
                src: ['<%= properties.webVisiblePath %>/shared/angular/1.4.5-build.4179+sha.d881673/angular.js',
                    '<%= properties.webVisiblePath %>/shared/angular-mocks/1.3.17/angular-mocks.js',
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

    grunt.task.registerTask("dependencies", ["bower:install", "tsd:refresh"]);
    grunt.task.registerTask("compile", ["ts:MetsEditorClient", "less:MetsEditorClient", "html2js:templates", "uglify"]);
    grunt.task.registerTask("compileTests", ["ts:MetsEditorClientTests"]);
    grunt.task.registerTask("default", ["dependencies", "compile", "compileTests", "tslint",
        "copy"]);

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
