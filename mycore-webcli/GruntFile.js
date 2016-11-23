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
                    tsconfig: true
                }
            },
            copy: {
                main: {
                    files: [
                        {
                            expand: true,
                            cwd: './node_modules',
                            dest: './target/classes/META-INF/resources/modules/webcli/node_modules',
                            flatten: true,
                            src: [
                                './bootstrap/dist/js/bootstrap.min.js',
                                './bootstrap/dist/css/bootstrap.min.css',
                                './core-js/client/shim.js',
                                './zone.js/dist/zone.js',
                                './reflect-metadata/Reflect.js',
                                './systemjs/dist/system.js',
                                './jquery/dist/jquery.min.js'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/@angular',
                            dest: './target/classes/META-INF/resources/modules/webcli/node_modules/@angular',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/rxjs',
                            dest: './target/classes/META-INF/resources/modules/webcli/node_modules/rxjs',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/angular2-in-memory-web-api',
                            dest: './target/classes/META-INF/resources/modules/webcli/node_modules/angular2-in-memory-web-api',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/font-awesome',
                            dest: './target/classes/META-INF/resources/modules/webcli/node_modules/font-awesome',
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
