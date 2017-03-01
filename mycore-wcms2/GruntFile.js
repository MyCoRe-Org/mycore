module.exports = function (grunt) {
    grunt.loadNpmTasks('grunt-maven');
    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt
        .initConfig({
            pkg: grunt.file.readJSON('package.json'),
            copy: {
                main: {
                    files: [
                        {
                            expand: true,
                            cwd: './bower_components/ckeditor',
                            dest: './target/classes/META-INF/resources/modules/wcms2/bower_components/ckeditor',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './bower_components/dojo',
                            dest: './target/classes/META-INF/resources/modules/wcms2/bower_components/dojo',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './bower_components/dijit',
                            dest: './target/classes/META-INF/resources/modules/wcms2/bower_components/dijit',
                            src: [
                                '**'
                            ]
                        }
                    ]
                }
            }
        });

    grunt.registerTask('default', ['copy']);

};
