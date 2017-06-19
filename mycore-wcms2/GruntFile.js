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
                            cwd: './node_modules/ckeditor',
                            dest: './target/classes/META-INF/resources/modules/wcms2/node_modules/ckeditor',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/dojo',
                            dest: './target/classes/META-INF/resources/modules/wcms2/node_modules/dojo',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/dijit',
                            dest: './target/classes/META-INF/resources/modules/wcms2/node_modules/dijit',
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
