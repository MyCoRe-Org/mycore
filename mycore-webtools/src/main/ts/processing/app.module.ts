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

// modules
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {MomentModule} from 'angular2-moment';
import {ModalModule} from 'ngx-bootstrap';

// components
import {AppComponent} from './component/app.component';
import {CollectionComponent} from './component/collection.component';
import {ProcessableComponent} from './component/processable.component';

// pipes
import {JsonStringPipe} from './pipe/json.pipe';

@NgModule({
    imports: [BrowserModule, MomentModule, ModalModule.forRoot()],
    bootstrap: [AppComponent],
    declarations: [AppComponent, CollectionComponent, ProcessableComponent, JsonStringPipe]
})
export class AppModule {
}
