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

import {Component, OnInit} from '@angular/core';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';
import {Commands} from './commands';

@Component({
  selector: '[webcli-commands]',
  templateUrl: 'app/commands/commands.html',
})
export class WebCliCommandsComponent implements OnInit {
  commandList: Commands[];
  currentCommand: string;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
                this._restService.currentCommandList.subscribe(
                  commandList => this.commandList = commandList
                );
              }

  ngOnInit() {
    this._restService.getCommands();
  }

  onSelect(command) {
    this._comunicationService.setCurrentCommand(command);
  }

  onHoverSubmenu(event) {
    if (event.target.className == "dropdown-item") {
      var maxHeight = window.innerHeight - event.target.parentElement.getBoundingClientRect().top - 10;
      if (event.target.parentElement.children.length > 1 && event.target.parentElement.children[1].className == "dropdown-menu") {
        event.target.parentElement.children[1].style.maxHeight = maxHeight;
      }
    }
  }
}
