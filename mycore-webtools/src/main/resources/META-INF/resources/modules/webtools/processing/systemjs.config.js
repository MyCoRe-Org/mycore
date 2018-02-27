/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    'ngx-bootstrap':                      'npm:ngx-bootstrap'
  };
  // packages tells the System loader how to load when no filename and/or no extension
  var packages = {
    'app':                                { main: 'main',  defaultExtension: 'js' },
    'rxjs':                               { defaultExtension: 'js' },
    'moment':                             { main: './moment', defaultExtension: 'js' },
    'angular2-moment':                    { main: './index', defaultExtension: 'js' },
    'ngx-bootstrap':                      { main: './ngx-bootstrap.umd.min', defaultExtension: 'js' }
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
  };
  System.config(config);
}(this));
