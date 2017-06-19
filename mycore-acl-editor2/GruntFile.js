module.exports = function (grunt) {
    grunt.loadNpmTasks("grunt-maven");
    grunt.loadNpmTasks("grunt-contrib-copy");

    grunt
        .initConfig({
            pkg: grunt.file.readJSON("package.json"),
            copy: {
                main: {
                    files: [
                        {
                            expand: true,
                            cwd: "./node_modules",
                            dest: "./target/classes/META-INF/resources/modules/acl-editor2/gui/node_modules",
                            flatten: true,
                            src: [
                                "./jquery-stupid-table/stupidtable.min.js",
                                "./bootstrap-3-typeahead/bootstrap3-typeahead.min.js"
                            ]
                        },
                        {
                            expand: true,
                            cwd: "./node_modules/select2",
                            dest: "./target/classes/META-INF/resources/modules/acl-editor2/gui/node_modules/select2",
                            src: [
                                '**'
                            ]
                        }
                    ]
                }
            }
        });

    grunt.registerTask("default", ["copy"]);

};
