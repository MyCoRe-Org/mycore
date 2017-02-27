(function(global) {
  var paths = {
    'npm:': '../node_modules/'
  };
  // map tells the System loader where to look for things
  var map = {
    'app':                                'js',
    '@angular/core':                      'npm:@angular/core',
    '@angular/common':                    'npm:@angular/common',
    '@angular/compiler':                  'npm:@angular/compiler',
    '@angular/http':                      'npm:@angular/http',
    '@angular/platform-browser':          'npm:@angular/platform-browser',
    '@angular/platform-browser-dynamic':  'npm:@angular/platform-browser-dynamic',
    '@angular/router':                    'npm:@angular/router',
    '@angular/upgrade':                   'npm:@angular/upgrade',
    '@angular/forms':                     'npm:@angular/forms',
    'rxjs':                               'npm:rxjs',
    'moment':                             'npm:moment',
    'angular2-moment':                    'npm:angular2-moment',
    'ng2-bootstrap':                      'npm:ng2-bootstrap'
  };
  // packages tells the System loader how to load when no filename and/or no extension
  var packages = {
    'app':                                { main: 'main',  defaultExtension: 'js' },
    'rxjs':                               { defaultExtension: 'js' },
    'moment':                             { main: './moment', defaultExtension: 'js' },
    'angular2-moment':                    { main: './index', defaultExtension: 'js' },
    'ng2-bootstrap':                      { main: './ng2-bootstrap.umd.min', defaultExtension: 'js' }
  };
  var ngPackageNames = [
    'common',
    'compiler',
    'core',
    'http',
    'platform-browser',
    'platform-browser-dynamic',
    'router',
    'upgrade',
    'forms'
  ];
  // Add package entries for angular packages
  ngPackageNames.forEach(function(pkgName) {
    packages['@angular/'+pkgName] = { main: 'bundles/'+pkgName+'.umd.js', defaultExtension: 'js' };
  });
  var config = {
    paths,
    map,
    packages
  }
  System.config(config);
}(this));
