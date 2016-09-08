(function(global) {
  // map tells the System loader where to look for things
  var map = {
    'app':                                'build',
    '@angular/core':                      '//unpkg.com/@angular/core@2.0.0-rc.1/',
    '@angular/common':                    '//unpkg.com/@angular/common@2.0.0-rc.1/',
    '@angular/compiler':                  '//unpkg.com/@angular/compiler@2.0.0-rc.1/',
    '@angular/http':                      '//unpkg.com/@angular/http@2.0.0-rc.1/',
    '@angular/platform-browser':          '//unpkg.com/@angular/platform-browser@2.0.0-rc.1/',
    '@angular/platform-browser-dynamic':  '//unpkg.com/@angular/platform-browser-dynamic@2.0.0-rc.1/',
    '@angular/router':                    '//unpkg.com/@angular/router@2.0.0-rc.1/',
    '@angular/router-deprecated':         '//unpkg.com/@angular/router-deprecated@2.0.0-rc.1/',
    '@angular/upgrade':                   '//unpkg.com/@angular/upgrade@2.0.0-rc.1/',
    'angular2-in-memory-web-api':         '//unpkg.com/angular2-in-memory-web-api@0.0.10/',
    'rxjs':                               '//unpkg.com/rxjs@5.0.0-beta.6/'
  };
  // packages tells the System loader how to load when no filename and/or no extension
  var packages = {
    'app':                        { main: 'main.js',  defaultExtension: 'js' },
    'rxjs':                       { defaultExtension: 'js' },
    'angular2-in-memory-web-api': { defaultExtension: 'js' },
  };
  var ngPackageNames = [
    'common',
    'compiler',
    'core',
    'http',
    'platform-browser',
    'platform-browser-dynamic',
    'router',
    'router-deprecated',
    'upgrade',
  ];
  // Add package entries for angular packages
  ngPackageNames.forEach(function(pkgName) {
    packages['@angular/'+pkgName] = { main: pkgName + '.umd.js', defaultExtension: 'js' };
  });
  var config = {
    map: map,
    packages: packages
  }
  System.config(config);
})(this);
