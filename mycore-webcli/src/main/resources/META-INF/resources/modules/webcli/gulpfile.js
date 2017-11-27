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

var gulp = require('gulp');
var plugins = require('gulp-load-plugins')();

var paths = {
    src: {
        ts: './app/**/*.ts'
    },
    dest: {
        js: 'build'
    }
};

gulp.task('compile:typescript', function () {
    var result = gulp.src([paths.src.ts])
        .pipe(plugins.inlineNg2Template({
            base: '/',
            html: true,
            css: false,
            jade: false,
            target: 'es6',
            useRelativePaths: false
        }))
        .pipe(plugins.typescript(plugins.typescript.createProject('tsconfig.json', {
            typescript: require('typescript'),
            outDir: paths.dest.js
        })));
    return result.js
        .pipe(gulp.dest(paths.dest.js));
});

gulp.task('default', gulp.parallel('compile:typescript'));
