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
import {CommunicationService} from '../service/communication.service';
import {RESTService} from '../service/rest.service';
import {Settings} from './settings';

@Component({
  selector: 'web-cli-settings',
  templateUrl: 'app/settings/settings.html'
})
export class WebCliSettingsComponent {
  settings: Settings;

  constructor(private _communicationService: CommunicationService,
              private _restService: RESTService){
                this._restService.continueIfOneFails.subscribe(
                  value => this.settings.continueIfOneFails = value
                );
              }

  ngOnInit() {
    this.settings = this.getSettingsFromCookie(500, 10, true, false);
    this._communicationService.setSettings(this.settings);
    this._restService.setContinueIfOneFails(this.settings.continueIfOneFails);
  }

  onHistoryChange() {
    if (localStorage.getItem("historySize") != this.settings.historySize + "") {
      localStorage.setItem("historySize", this.settings.historySize + "");
    }
  }

  onComHistoryChange() {
    if (localStorage.getItem("comHistorySize") != this.settings.comHistorySize + "") {
      localStorage.setItem("comHistorySize", this.settings.comHistorySize + "");
    }
  }

  onAutoScrollChange(event) {
    if (localStorage.getItem("autoScroll") != event.srcElement.checked + "") {
      localStorage.setItem("autoScroll", event.srcElement.checked);
    }
  }

  onContinueIfOneFailsChange(event) {
    if (localStorage.getItem("continueIfOneFails") != event.srcElement.checked + "") {
      localStorage.setItem("continueIfOneFails", event.srcElement.checked);
    }
    this._restService.setContinueIfOneFails(event.srcElement.checked);
  }

  deleteCommandHistory() {
    this._communicationService.setCommandHistory([]);
    localStorage.removeItem("commandHistory");
  }

  private getSettingsFromCookie(defaultHSize: number, defaultComHSize: number, defaultAutoScroll: boolean, defaultContinueIfOneFails: boolean) {
    var storageHSize = localStorage.getItem("historySize");
    if (storageHSize != undefined && storageHSize != ""){
      defaultHSize = parseInt(storageHSize);
    }
    else {
      localStorage.setItem("historySize", defaultHSize + "");
    }
    var storageComHSize = localStorage.getItem("comHistorySize");
    if (storageComHSize != undefined && storageComHSize != ""){
      defaultComHSize = parseInt(storageComHSize);
    }
    else {
      localStorage.setItem("comHistorySize", defaultComHSize + "");
    }
    var storageAutoScroll = localStorage.getItem("autoScroll");
    if (storageAutoScroll != undefined && storageAutoScroll != ""){
      defaultAutoScroll = (storageAutoScroll == "true");
    }
    else {
      localStorage.setItem("autoScroll", defaultAutoScroll +  "");
    }

    var storageContinueIfOneFails = localStorage.getItem("continueIfOneFails");
    if (storageContinueIfOneFails != undefined && storageContinueIfOneFails != ""){
      defaultContinueIfOneFails = (storageContinueIfOneFails == "true");
    }
    else {
      localStorage.setItem("defaultContinueIfOneFails", defaultContinueIfOneFails + "")
    }
    return new Settings(defaultHSize, defaultComHSize, defaultAutoScroll, defaultContinueIfOneFails);
  }
}
