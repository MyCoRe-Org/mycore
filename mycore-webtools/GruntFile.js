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
                          cwd: './node_modules/ngx-bootstrap/bundles',
                          dest: './target/classes/META-INF/resources/modules/webtools/node_modules/ngx-bootstrap',
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
