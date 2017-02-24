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
                            cwd: './bower_components',
                            dest: './target/classes/META-INF/resources/modules/acl-editor2/gui/bower_components',
                            flatten: true,
                            src: [
                                './jquery-stupid-table/stupidtable.min.js',
                                './bootstrap3-typeahead/bootstrap3-typeahead.min.js'
                            ]
                        },
                        {
                            expand: true,
                            cwd: './bower_components/select2',
                            dest: './target/classes/META-INF/resources/modules/acl-editor2/gui/bower_components/select2',
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
