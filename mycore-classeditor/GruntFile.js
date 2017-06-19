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
                            cwd: './node_modules/font-awesome',
                            dest: './target/classes/META-INF/resources/modules/classeditor/node_modules/font-awesome',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/dojo',
                            dest: './target/classes/META-INF/resources/modules/classeditor/node_modules/dojo',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './node_modules/dijit',
                            dest: './target/classes/META-INF/resources/modules/classeditor/node_modules/dijit',
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
