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

import {Component, ViewChild} from '@angular/core';
import {WebCliCommandsComponent} from './commands/commands.component';
import {WebCliCommandInputComponent} from './command-input/command-input.component';
import {WebCliLogComponent} from './log/log.component';
import {WebCliQueueComponent} from './queue/queue.component';
import {WebCliSettingsComponent} from './settings/settings.component';
import {CommunicationService} from './service/communication.service';
import {RESTService} from './service/rest.service';

declare var $: any;

@Component({
  selector: 'webcli',
  templateUrl: 'app/app.html',
  directives: [WebCliCommandsComponent, WebCliCommandInputComponent, WebCliLogComponent, WebCliSettingsComponent, WebCliQueueComponent],
  providers: [CommunicationService, RESTService]
})
export class AppComponent {
  title = 'MyCoRe Web CLI2';
  refreshRunning = true;
  currentCommand: String = "";
  currentQueueLength: number = 0;
  commandHistory: string[];
  @ViewChild(WebCliLogComponent)
  webCliLogComponent: WebCliLogComponent;

  constructor(private _restService: RESTService,
              private _communicationService: CommunicationService){
    this._restService.currentCommand.subscribe(
      command => {
          this.currentCommand = command;
      });

    this._restService.currentQueueLength.subscribe(
      queueLength => {
          this.currentQueueLength = queueLength;
          if (queueLength < 1) {
            (<HTMLElement>document.getElementsByClassName('logTab')[0]).click();
            this.webCliLogComponent.scrollLog();
          }
      });
    this._communicationService.commandHistory.subscribe(
      value => {
        this.commandHistory = value.slice().reverse();
      }
    );
  }

  onClickCommandDropDown(event) {
    // if (event.target.className.indexOf("dropdown-toggle") > -1) {
    //   var maxHeight = window.innerHeight - event.target.getBoundingClientRect().bottom - 10;
    //   if (event.target.parentElement.children.length > 1 && event.target.parentElement.children[1].className == "dropdown-menu") {
    //     event.target.parentElement.children[1].style.maxHeight = maxHeight;
    //   }
    // }
  }

  clearLog(){
    this.webCliLogComponent.clearLog();
  }

  clearCommandList(){
    this._restService.clearCommandList();
  }

  setRefresh(refresh: boolean){
    this.refreshRunning = refresh;
    if (refresh){
      this._restService.startLogging();
    }
    else {
      this._restService.stopLogging();
    }
  }

  showCommandHistory() {
    $('#comHistoryModal').modal('show');
  }

  onSelectCommand(com) {
    this._communicationService.setCurrentCommand(com);
    $('#comHistoryModal').modal('hide');
  }
}
