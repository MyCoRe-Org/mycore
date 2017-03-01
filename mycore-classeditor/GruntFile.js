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
                            cwd: './bower_components/font-awesome',
                            dest: './target/classes/META-INF/resources/modules/classeditor/bower_components/font-awesome',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './bower_components/dojo',
                            dest: './target/classes/META-INF/resources/modules/classeditor/bower_components/dojo',
                            src: [
                                '**'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './bower_components/dijit',
                            dest: './target/classes/META-INF/resources/modules/classeditor/bower_components/dijit',
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
