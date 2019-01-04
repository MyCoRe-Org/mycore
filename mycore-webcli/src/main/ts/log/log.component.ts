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

import {AfterViewInit, Component, OnInit} from '@angular/core';
import {Log} from './log';
import {Settings} from '../settings/settings';
import {RESTService} from '../service/rest.service';
import {CommunicationService} from '../service/communication.service';

@Component({
  selector: '[web-cli-log]',
  templateUrl: 'app/log/log.html'
})
export class WebCliLogComponent implements AfterViewInit, OnInit {
  timeout: number;
  settings: Settings;
  webCLILogElement: HTMLElement;

  constructor(private _restService: RESTService,
              private _comunicationService: CommunicationService) {
    this.settings = new Settings(500, 10, true, false);
  }

  ngOnInit() {
    this._comunicationService.settings.subscribe(
      settings =>{
        this.settings = settings;
      }
    );
    this._restService.currentLog.subscribe(
      log => {
        if (log != undefined) {
          var node = document.createElement("pre");
          var text = document.createTextNode(log.logLevel + ": " + log.message);
          node.appendChild(text);
          this.webCLILogElement.appendChild(node);
          if(log.exception != undefined) {
            var nodeEx = document.createElement("pre");
            var textEx = document.createTextNode(log.exception);
            nodeEx.appendChild(textEx);
            this.webCLILogElement.appendChild(nodeEx);
          }
          for (let removeNodes = this.webCLILogElement.childNodes.length - this.settings.historySize;
               removeNodes > 0;
               removeNodes--) {
            (<HTMLElement>this.webCLILogElement.childNodes[0]).remove();
          }
        }
        this.scrollLog();
      });
  }

  ngAfterViewInit() {
    this.webCLILogElement = <HTMLElement> document.getElementsByClassName('web-cli-log')[0];
  }

  public clearLog() {
    this.webCLILogElement.innerHTML = '';
  }

  scrollLog() {
    if (this.settings.autoscroll) {
        this.webCLILogElement.scrollTop = this.webCLILogElement.scrollHeight;
    }
  }
}
