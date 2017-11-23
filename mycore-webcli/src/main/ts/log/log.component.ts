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
import {Log} from './log';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';

@Component({
  selector: '[web-cli-log]',
  templateUrl: 'app/log/log.html'
})
export class WebCliLogComponent {
  timeout: number;
  settings: Settings;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService){
                this.settings = new Settings(500, 10, true, false);
                this._comunicationService.settings.subscribe(
                  settings =>{
                    this.settings = settings;
                  }
                );
                this._restService.currentLog.subscribe(
                  log => {
                    if (log != undefined) {
                      if (document.getElementsByClassName('web-cli-log')[0].childNodes.length + 1 > this.settings.historySize) {
                        document.getElementsByClassName('web-cli-log')[0].removeChild(document.getElementsByClassName('web-cli-log')[0].childNodes[0]);
                      }
                      var node = document.createElement("pre");
                      var text = document.createTextNode(log.logLevel + ": " + log.message);
                      node.appendChild(text);
                      document.getElementsByClassName('web-cli-log')[0].appendChild(node);
                      if(log.exception != undefined) {
                        var nodeEx = document.createElement("pre");
                        var textEx = document.createTextNode(log.exception);
                        nodeEx.appendChild(textEx);
                        document.getElementsByClassName('web-cli-log')[0].appendChild(nodeEx);
                      }
                    }
                  });
              }

  public clearLog() {
    document.getElementsByClassName('web-cli-log')[0].innerHTML = "";
  }

  ngAfterViewChecked() {
    this.scrollLog()
  }

  scrollLog() {
    if (this.settings.autoscroll) {
        var elem = document.getElementsByClassName('web-cli-log');
        elem[0].scrollTop = elem[0].scrollHeight;
    }
  }
}
