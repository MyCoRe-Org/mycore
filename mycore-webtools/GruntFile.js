module.exports = function (grunt) {
    grunt.loadNpmTasks('grunt-ts');
    grunt.loadNpmTasks('grunt-maven');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt
        .initConfig({
            pkg: grunt.file.readJSON('package.json'),
            ts: {
                options: {
                    fast: 'never'
                },
                default: {
                    tsconfig: './src/main/ts/processing/tsconfig.json'
                }
            },
            copy: {
                main: {
                    files: [
                        {
                            expand: true,
                            cwd: './node_modules',
                            dest: './target/classes/META-INF/resources/modules/webtools/node_modules',
                            flatten: true,
                            src: [
                              './core-js/client/shim.js',
                              './zone.js/dist/zone.js',
                              './reflect-metadata/Reflect.js',
                              './systemjs/dist/system.js'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/@angular',
                            dest: './target/classes/META-INF/resources/modules/webtools/node_modules/@angular',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/rxjs',
                            dest: './target/classes/META-INF/resources/modules/webtools/node_modules/rxjs',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/moment',
                            dest: './target/classes/META-INF/resources/modules/webtools/node_modules/moment',
                            src: [
                                '**'
                            ]
                        },
                        {
                          expand: true,
                          cwd: './node_modules/angular2-moment',
                          dest: './target/classes/META-INF/resources/modules/webtools/node_modules/angular2-moment',
                          src: [
                              '**'
                          ]
                        },
                        {
                          expand: true,
                          cwd: './node_modules/ng2-bootstrap/bundles',
                          dest: './target/classes/META-INF/resources/modules/webtools/node_modules/ng2-bootstrap',
                          src: [
                              '**'
                          ]
                        }
                    ]
                }
            }
        });

    grunt.registerTask('default', ['ts', 'copy']);

};
