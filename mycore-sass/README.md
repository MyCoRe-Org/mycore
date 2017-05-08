# MyCoRe-SASS
The MyCoRe-SASS module can be used to compile sass (.scss) files to css. It also contains a minifier for css files.

## Requirements
This module uses jsass which is a wrapper for a native compiler. Check compatibility 
table on their [github page](https://github.com/bit3/jsass).

## Usage
Add the module as dependency to your project.
There is one configuration property: ``MCR.SASS.DeveloperMode = true`` which disables the caching of the resource.

You can now compile request your resources with ``http://localhost:8080/rsc/sass/my-custom-bootstrap.css``.
The compiler will search your files in the Web-Context. The example above is placed in 
``src/main/resources/META-INF/resources/my-custom-bootstrap.sass``.

You can trigger minification by adding .min.css instead of .css to your request.
