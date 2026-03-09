/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import { Component, OnInit } from '@angular/core';
import { RESTService } from '../service/rest.service';
import { CommunicationService } from '../service/communication.service';
import { Commands } from './commands';

@Component({
  selector: '[webcli-commands]',
  templateUrl: 'app/commands/commands.html',
})
export class WebCliCommandsComponent implements OnInit {
  commandList: Commands[];
  currentCommand: string;

  minVisibleCommands: number = 10;

  constructor(private _restService: RESTService,
    private _comunicationService: CommunicationService) {
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

  onHoverSubmenu(event, commands: any[]) {
    const parent = event.currentTarget;
    if (parent.className && parent.className.indexOf("dropdown-submenu") > -1) {
      const submenu = parent.querySelector(".dropdown-menu");
      if (submenu) {
        let itemHeight = 32;
        const firstItem = submenu.querySelector('.dropdown-item');
        if (firstItem && firstItem.offsetHeight > 0) {
            itemHeight = firstItem.offsetHeight;
        }

        const itemsCount = commands ? commands.length : 0;
        // Calculate space needed for minVisibleCommands (or all items if fewer)
        const minRequiredItems = Math.min(itemsCount, this.minVisibleCommands);
        // Add some padding (e.g. 10px) to the calculation
        const minRequiredHeight = minRequiredItems * itemHeight + 10;

        const parentRect = parent.getBoundingClientRect();
        const availableSpaceBelow = window.innerHeight - parentRect.top - 10;

        let newTop = parentRect.top;

        // If space below is insufficient for the minimum desired items, shift up
        if (availableSpaceBelow < minRequiredHeight) {
            const neededShift = minRequiredHeight - availableSpaceBelow;
            newTop = parentRect.top - neededShift;
            // Ensure we don't go off the top of the screen (e.g. 10px margin)
            if (newTop < 10) {
                newTop = 10;
            }
        }

        // Extend to bottom margin
        const maxHeight = window.innerHeight - newTop - 10;

        submenu.style.maxHeight = maxHeight + "px";
        submenu.style.top = newTop + "px";
        submenu.style.left = parentRect.right + "px";
      }
    }
  }
}
