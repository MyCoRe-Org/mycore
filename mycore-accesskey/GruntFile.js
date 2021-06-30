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
  grunt.loadNpmTasks("grunt-contrib-uglify-es");
  grunt.initConfig({
    uglify: {
      main: {
        files: {
          'target/classes/META-INF/resources/accesskey.min.js': ['src/main/resources/META-INF/resources/accesskey.js'],
          'target/classes/META-INF/resources/accesskey-utils.min.js': ['src/main/resources/META-INF/resources/accesskey-utils.js'],
        }
      }
    }
  });
  grunt.registerTask("default", ["uglify"]);
};
