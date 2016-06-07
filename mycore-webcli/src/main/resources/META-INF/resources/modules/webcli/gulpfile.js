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
