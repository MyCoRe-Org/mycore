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

import {Component} from '@angular/core';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';

@Component({
  selector: '[web-cli-queue]',
  templateUrl: 'app/queue/queue.html'
})
export class WebCliQueueComponent {
  constructor(private _restService: RESTService){
    this._restService.currentQueue.subscribe(
      queue => {
        let ellipsis = "";
        if (queue.length > 99) {
          ellipsis = "</br>...";
        }
        let queueString = queue.join("</br>") + ellipsis;
        document.getElementsByClassName('web-cli-pre')[0].innerHTML = queueString;
      });
  }
}
