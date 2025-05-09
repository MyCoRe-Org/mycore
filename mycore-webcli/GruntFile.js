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

module.exports = function(grunt) {
  grunt.loadNpmTasks('grunt-ts');
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
