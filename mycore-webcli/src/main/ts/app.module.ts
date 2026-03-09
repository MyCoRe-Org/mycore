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

import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { HttpModule }    from '@angular/http';

import { AppComponent }   from './app.component';
import { WebCliCommandsComponent } from './commands/commands.component';
import { WebCliCommandInputComponent } from './command-input/command-input.component';
import { WebCliLogComponent } from './log/log.component';
import { WebCliQueueComponent } from './queue/queue.component';
import { WebCliSettingsComponent } from './settings/settings.component';

import { CommunicationService } from './service/communication.service';
import { RESTService } from './service/rest.service';

@NgModule({
  imports:      [ BrowserModule, FormsModule, HttpModule ],
  declarations: [
    AppComponent,
    WebCliCommandsComponent,
    WebCliCommandInputComponent,
    WebCliLogComponent,
    WebCliQueueComponent,
    WebCliSettingsComponent
  ],
  providers:    [ CommunicationService, RESTService ],
  bootstrap:    [ AppComponent ]
})
export class AppModule { }

